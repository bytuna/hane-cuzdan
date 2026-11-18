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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.Loan
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import com.hanecuzdan.app.domain.usecase.LoanCalculator
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Tıklanabilir Aylık Harcama modülünün detay ekranı (tekil giderler + günü gelmiş abonelikler). */
@Composable
fun MonthlyExpensesScreen(
    allTransactions: List<Transaction>,
    subscriptions: List<Subscription>,
    loans: List<Loan> = emptyList()
) {
    val now = remember { System.currentTimeMillis() }
    val monthStart = remember(now) {
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    val monthlyTxs = remember(allTransactions, monthStart) {
        allTransactions.filter { it.type == TransactionType.EXPENSE && it.timestamp >= monthStart }
    }
    val dueSubs = remember(subscriptions, todayDay) {
        subscriptions.filter { it.isActive && it.billingDay <= todayDay }
    }
    val dueLoans = remember(loans, todayDay, now) {
        loans.map { LoanCalculator.summarize(it, now) }
            .filter { it.isActive && it.loan.paymentDay <= todayDay }
    }

    val totalAmount = remember(monthlyTxs, dueSubs, dueLoans) {
        monthlyTxs.sumOf { it.amount } +
            dueSubs.sumOf { it.monthlyEquivalent() } +
            dueLoans.sumOf { it.monthlyInstallment }
    }

    val categoryTotals = remember(monthlyTxs, dueSubs, dueLoans) {
        val map = mutableMapOf<String, Double>()
        monthlyTxs.forEach { tx ->
            map[tx.categoryName] = (map[tx.categoryName] ?: 0.0) + tx.amount
        }
        dueSubs.forEach { sub ->
            map[sub.categoryName] = (map[sub.categoryName] ?: 0.0) + sub.monthlyEquivalent()
        }
        dueLoans.forEach { summary ->
            map["Banka Kredi"] = (map["Banka Kredi"] ?: 0.0) + summary.monthlyInstallment
        }
        map.entries.sortedByDescending { it.value }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("tr", "TR")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Aylık Harcama Detayları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Bu Ay Toplam Harcama", style = MaterialTheme.typography.labelMedium, color = ExpenseRed)
                    Text(
                        text = formatTurkishCurrency(totalAmount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpenseRed
                    )
                    Text(
                        text = "${monthlyTxs.size} işlem + ${dueSubs.size} abonelik + ${dueLoans.size} kredi taksiti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (categoryTotals.isNotEmpty()) {
            item {
                Text("Kategori Dağılımı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(categoryTotals, key = { it.key }) { (catName, amount) ->
                val ratio = if (totalAmount > 0.0) (amount / totalAmount).toFloat().coerceIn(0f, 1f) else 0f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(catName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${formatTurkishCurrency(amount)} (%${(ratio * 100).toInt()})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(progress = { ratio }, color = ExpenseRed, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (dueSubs.isNotEmpty()) {
            item {
                Text("Ödeme Günü Gelmiş Abonelikler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(dueSubs, key = { "sub_${it.id}" }) { sub ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sub.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Ayın ${sub.billingDay}. günü • Abonelik", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("-${formatTurkishCurrency(sub.monthlyEquivalent())}", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Harcama İşlemleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(monthlyTxs, key = { it.id }) { tx ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${tx.categoryName} • ${dateFormat.format(Date(tx.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("-${formatTurkishCurrency(tx.amount)}", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
