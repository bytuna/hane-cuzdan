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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.ui.components.SwipeableTransactionItem
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import java.util.Calendar

@Composable
fun ScopeTransactionsScreen(
    title: String,
    subtitle: String,
    scope: DataScope,
    allTransactions: List<Transaction>,
    subscriptions: List<Subscription> = emptyList(),
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
) {
    val filteredTxs = remember(allTransactions, scope) {
        allTransactions.filter { it.scope == scope }
    }
    val scopedSubs = remember(subscriptions, scope) {
        subscriptions.filter { it.scope == scope && it.isActive }
    }
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (scopedSubs.isNotEmpty()) {
            item {
                val subTitle = if (scope == DataScope.FAMILY) "Aile Abonelikleri" else "Kişisel Abonelikler"
                Text(subTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(scopedSubs, key = { "scope_sub_${it.id}" }) { sub ->
                val isUpcoming = sub.billingDay > todayDay
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(sub.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isUpcoming) "Ayın ${sub.billingDay}. günü • Gelecek Ödeme" else "Ayın ${sub.billingDay}. günü • Bu ay işlendi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = formatTurkishCurrency(sub.amount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text("İşlemler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (filteredTxs.isEmpty()) {
            item {
                Text("Bu bölümde henüz işlem kaydı bulunmuyor.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(filteredTxs, key = { it.id }) { tx ->
                SwipeableTransactionItem(
                    transaction = tx,
                    onDelete = onDelete,
                    onEdit = onEdit
                )
            }
        }
    }
}
