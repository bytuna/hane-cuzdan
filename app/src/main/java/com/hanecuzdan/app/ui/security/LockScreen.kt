package com.hanecuzdan.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.fragment.app.FragmentActivity
import com.hanecuzdan.app.core.security.BiometricAuthHelper
import com.hanecuzdan.app.core.security.SecurityPreferences

@Composable
fun LockScreen(
    securityPreferences: SecurityPreferences,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var secretAnswer by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val biometricReady = remember {
        securityPreferences.isBiometricEnabled && BiometricAuthHelper.isBiometricAvailable(context)
    }

    LaunchedEffect(biometricReady) {
        if (biometricReady && context is FragmentActivity) {
            BiometricAuthHelper.authenticate(
                activity = context,
                onSuccess = onUnlocked,
                onError = { errorMsg = it }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "HaneCüzdan Kilitli",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (!showRecovery) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                label = { Text("Uygulama Parolası") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (securityPreferences.verifyPassword(password)) {
                        onUnlocked()
                    } else {
                        errorMsg = "Parola hatalı!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Giriş Yap")
            }

            if (biometricReady && context is FragmentActivity) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        BiometricAuthHelper.authenticate(
                            activity = context,
                            onSuccess = onUnlocked,
                            onError = { errorMsg = it }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Text(" Parmak İzi / Yüz Tanıma")
                }
            }

            TextButton(onClick = { showRecovery = true; errorMsg = null }) {
                Text("Parolamı Unuttum (Gizli Soru)")
            }
        } else {
            Text(
                text = "Gizli Soru: ${securityPreferences.secretQuestion.ifBlank { "Tanımlanmamış" }}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = secretAnswer,
                onValueChange = { secretAnswer = it },
                label = { Text("Gizli Soru Cevabı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Yeni Parola") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (newPassword.length < 4) {
                        errorMsg = "Yeni parola en az 4 karakter olmalı."
                    } else if (securityPreferences.resetPasswordWithSecret(secretAnswer, newPassword)) {
                        showRecovery = false
                        errorMsg = "Parola güncellendi. Giriş yapabilirsiniz."
                    } else {
                        errorMsg = "Gizli soru cevabı yanlış!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Parolayı Sıfırla")
            }
            TextButton(onClick = { showRecovery = false }) {
                Text("Giriş Ekranına Dön")
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
