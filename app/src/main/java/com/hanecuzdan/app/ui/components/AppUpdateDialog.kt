package com.hanecuzdan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.core.update.AppReleaseInfo
import com.hanecuzdan.app.core.update.AppUpdateManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppUpdateDialog(
    releaseInfo: AppReleaseInfo,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(
                text = "Yeni Sürüm Mevcut (v${releaseInfo.versionName})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = releaseInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "İndiriliyor... %${(progress * 100).roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isDownloading,
                onClick = {
                    val apkUrl = releaseInfo.apkDownloadUrl
                    if (apkUrl.isNullOrBlank()) {
                        updateManager.openReleasePage(releaseInfo.htmlUrl)
                        onDismiss()
                        return@Button
                    }
                    if (!updateManager.canInstallPackages()) {
                        errorMsg = "Lütfen açılan ekrandan HaneCüzdan için yükleme iznini onaylayın, ardından tekrar Güncelle'ye basın."
                        updateManager.openInstallPermissionSettings()
                        return@Button
                    }
                    errorMsg = null
                    isDownloading = true
                    progress = 0f
                    scope.launch {
                        updateManager.downloadApk(apkUrl) { p ->
                            progress = p
                        }.onSuccess { apkFile ->
                            isDownloading = false
                            updateManager.installApk(apkFile)
                            onDismiss()
                        }.onFailure { err ->
                            isDownloading = false
                            errorMsg = err.localizedMessage ?: "İndirme başarısız oldu."
                        }
                    }
                }
            ) {
                Text(if (isDownloading) "İndiriliyor..." else "Güncelle")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isDownloading,
                onClick = onDismiss
            ) {
                Text("Sonra")
            }
        }
    )
}
