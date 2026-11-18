package com.hanecuzdan.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.usecase.CategorySlice
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.theme.IncomeGreen
import com.hanecuzdan.app.ui.util.formatTurkishCurrency

private val ChartPalette = listOf(
    Color(0xFF00897B),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFFF4511E),
    Color(0xFFFB8C00),
    Color(0xFF3949AB)
)

@Composable
fun AnalyticsSection(
    slices: List<CategorySlice>,
    periodIncome: Double,
    periodExpense: Double,
    modifier: Modifier = Modifier
) {
    val totalExpense = slices.sumOf { it.amount }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Harcama Analizi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
                    DonutChart(slices = slices, modifier = Modifier.size(132.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Toplam",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTurkishCurrency(totalExpense, showDecimals = false),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (slices.isEmpty()) {
                        Text(
                            text = "Bu dönem için gider kaydı yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val safeTotal = totalExpense.takeIf { it > 0.0 } ?: 1.0
                        slices.take(4).forEachIndexed { idx, slice ->
                            val pct = ((slice.amount / safeTotal) * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(ChartPalette[idx % ChartPalette.size], CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = slice.categoryName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "%$pct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            IncomeExpenseBarChart(income = periodIncome, expense = periodExpense)

            if (slices.isNotEmpty()) {
                HorizontalDivider()
                CategoryBreakdownTable(slices = slices, totalExpense = totalExpense)
            }
        }
    }
}

@Composable
private fun CategoryBreakdownTable(slices: List<CategorySlice>, totalExpense: Double) {
    val safeTotal = totalExpense.takeIf { it > 0.0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Kategori Analiz Tablosu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("Adet • Tutar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        slices.take(6).forEachIndexed { idx, slice ->
            val pct = ((slice.amount / safeTotal) * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ChartPalette[idx % ChartPalette.size], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(slice.categoryName, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "${slice.count}x • ${formatTurkishCurrency(slice.amount, false)} (%$pct)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DonutChart(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.amount }.takeIf { it > 0.0 } ?: 1.0
    Canvas(modifier = modifier) {
        val strokeWidth = 30f
        if (slices.isEmpty()) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.35f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        } else {
            var startAngle = -90f
            slices.forEachIndexed { idx, slice ->
                val sweep = ((slice.amount / total) * 360f).toFloat()
                drawArc(
                    color = ChartPalette[idx % ChartPalette.size],
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun IncomeExpenseBarChart(income: Double, expense: Double) {
    val maxVal = maxOf(income, expense, 1.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Gelir: ${formatTurkishCurrency(income, showDecimals = false)}", style = MaterialTheme.typography.labelMedium, color = IncomeGreen)
            Text("Gider: ${formatTurkishCurrency(expense, showDecimals = false)}", style = MaterialTheme.typography.labelMedium, color = ExpenseRed)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val barHeight = 10.dp.toPx()
            val incomeWidth = ((income / maxVal) * size.width).toFloat()
            val expenseWidth = ((expense / maxVal) * size.width).toFloat()

            drawRoundRect(
                color = IncomeGreen.copy(alpha = 0.15f),
                topLeft = Offset.Zero,
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = IncomeGreen,
                topLeft = Offset.Zero,
                size = Size(incomeWidth.coerceAtLeast(8f), barHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = ExpenseRed.copy(alpha = 0.15f),
                topLeft = Offset(0f, barHeight + 6.dp.toPx()),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = ExpenseRed,
                topLeft = Offset(0f, barHeight + 6.dp.toPx()),
                size = Size(expenseWidth.coerceAtLeast(8f), barHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}
