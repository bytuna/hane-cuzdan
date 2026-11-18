package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Loan
import com.hanecuzdan.app.domain.usecase.LoanCalculator
import com.hanecuzdan.app.domain.usecase.LoanSummary
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.theme.IncomeGreen
import com.hanecuzdan.app.ui.util.TurkishCurrencyVisualTransformation
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import com.hanecuzdan.app.ui.util.parseTurkishCurrency
import com.hanecuzdan.app.ui.util.sanitizeCurrencyInput
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loans: List<Loan>,
    onAddLoan: (Loan) -> Unit,
    onDeleteLoan: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var principalText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var installmentsText by remember { mutableStateOf("12") }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var paymentDayText by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString())
    }
    var scope by remember { mutableStateOf(DataScope.FAMILY) }
    var showStartPicker by remember { mutableStateOf(false) }
    var selectedSummaryForSchedule by remember { mutableStateOf<LoanSummary?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("tr", "TR")) }
    val summaries = remember(loans) { loans.map { LoanCalculator.summarize(it) } }
    val totalRemainingDebt = remember(summaries) { summaries.sumOf { it.remainingDebtAmount } }
    val monthlyLoanLoad = remember(summaries) { summaries.filter { it.isActive }.sumOf { it.monthlyInstallment } }

    // Canlı form önizlemesi
    val previewPrincipal = parseTurkishCurrency(principalText) ?: 0.0
    val previewRate = rateText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val previewInstallments = (installmentsText.toIntOrNull() ?: 0).coerceAtLeast(0)
    val previewMonthly = if (previewPrincipal > 0.0 && previewInstallments > 0) {
        LoanCalculator.calculateMonthlyInstallment(previewPrincipal, previewRate, previewInstallments)
    } else 0.0
    val previewEndMillis = if (previewInstallments > 0) {
        LoanCalculator.calculateEndDateMillis(startMillis, previewInstallments)
    } else startMillis

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kredi & Taksit Yönetimi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Kalan Toplam Kredi Borcu", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatTurkishCurrency(totalRemainingDebt),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Aylık Taksit Yükü: ${formatTurkishCurrency(monthlyLoanLoad)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kredi Adı (Örn: Konut Kredisi, Araç)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = sanitizeCurrencyInput(it) },
                    label = { Text("Anapara Tutarı (₺)") },
                    suffix = { Text("₺") },
                    visualTransformation = TurkishCurrencyVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                        label = { Text("Aylık Faiz (%)") },
                        placeholder = { Text("Örn: 3,49") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = installmentsText,
                        onValueChange = { installmentsText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Taksit (Ay)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = paymentDayText,
                        onValueChange = { paymentDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Ödeme Günü") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedButton(
                    onClick = { showStartPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Text(" Başlangıç: ${dateFormat.format(Date(startMillis))} • Bitiş: ${dateFormat.format(Date(previewEndMillis))}")
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = scope == DataScope.FAMILY,
                        onClick = { scope = DataScope.FAMILY },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Aile Havuzu") }
                    SegmentedButton(
                        selected = scope == DataScope.PERSONAL,
                        onClick = { scope = DataScope.PERSONAL },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Kişisel") }
                }

                if (previewMonthly > 0.0) {
                    Text(
                        text = "Hesaplanan Aylık Taksit: ${formatTurkishCurrency(previewMonthly)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val principal = parseTurkishCurrency(principalText) ?: return@Button
                        val count = (installmentsText.toIntOrNull() ?: 0)
                        if (name.isBlank() || principal <= 0.0 || count <= 0) return@Button
                        val rate = rateText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val payDay = (paymentDayText.toIntOrNull() ?: 1).coerceIn(1, 31)

                        onAddLoan(
                            Loan(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                principalAmount = principal,
                                monthlyInterestRate = rate,
                                totalInstallments = count,
                                startDateMillis = startMillis,
                                paymentDay = payDay,
                                scope = scope
                            )
                        )
                        name = ""
                        principalText = ""
                        rateText = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Krediyi Kaydet")
                }
            }
        }

        items(summaries, key = { it.loan.id }) { summary ->
            val progress = if (summary.loan.totalInstallments > 0) {
                summary.elapsedInstallments.toFloat() / summary.loan.totalInstallments.toFloat()
            } else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedSummaryForSchedule = summary },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(summary.loan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Aylık Taksit: ${formatTurkishCurrency(summary.monthlyInstallment)} • Ayın ${summary.loan.paymentDay}. günü",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { onDeleteLoan(summary.loan.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        color = IncomeGreen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Ödenen: ${summary.elapsedInstallments}/${summary.loan.totalInstallments} taksit",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "Bitiş: ${dateFormat.format(Date(summary.endDateMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Kalan Borç: ${formatTurkishCurrency(summary.remainingDebtAmount)} (Ödeme planı için dokunun)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            startMillis = millis
                            val cal = Calendar.getInstance().apply { timeInMillis = millis }
                            paymentDayText = cal.get(Calendar.DAY_OF_MONTH).toString()
                        }
                        showStartPicker = false
                    }
                ) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("İptal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    selectedSummaryForSchedule?.let { summary ->
        val schedule = remember(summary.loan) { LoanCalculator.buildAmortizationSchedule(summary.loan) }
        AlertDialog(
            onDismissRequest = { selectedSummaryForSchedule = null },
            title = { Text("${summary.loan.name} — Ödeme Planı") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Text(
                            text = "Toplam Geri Ödeme: ${formatTurkishCurrency(summary.totalPaybackAmount)} (Faiz: ${formatTurkishCurrency(summary.totalInterestAmount)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(schedule.take(24), key = { it.installmentNumber }) { row ->
                        val isPaid = row.installmentNumber <= summary.elapsedInstallments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${row.installmentNumber}. Taksit ${if (isPaid) "✓" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Anapara: ${formatTurkishCurrency(row.principalPart, false)} • Faiz: ${formatTurkishCurrency(row.interestPart, false)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSummaryForSchedule = null }) { Text("Kapat") }
            }
        )
    }
}
