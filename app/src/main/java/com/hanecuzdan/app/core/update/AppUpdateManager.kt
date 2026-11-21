package com.hanecuzdan.app.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class AppReleaseInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val htmlUrl: String
)

class AppUpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun getCurrentVersionName(): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0.0" }

    suspend fun checkForUpdate(
        currentVersion: String = getCurrentVersionName()
    ): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "HaneCuzdan-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@runCatching null
                if (!response.isSuccessful) {
                    throw IllegalStateException("GitHub API hatası: ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject

                val tagName = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
                if (!isNewerVersion(tagName, currentVersion)) {
                    return@runCatching null
                }

                val releaseNotes = root["body"]?.jsonPrimitive?.content.orEmpty()
                val htmlUrl = root["html_url"]?.jsonPrimitive?.content.orEmpty()
                val assets = root["assets"]?.jsonArray.orEmpty()
                val apkUrl = assets.firstNotNullOfOrNull { element ->
                    val obj = element.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content.orEmpty()
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        obj["browser_download_url"]?.jsonPrimitive?.content
                    } else {
                        null
                    }
                }

                AppReleaseInfo(
                    versionName = tagName.trimStart('v', 'V'),
                    releaseNotes = releaseNotes.ifBlank { "Yeni sürüm iyileştirmeleri ve hata düzeltmeleri." },
                    apkDownloadUrl = apkUrl,
                    htmlUrl = htmlUrl
                )
            }
        }
    }

    suspend fun downloadApk(
        url: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "hane-cuzdan-update.apk")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HaneCuzdan-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("APK indirilemedi: ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Boş yanıt")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }
            }
            onProgress(1f)
            apkFile
        }
    }

    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun installApk(apkFile: File) {
        if (!canInstallPackages()) {
            openInstallPermissionSettings()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openReleasePage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/bytuna/hane-cuzdan/releases/latest"

        fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
            val remoteParts = remoteTag.trimStart('v', 'V')
                .substringBefore('-')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.trimStart('v', 'V')
                .substringBefore('-')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
