package com.hanecuzdan.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.core.backup.BackupSnapshot
import com.hanecuzdan.app.core.export.ExportManager
import com.hanecuzdan.app.core.security.SecurityPreferences
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.Transaction
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun SettingsScreen(
    securityPreferences: SecurityPreferences,
    transactions: List<Transaction>,
    categories: List<Category> = emptyList(),
    budgets: List<Budget> = emptyList(),
    onThemeModeChanged: (Int) -> Unit = {},
    onRestoreSnapshot: suspend (BackupSnapshot) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf(securityPreferences.userName) }
    var familyId by remember { mutableStateOf(securityPreferences.familyId) }
    var themeMode by remember { mutableIntStateOf(securityPreferences.themeMode) }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Dosya okunamadı")
                    val snapshot = Json { ignoreUnknownKeys = true }.decodeFromString<BackupSnapshot>(raw)
                    onRestoreSnapshot(snapshot)
                    infoMsg = "${snapshot.transactions.size} işlem başarıyla geri yüklendi!"
                }.onFailure {
                    infoMsg = "Yedek geri yüklenemedi: ${it.localizedMessage ?: "Geçersiz JSON"}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ayarlar & Dışa Aktarma", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Text("Görünüm Teması", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("Sistem", "Açık", "Koyu").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = themeMode == index,
                    onClick = {
                        themeMode = index
                        securityPreferences.themeMode = index
                        onThemeModeChanged(index)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, 3)
                ) { Text(label) }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Kullanıcı Adı") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = familyId,
            onValueChange = { familyId = it },
            label = { Text("Aile Havuzu ID (Ortak Senkronizasyon)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                securityPreferences.userName = userName.trim().ifBlank { "Hane Üyesi" }
                securityPreferences.familyId = familyId.trim().ifBlank { "hane_aile_havuzu" }
                infoMsg = "Profil ve Aile Havuzu bilgileri kaydedildi."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Profil Bilgilerini Kaydet")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Veri Dışa Aktarma & Yedek Geri Yükleme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val file = ExportManager.exportCsvToFile(context, transactions)
                    ExportManager.shareFile(context, file, "text/csv")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("CSV Paylaş")
            }
            OutlinedButton(
                onClick = {
                    val file = ExportManager.exportPdfToFile(context, transactions)
                    ExportManager.shareFile(context, file, "application/pdf")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("PDF Paylaş")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val snapshot = BackupSnapshot(
                        exportedAt = System.currentTimeMillis(),
                        transactions = transactions,
                        categories = categories,
                        budgets = budgets
                    )
                    val file = ExportManager.exportJsonBackupToFile(context, snapshot)
                    ExportManager.shareFile(context, file, "application/json")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("JSON Yedek Al")
            }
            Button(
                onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Text("JSON Geri Yükle")
            }
        }

        if (infoMsg != null) {
            Text(infoMsg!!, color = MaterialTheme.colorScheme.primary)
        }
    }
}
