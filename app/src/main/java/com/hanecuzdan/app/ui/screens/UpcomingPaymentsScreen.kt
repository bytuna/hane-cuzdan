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
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.ui.theme.WarningAmber
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import java.util.Calendar

/** Bu ay içerisinde ödeme tarihi bugünden ileride olan abonelikleri listeler. */
@Composable
fun UpcomingPaymentsScreen(
    subscriptions: List<Subscription>,
    onRecordPayment: (Subscription) -> Unit
) {
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val upcoming = remember(subscriptions, todayDay) {
        subscriptions.filter { it.isActive && it.billingDay > todayDay }
            .sortedBy { it.billingDay }
    }
    val totalUpcoming = remember(upcoming) { upcoming.sumOf { it.monthlyEquivalent() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Gelecek Ödemeler (Bu Ay)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Bu Ay Bekleyen Toplam", style = MaterialTheme.typography.labelMedium, color = WarningAmber)
                    Text(
                        text = formatTurkishCurrency(totalUpcoming),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarningAmber
                    )
                }
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = WarningAmber)
            }
        }

        if (upcoming.isEmpty()) {
            Text(
                text = "Bu ay için ileriki bir tarihte bekleyen abonelik ödemesi yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(upcoming, key = { it.id }) { sub ->
                    val daysLeft = sub.billingDay - todayDay
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
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(sub.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Ayın ${sub.billingDay}. günü ($daysLeft gün kaldı) • ${formatTurkishCurrency(sub.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmber
                                )
                            }
                            OutlinedButton(onClick = { onRecordPayment(sub) }) {
                                Icon(Icons.Default.Receipt, contentDescription = null)
                                Text(" Erken İşle")
                            }
                        }
                    }
                }
            }
        }
    }
}
