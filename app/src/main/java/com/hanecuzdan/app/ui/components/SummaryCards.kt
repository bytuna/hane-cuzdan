package com.hanecuzdan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import com.hanecuzdan.app.ui.theme.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.SyncConnectionState
import com.hanecuzdan.app.domain.model.SyncStatusSummary
import com.hanecuzdan.app.domain.usecase.DashboardMetrics
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.theme.IncomeGreen
import com.hanecuzdan.app.ui.util.formatTurkishCurrency

@Composable
fun SummaryCardsGrid(
    metrics: DashboardMetrics,
    syncStatus: SyncStatusSummary,
    onSyncClick: () -> Unit,
    onQuickAdd: () -> Unit = {},
    onMonthlyExpenseClick: () -> Unit = {},
    onUpcomingPaymentsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val savingsRate = if (metrics.periodIncome > 0.0) {
        (((metrics.periodIncome - metrics.periodExpense) / metrics.periodIncome) * 100.0)
            .toInt()
            .coerceIn(-999, 100)
    } else 0

    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Revolut / Wise tarzı yatay kaydırılabilir Hero kartları
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            when (page) {
                0 -> HeroBalanceCard(
                    title = "Kalan Bütçe",
                    mainAmount = formatTurkishCurrency(metrics.totalBudgetPool - metrics.periodExpense),
                    leftLabel = "Bütçe",
                    leftAmount = formatTurkishCurrency(metrics.totalBudgetPool, false),
                    rightLabel = "Gider",
                    rightAmount = formatTurkishCurrency(metrics.periodExpense, false),
                    gradientColors = listOf(Color(0xFF004D3A), Color(0xFF006C51), Color(0xFF00897B)),
                    syncStatus = syncStatus,
                    onSyncClick = onSyncClick
                )
                else -> HeroBalanceCard(
                    title = "Finansal Sağlık & Tasarruf",
                    mainAmount = "%$savingsRate Tasarruf",
                    leftLabel = "Kalan Limit",
                    leftAmount = formatTurkishCurrency(metrics.remainingMonthlyLimit, false),
                    rightLabel = "Bugünkü Harcama",
                    rightAmount = formatTurkishCurrency(metrics.todayExpense, false),
                    gradientColors = listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF1E88E5)),
                    syncStatus = syncStatus,
                    onSyncClick = onSyncClick
                )
            }
        }

        // Sayfa göstergesi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (selected) 20.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                )
            }
        }

        // Bento-Grid Metrik Kartları
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniMetricCard(
                title = "Bu Ay Kalan Limit",
                value = formatTurkishCurrency(metrics.remainingMonthlyLimit, showDecimals = false),
                icon = Icons.Default.Speed,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MiniMetricCard(
                title = "Aylık Harcama ↗",
                value = formatTurkishCurrency(metrics.monthlyTotalExpense, showDecimals = false),
                icon = Icons.Default.ReceiptLong,
                accentColor = ExpenseRed,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onMonthlyExpenseClick)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniMetricCard(
                title = "Gelecek Ödemeler (${metrics.upcomingPaymentsCount})",
                value = formatTurkishCurrency(metrics.upcomingPaymentsTotal, showDecimals = false),
                icon = Icons.Default.EventAvailable,
                accentColor = WarningAmber,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onUpcomingPaymentsClick)
            )
            MiniMetricCard(
                title = "Hızlı Fiş Tara",
                value = "Kamera / OCR",
                icon = Icons.Default.DocumentScanner,
                accentColor = Color(0xFF1E88E5),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onQuickAdd)
            )
        }
    }
}

@Composable
private fun HeroBalanceCard(
    title: String,
    mainAmount: String,
    leftLabel: String,
    leftAmount: String,
    rightLabel: String,
    rightAmount: String,
    gradientColors: List<Color>,
    syncStatus: SyncStatusSummary,
    onSyncClick: () -> Unit
) {
    val gradient = Brush.linearGradient(colors = gradientColors)

    val (syncText, syncIcon) = when (syncStatus.state) {
        SyncConnectionState.SYNCED -> "Güncel" to Icons.Default.CloudDone
        SyncConnectionState.SYNCING -> "Eşitleniyor" to Icons.Default.CloudSync
        SyncConnectionState.OFFLINE_PENDING -> "${syncStatus.pendingCount} Bekliyor" to Icons.Default.CloudOff
        SyncConnectionState.ERROR -> "Hata" to Icons.Default.CloudOff
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(gradient)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.clickable(onClick = onSyncClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(syncIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(syncText, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }

            Text(
                text = mainAmount,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroPill(
                    label = leftLabel,
                    amount = leftAmount,
                    icon = Icons.Default.ArrowUpward,
                    iconTint = Color(0xFF81C784),
                    modifier = Modifier.weight(1f)
                )
                HeroPill(
                    label = rightLabel,
                    amount = rightAmount,
                    icon = Icons.Default.ArrowDownward,
                    iconTint = Color(0xFFFF8A80),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    label: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.13f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                Text(amount, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun MiniMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
