package com.hanecuzdan.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.core.backup.BackupSnapshot
import com.hanecuzdan.app.core.export.ExportManager
import com.hanecuzdan.app.core.security.SecurityPreferences
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.ui.security.SecuritySettingsContent
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

    var themeExpanded by remember { mutableStateOf(false) }
    var profileExpanded by remember { mutableStateOf(false) }
    var securityExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }

    var isLockEnabled by remember { mutableStateOf(securityPreferences.isAppLockEnabled) }
    var isBiometricEnabled by remember { mutableStateOf(securityPreferences.isBiometricEnabled) }

    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0.0" }
    }

    val themeLabels = remember { listOf("Sistem", "Açık", "Koyu") }

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ayarlar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SettingsExpandableSection(
                title = "Görünüm Teması",
                subtitle = "Seçili: ${themeLabels.getOrElse(themeMode) { "Sistem" }}",
                icon = Icons.Default.Palette,
                expanded = themeExpanded,
                onToggle = { themeExpanded = !themeExpanded }
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeLabels.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = themeMode == index,
                            onClick = {
                                themeMode = index
                                securityPreferences.themeMode = index
                                onThemeModeChanged(index)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, themeLabels.size)
                        ) { Text(label) }
                    }
                }
            }

            SettingsExpandableSection(
                title = "Profil & Aile Havuzu",
                subtitle = "${userName.ifBlank { "Hane Üyesi" }} • ${familyId.ifBlank { "hane_aile_havuzu" }}",
                icon = Icons.Default.Person,
                expanded = profileExpanded,
                onToggle = { profileExpanded = !profileExpanded }
            ) {
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
            }

            SettingsExpandableSection(
                title = "Güvenlik & Uygulama Kilidi",
                subtitle = when {
                    isLockEnabled && isBiometricEnabled -> "Parola kilidi ve biyometrik giriş aktif"
                    isLockEnabled -> "Parola kilidi aktif"
                    isBiometricEnabled -> "Biyometrik giriş aktif"
                    else -> "Uygulama kilidi devre dışı"
                },
                icon = Icons.Default.Lock,
                expanded = securityExpanded,
                onToggle = { securityExpanded = !securityExpanded }
            ) {
                SecuritySettingsContent(
                    securityPreferences = securityPreferences,
                    onSecurityChanged = { lock, bio ->
                        isLockEnabled = lock
                        isBiometricEnabled = bio
                    }
                )
            }

            SettingsExpandableSection(
                title = "Veri Dışa Aktarma & Yedekleme",
                subtitle = "CSV, PDF paylaşımı ve JSON yedek/geri yükleme",
                icon = Icons.Default.Backup,
                expanded = backupExpanded,
                onToggle = { backupExpanded = !backupExpanded }
            ) {
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
            }

            if (infoMsg != null) {
                Text(
                    text = infoMsg!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Sürüm $appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ByTuna",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingsExpandableSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()
                    content()
                }
            }
        }
    }
}
