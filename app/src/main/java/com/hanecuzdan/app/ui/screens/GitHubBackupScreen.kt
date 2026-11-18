package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.core.backup.BackupResult
import com.hanecuzdan.app.core.backup.BackupSnapshot
import com.hanecuzdan.app.core.backup.GitHubBackupEngine
import com.hanecuzdan.app.core.security.SecurityPreferences
import com.hanecuzdan.app.domain.repository.BudgetRepository
import com.hanecuzdan.app.domain.repository.CategoryRepository
import com.hanecuzdan.app.domain.repository.TransactionRepository
import kotlinx.coroutines.launch

@Composable
fun GitHubBackupScreen(
    securityPreferences: SecurityPreferences,
    backupEngine: GitHubBackupEngine,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository
) {
    val scope = rememberCoroutineScope()
    var repoUrl by remember { mutableStateOf(securityPreferences.githubRepoUrl) }
    var pat by remember { mutableStateOf(securityPreferences.githubPat) }
    var isBackingUp by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GitHub Yedekleme", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "Veritabanı yedeğinizi (JSON) belirttiğiniz GitHub reposuna Personal Access Token (PAT) ile güvenle commit & push yapın.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = repoUrl,
            onValueChange = { repoUrl = it },
            label = { Text("GitHub Repo Adresi (Örn: kullanici/hane-yedek)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = pat,
            onValueChange = { pat = it },
            label = { Text("Personal Access Token (PAT)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                securityPreferences.githubRepoUrl = repoUrl.trim()
                securityPreferences.githubPat = pat.trim()
                isBackingUp = true
                statusMessage = null
                scope.launch {
                    val snapshot = BackupSnapshot(
                        exportedAt = System.currentTimeMillis(),
                        transactions = transactionRepository.getAllSnapshot(),
                        categories = categoryRepository.getAllSnapshot(),
                        budgets = budgetRepository.getAllSnapshot()
                    )
                    val result = backupEngine.pushBackup(repoUrl, pat, snapshot)
                    isBackingUp = false
                    statusMessage = when (result) {
                        is BackupResult.Success -> {
                            securityPreferences.lastBackupAt = System.currentTimeMillis()
                            "Yedekleme başarılı! Commit: ${result.commitUrl}"
                        }
                        is BackupResult.Error -> result.message
                    }
                }
            },
            enabled = !isBackingUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isBackingUp) CircularProgressIndicator() else Text("Şimdi GitHub'a Yedekle")
        }

        if (statusMessage != null) {
            Text(statusMessage!!, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
