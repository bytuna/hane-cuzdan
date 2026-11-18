package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.ui.util.TurkishCurrencyVisualTransformation
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import com.hanecuzdan.app.ui.util.parseTurkishCurrency
import com.hanecuzdan.app.ui.util.sanitizeCurrencyInput

/** Splitwise ilhamlı hızlı aile/grup masraf bölüşme hesaplayıcısı. */
@Composable
fun ExpenseSplitScreen() {
    var totalText by remember { mutableStateOf("") }
    var peopleCountText by remember { mutableStateOf("2") }
    var payerName by remember { mutableStateOf("Ben") }

    val total = parseTurkishCurrency(totalText) ?: 0.0
    val peopleCount = (peopleCountText.toIntOrNull() ?: 2).coerceAtLeast(1)
    val perPerson = if (total > 0.0) total / peopleCount else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ortak Masraf Bölüşme", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "Aile veya arkadaş grubu harcamalarında kişi başı düşen tutarı ve ödeme planını anında hesaplayın.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = totalText,
            onValueChange = { totalText = sanitizeCurrencyInput(it) },
            label = { Text("Toplam Harcama Tutarı (₺)") },
            visualTransformation = TurkishCurrencyVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = peopleCountText,
                onValueChange = { peopleCountText = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("Kişi Sayısı") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = payerName,
                onValueChange = { payerName = it },
                label = { Text("Ödeyen Kişi") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Kişi Başı Düşen Tutar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = formatTurkishCurrency(perPerson),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (total > 0.0 && peopleCount > 1) {
                    Text(
                        text = "Diğer ${peopleCount - 1} kişinin her biri ${payerName.ifBlank { "ödeyen kişiye" }} ${formatTurkishCurrency(perPerson)} ödemelidir.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
