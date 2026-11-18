package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.hanecuzdan.app.domain.model.SavingsGoal
import com.hanecuzdan.app.ui.theme.IncomeGreen
import com.hanecuzdan.app.ui.util.TurkishCurrencyVisualTransformation
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import com.hanecuzdan.app.ui.util.parseTurkishCurrency
import com.hanecuzdan.app.ui.util.sanitizeCurrencyInput
import java.util.UUID

@Composable
fun SavingsGoalsScreen(
    goals: List<SavingsGoal>,
    onUpsertGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var contributionText by remember { mutableStateOf("500") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tasarruf Hedefleri (Kumbara)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Hedef Adı (Örn: Acil Durum Fonu, Tatil)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = targetText,
            onValueChange = { targetText = sanitizeCurrencyInput(it) },
            label = { Text("Hedef Tutar (₺)") },
            visualTransformation = TurkishCurrencyVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val target = parseTurkishCurrency(targetText) ?: return@Button
                if (title.isBlank()) return@Button
                onUpsertGoal(
                    SavingsGoal(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        targetAmount = target,
                        savedAmount = 0.0
                    )
                )
                title = ""
                targetText = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Savings, contentDescription = null)
            Text(" Hedef Oluştur")
        }

        OutlinedTextField(
            value = contributionText,
            onValueChange = { contributionText = sanitizeCurrencyInput(it) },
            label = { Text("Hızlı Katkı Tutarı (₺)") },
            visualTransformation = TurkishCurrencyVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(goals, key = { it.id }) { goal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil")
                            }
                        }

                        LinearProgressIndicator(
                            progress = { goal.progressRatio },
                            color = IncomeGreen,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTurkishCurrency(goal.savedAmount, false)} / ${formatTurkishCurrency(goal.targetAmount, false)} (%${(goal.progressRatio * 100).toInt()})",
                                style = MaterialTheme.typography.bodySmall,
                                color = IncomeGreen,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedButton(
                                onClick = {
                                    val addAmount = parseTurkishCurrency(contributionText) ?: 0.0
                                    if (addAmount > 0.0) {
                                        onUpsertGoal(goal.copy(savedAmount = goal.savedAmount + addAmount))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(" Para Ekle")
                            }
                        }
                    }
                }
            }
        }
    }
}
