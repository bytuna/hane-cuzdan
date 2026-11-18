package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.usecase.CategoryComparison
import com.hanecuzdan.app.domain.usecase.RecommendationSeverity
import com.hanecuzdan.app.domain.usecase.SpendingInsightsEngine
import com.hanecuzdan.app.domain.usecase.SpendingInsightsReport
import com.hanecuzdan.app.domain.usecase.SpendingPaceStatus
import com.hanecuzdan.app.domain.usecase.SpendingRecommendation
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.theme.IncomeGreen
import com.hanecuzdan.app.ui.theme.WarningAmber
import com.hanecuzdan.app.ui.util.formatTurkishCurrency
import kotlin.math.roundToInt

@Composable
fun SmartInsightsScreen(
    transactions: List<Transaction>,
    monthlyBudgetPool: Double,
    subscriptions: List<Subscription>
) {
    val engine = remember { SpendingInsightsEngine() }
    val report = remember(transactions, monthlyBudgetPool, subscriptions) {
        engine.analyze(transactions, monthlyBudgetPool, subscriptions)
    }

    var reductionPercent by remember { mutableFloatStateOf(15f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Akıllı Danışman & Tahmin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        item { PaceMarkerCard(report = report) }

        item { RunwayBurnDownChartCard(report = report) }

        item {
            WhatIfSimulatorCard(
                projectedExpense = report.projectedNextMonthExpense,
                reductionPercent = reductionPercent,
                onReductionChange = { reductionPercent = it }
            )
        }

        if (report.categoryComparisons.isNotEmpty()) {
            item { CategoryComparisonCard(comparisons = report.categoryComparisons) }
        }

        item {
            Text(
                text = "Gelecek Ay İçin Tasarruf Tavsiyeleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(report.recommendations) { rec ->
            RecommendationCard(rec = rec)
        }
    }
}

@Composable
private fun PaceMarkerCard(report: SpendingInsightsReport) {
    val (statusLabel, statusColor) = when (report.paceStatus) {
        SpendingPaceStatus.UNDER_PACE -> "Harcama Hızınız İdeal (Tasarruflu)" to IncomeGreen
        SpendingPaceStatus.ON_PACE -> "Harcama Hızınız Dengede" to MaterialTheme.colorScheme.primary
        SpendingPaceStatus.OVER_PACE -> "Harcama Hızınız Yüksek!" to ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Aylık Harcama Hızı (Pace)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${report.todayDay}/${report.daysInMonth}. Gün", style = MaterialTheme.typography.labelMedium)
            }

            Text(statusLabel, style = MaterialTheme.typography.labelLarge, color = statusColor, fontWeight = FontWeight.SemiBold)

            // Pace Marker çubuğu
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val trackHeight = 12.dp.toPx()
                val topY = (size.height - trackHeight) / 2f
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                val fillW = (report.actualSpendRatio.coerceIn(0f, 1f) * size.width)
                drawRoundRect(
                    color = statusColor,
                    topLeft = Offset(0f, topY),
                    size = Size(fillW.coerceAtLeast(6f), trackHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Bugün referans çizgisi
                val idealX = (report.idealPaceRatio.coerceIn(0f, 1f) * size.width)
                drawLine(
                    color = Color.Black.copy(alpha = 0.7f),
                    start = Offset(idealX, 0f),
                    end = Offset(idealX, size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Harcanan: %${(report.actualSpendRatio * 100).roundToInt()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Günlük Güvenli Limit: ${formatTurkishCurrency(report.dailySafeSpend, false)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RunwayBurnDownChartCard(report: SpendingInsightsReport) {
    val cumulative = report.dailyCumulativeSpend
    val maxVal = maxOf(
        report.monthlyBudgetPool,
        cumulative.maxOrNull() ?: 0.0,
        1.0
    )
    val curveColor = if (report.paceStatus == SpendingPaceStatus.OVER_PACE) ExpenseRed else IncomeGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bütçe Yanma Eğrisi (İdeal vs Gerçek)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val totalDays = report.daysInMonth.coerceAtLeast(2)
                val stepX = size.width / (totalDays - 1).toFloat()

                // İdeal bütçe çizgisi (kesikli)
                val idealEndY = if (report.monthlyBudgetPool > 0.0) {
                    size.height - ((report.monthlyBudgetPool / maxVal).toFloat() * size.height)
                } else size.height * 0.2f

                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, idealEndY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                )

                // Gerçekleşen harcama eğrisi
                if (cumulative.isNotEmpty()) {
                    val path = Path()
                    cumulative.forEachIndexed { idx, value ->
                        val x = idx * stepX
                        val y = size.height - ((value / maxVal).toFloat().coerceIn(0f, 1f) * size.height)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = curveColor,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).background(Color.Gray, CircleShape))
                    Text("İdeal Tempo", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).background(curveColor, CircleShape))
                    Text("Gerçekleşen Harcama", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun WhatIfSimulatorCard(
    projectedExpense: Double,
    reductionPercent: Float,
    onReductionChange: (Float) -> Unit
) {
    val monthlySaving = projectedExpense * (reductionPercent / 100.0)
    val newProjected = (projectedExpense - monthlySaving).coerceAtLeast(0.0)
    val yearlySaving = monthlySaving * 12.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = IncomeGreen)
                Text("Gelecek Ay Tasarruf Simülatörü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Gelecek ay harcamaları %${reductionPercent.roundToInt()} azaltırsam:",
                style = MaterialTheme.typography.bodyMedium
            )

            Slider(
                value = reductionPercent,
                onValueChange = onReductionChange,
                valueRange = 0f..40f
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Yeni Tahmini Gider", style = MaterialTheme.typography.labelSmall)
                    Text(formatTurkishCurrency(newProjected, false), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Aylık / Yıllık Tasarruf", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "+${formatTurkishCurrency(monthlySaving, false)} / +${formatTurkishCurrency(yearlySaving, false)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryComparisonCard(comparisons: List<CategoryComparison>) {
    val maxVal = comparisons.maxOfOrNull { maxOf(it.thisMonth, it.lastMonth) }?.takeIf { it > 0.0 } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Geçen Ay vs Bu Ay Karşılaştırma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            comparisons.take(5).forEach { comp ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(comp.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        val sign = if (comp.changePercent > 0) "+" else ""
                        val pctColor = if (comp.changePercent > 15) ExpenseRed else if (comp.changePercent < 0) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(
                            text = "${formatTurkishCurrency(comp.thisMonth, false)} ($sign%${comp.changePercent})",
                            style = MaterialTheme.typography.labelMedium,
                            color = pctColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // İkili yatay karşılaştırma barı
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((comp.lastMonth / maxVal).toFloat().coerceIn(0.05f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((comp.thisMonth / maxVal).toFloat().coerceIn(0.05f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (comp.changePercent > 15) ExpenseRed else MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Text("Gri: Geçen Ay • Renkli: Bu Ay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecommendationCard(rec: SpendingRecommendation) {
    val (color, icon) = when (rec.severity) {
        RecommendationSeverity.CRITICAL -> ExpenseRed to Icons.Default.Warning
        RecommendationSeverity.WARNING -> WarningAmber to Icons.Default.Warning
        RecommendationSeverity.INFO -> MaterialTheme.colorScheme.primary to Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(rec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                Text(rec.description, style = MaterialTheme.typography.bodySmall)
                if (rec.potentialMonthlySaving > 0.0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = IncomeGreen.copy(alpha = 0.15f)) {
                        Text(
                            text = "Potansiyel Tasarruf: ~${formatTurkishCurrency(rec.potentialMonthlySaving, false)}/ay",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
