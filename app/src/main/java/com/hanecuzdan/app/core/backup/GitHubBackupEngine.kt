package com.hanecuzdan.app.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Base64

sealed interface BackupResult {
    data class Success(val commitUrl: String) : BackupResult
    data class Error(val message: String) : BackupResult
}

/**
 * Veritabanı JSON snapshot'ını kullanıcının belirttiği GitHub reposuna
 * Personal Access Token (PAT) ile commit & push yapar.
 */
class GitHubBackupEngine(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json { prettyPrint = true }

    suspend fun pushBackup(
        repoInput: String,
        pat: String,
        snapshot: BackupSnapshot,
        filePath: String = "backups/hanecuzdan_backup.json"
    ): BackupResult = withContext(Dispatchers.IO) {
        if (pat.isBlank()) {
            return@withContext BackupResult.Error("Personal Access Token (PAT) boş olamaz.")
        }
        val parsed = parseOwnerAndRepo(repoInput)
            ?: return@withContext BackupResult.Error("Geçersiz GitHub repo adresi. Örn: kullanici/hane-yedek")

        val (owner, repo) = parsed
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$filePath"

        // Mevcut dosyanın sha değerini kontrol et (varsa güncelleme için gerekli)
        val existingSha = fetchExistingFileSha(apiUrl, pat)

        val contentJson = json.encodeToString(snapshot)
        val contentBase64 = Base64.getEncoder().encodeToString(contentJson.toByteArray(Charsets.UTF_8))

        val bodyJson = JSONObject().apply {
            put("message", "HaneCüzdan otomatik yedekleme (${snapshot.exportedAt})")
            put("content", contentBase64)
            if (existingSha != null) put("sha", existingSha)
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $pat")
            .header("Accept", "application/vnd.github+json")
            .put(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val commitUrl = runCatching {
                        JSONObject(respStr).getJSONObject("commit").getString("html_url")
                    }.getOrDefault("https://github.com/$owner/$repo")
                    BackupResult.Success(commitUrl)
                } else {
                    BackupResult.Error("GitHub API Hatası (${response.code}): ${response.message}")
                }
            }
        }.getOrElse { ex ->
            BackupResult.Error("Bağlantı hatası: ${ex.localizedMessage ?: "Bilinmeyen hata"}")
        }
    }

    private fun fetchExistingFileSha(apiUrl: String, pat: String): String? {
        val req = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $pat")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()

        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val jsonObj = JSONObject(resp.body?.string().orEmpty())
                jsonObj.optString("sha").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    companion object {
        fun parseOwnerAndRepo(input: String): Pair<String, String>? {
            val cleaned = input.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removeSuffix(".git")
                .trim('/')
            val parts = cleaned.split("/")
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
            return parts[0] to parts[1]
        }
    }
}
