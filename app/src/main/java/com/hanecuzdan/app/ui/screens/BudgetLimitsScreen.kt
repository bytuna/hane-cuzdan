package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetPeriod
import com.hanecuzdan.app.domain.model.BudgetStatus
import com.hanecuzdan.app.domain.model.BudgetThresholdLevel
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.theme.IncomeGreen
import com.hanecuzdan.app.ui.theme.WarningAmber
import com.hanecuzdan.app.ui.util.TurkishCurrencyVisualTransformation
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import com.hanecuzdan.app.ui.util.parseTurkishCurrency
import com.hanecuzdan.app.ui.util.sanitizeCurrencyInput
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetLimitsScreen(
    categories: List<Category>,
    statuses: List<BudgetStatus>,
    onSaveBudget: (Budget) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var limitText by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) } // null -> Toplam Bütçe
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bütçe Limitleri", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = period == BudgetPeriod.MONTHLY,
                onClick = { period = BudgetPeriod.MONTHLY },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Aylık") }
            SegmentedButton(
                selected = period == BudgetPeriod.WEEKLY,
                onClick = { period = BudgetPeriod.WEEKLY },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Haftalık") }
        }

        Text("Kategori Seçimi", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Toplam Bütçe") }
            )
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory?.id == cat.id,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.name) }
                )
            }
        }

        OutlinedTextField(
            value = limitText,
            onValueChange = { limitText = sanitizeCurrencyInput(it) },
            label = { Text("Limit Tutarı (₺) — ${selectedCategory?.name ?: "Toplam Bütçe"}") },
            placeholder = { Text("Örn: 25.000,00") },
            suffix = { Text("₺") },
            visualTransformation = TurkishCurrencyVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val limit = parseTurkishCurrency(limitText) ?: return@Button
                val cat = selectedCategory
                onSaveBudget(
                    Budget(
                        id = UUID.randomUUID().toString(),
                        categoryId = cat?.id,
                        categoryName = cat?.name ?: "Toplam Bütçe",
                        limitAmount = limit,
                        period = period
                    )
                )
                limitText = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Limit Ekle")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(statuses, key = { it.budget.id }) { status ->
                val color = when (status.thresholdLevel) {
                    BudgetThresholdLevel.EXCEEDED_100 -> ExpenseRed
                    BudgetThresholdLevel.WARNING_80 -> WarningAmber
                    BudgetThresholdLevel.NONE -> IncomeGreen
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${status.budget.categoryName} (${if (status.budget.period == BudgetPeriod.MONTHLY) "Aylık" else "Haftalık"})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = { editingBudget = status.budget }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                                }
                                IconButton(onClick = { onDeleteBudget(status.budget.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { status.usageRatio.toFloat().coerceIn(0f, 1f) },
                            color = color,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${formatTurkishCurrency(status.spentAmount)} / ${formatTurkishCurrency(status.budget.limitAmount)} (%${(status.usageRatio * 100).toInt()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    }
                }
            }
        }
    }

    editingBudget?.let { budget ->
        EditBudgetDialog(
            budget = budget,
            onDismiss = { editingBudget = null },
            onConfirm = { updated ->
                onSaveBudget(updated)
                editingBudget = null
            }
        )
    }
}

@Composable
private fun EditBudgetDialog(
    budget: Budget,
    onDismiss: () -> Unit,
    onConfirm: (Budget) -> Unit
) {
    var limitText by remember {
        mutableStateOf(sanitizeCurrencyInput(budget.limitAmount.toString().replace('.', ',')))
    }
    var period by remember { mutableStateOf(budget.period) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${budget.categoryName} Limitini Düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = sanitizeCurrencyInput(it) },
                    label = { Text("Yeni Limit Tutarı (₺)") },
                    suffix = { Text("₺") },
                    visualTransformation = TurkishCurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = period == BudgetPeriod.MONTHLY,
                        onClick = { period = BudgetPeriod.MONTHLY },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Aylık") }
                    SegmentedButton(
                        selected = period == BudgetPeriod.WEEKLY,
                        onClick = { period = BudgetPeriod.WEEKLY },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Haftalık") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newLimit = parseTurkishCurrency(limitText) ?: return@TextButton
                    if (newLimit > 0.0) {
                        onConfirm(budget.copy(limitAmount = newLimit, period = period))
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
