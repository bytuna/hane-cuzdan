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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.hanecuzdan.app.domain.model.BillingCycle
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.ui.util.TurkishCurrencyVisualTransformation
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import com.hanecuzdan.app.ui.util.parseTurkishCurrency
import com.hanecuzdan.app.ui.util.sanitizeCurrencyInput
import java.util.Calendar
import java.util.UUID

@Composable
fun SubscriptionsScreen(
    subscriptions: List<Subscription>,
    onAddSubscription: (Subscription) -> Unit,
    onDeleteSubscription: (String) -> Unit,
    onRecordPayment: (Subscription) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf("1") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var scope by remember { mutableStateOf(DataScope.FAMILY) }

    val monthlyTotal = remember(subscriptions) {
        subscriptions.filter { it.isActive }.sumOf { it.monthlyEquivalent() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Abonelikler & Sabit Ödemeler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Aylık Sabit Yük", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatTurkishCurrency(monthlyTotal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(Icons.Default.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Abonelik / Fatura Adı (Örn: Netflix, Aidat)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeCurrencyInput(it) },
                label = { Text("Tutar (₺)") },
                visualTransformation = TurkishCurrencyVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
            OutlinedTextField(
                value = dayText,
                onValueChange = { dayText = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("Ödeme Günü") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = cycle == BillingCycle.MONTHLY,
                    onClick = { cycle = BillingCycle.MONTHLY },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Aylık") }
                SegmentedButton(
                    selected = cycle == BillingCycle.YEARLY,
                    onClick = { cycle = BillingCycle.YEARLY },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Yıllık") }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = scope == DataScope.FAMILY,
                    onClick = { scope = DataScope.FAMILY },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Aile") }
                SegmentedButton(
                    selected = scope == DataScope.PERSONAL,
                    onClick = { scope = DataScope.PERSONAL },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Kişisel") }
            }
        }

        Button(
            onClick = {
                val amount = parseTurkishCurrency(amountText) ?: return@Button
                if (name.isBlank()) return@Button
                val day = (dayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                onAddSubscription(
                    Subscription(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        amount = amount,
                        cycle = cycle,
                        billingDay = day,
                        categoryName = "Fatura",
                        scope = scope
                    )
                )
                name = ""
                amountText = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(" Abonelik Ekle")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            items(subscriptions, key = { it.id }) { sub ->
                val isUpcoming = sub.billingDay > todayDay
                val statusHint = if (isUpcoming) {
                    "Gelecek Ödemeler (${sub.billingDay - todayDay} gün sonra)"
                } else {
                    "Bu ay harcama olarak işlendi"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Her ayın ${sub.billingDay}. günü • ${formatTurkishCurrency(sub.amount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = statusHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUpcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onRecordPayment(sub) }) {
                                Icon(Icons.Default.Receipt, contentDescription = null)
                                Text(" İşle")
                            }
                            IconButton(onClick = { onDeleteSubscription(sub.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil")
                            }
                        }
                    }
                }
            }
        }
    }
}
