package com.hanecuzdan.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.core.security.BiometricAuthHelper
import com.hanecuzdan.app.core.security.SecurityPreferences

@Composable
fun SecuritySettingsScreen(securityPreferences: SecurityPreferences) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Güvenlik & Uygulama Kilidi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SecuritySettingsContent(securityPreferences = securityPreferences)
    }
}

@Composable
fun SecuritySettingsContent(
    securityPreferences: SecurityPreferences,
    onSecurityChanged: (lockEnabled: Boolean, biometricEnabled: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var lockEnabled by remember { mutableStateOf(securityPreferences.isAppLockEnabled) }
    var biometricEnabled by remember { mutableStateOf(securityPreferences.isBiometricEnabled) }
    var password by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(securityPreferences.secretQuestion) }
    var answer by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val biometricSupported = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Açılışta Parola Kilidi")
            Switch(
                checked = lockEnabled,
                onCheckedChange = {
                    if (securityPreferences.hasPasswordSet() || !it) {
                        lockEnabled = it
                        securityPreferences.isAppLockEnabled = it
                        onSecurityChanged(lockEnabled, biometricEnabled)
                    } else {
                        statusMessage = "Önce aşağıdan parola ve gizli soru belirleyin."
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Biyometrik Giriş (Parmak İzi / Yüz)")
            Switch(
                checked = biometricEnabled,
                enabled = biometricSupported,
                onCheckedChange = {
                    biometricEnabled = it
                    securityPreferences.isBiometricEnabled = it
                    onSecurityChanged(lockEnabled, biometricEnabled)
                }
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Yeni Parola") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Gizli Soru (Örn: İlk evcil hayvanınızın adı?)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Gizli Soru Cevabı") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (password.length < 4 || question.isBlank() || answer.isBlank()) {
                    statusMessage = "Parola (en az 4 karakter), gizli soru ve cevap zorunludur."
                } else {
                    securityPreferences.savePasswordAndSecret(password, question, answer)
                    lockEnabled = true
                    onSecurityChanged(lockEnabled, biometricEnabled)
                    password = ""
                    answer = ""
                    statusMessage = "Güvenlik ayarları şifreli olarak kaydedildi."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Parola ve Gizli Soruyu Kaydet")
        }

        if (statusMessage != null) {
            Text(statusMessage!!, color = MaterialTheme.colorScheme.primary)
        }
    }
}
