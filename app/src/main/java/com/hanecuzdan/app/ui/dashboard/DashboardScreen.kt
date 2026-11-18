package com.hanecuzdan.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.BudgetThresholdLevel
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.TimePeriod
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.ui.components.AnalyticsSection
import com.hanecuzdan.app.ui.components.DashboardSkeletonLoader
import com.hanecuzdan.app.ui.components.SummaryCardsGrid
import com.hanecuzdan.app.ui.components.SwipeableTransactionItem
import com.hanecuzdan.app.ui.theme.ExpenseRed
import com.hanecuzdan.app.ui.theme.WarningAmber

@Composable
fun DashboardContent(
    state: DashboardUiState,
    onPeriodSelected: (TimePeriod) -> Unit,
    onAddClick: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onSyncClick: () -> Unit,
    onMonthlyExpenseClick: () -> Unit = {},
    onUpcomingPaymentsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf<DataScope?>(null) }

    val displayedTransactions = remember(state.filteredTransactions, searchQuery, scopeFilter) {
        state.filteredTransactions.filter { tx ->
            val matchesScope = scopeFilter == null || tx.scope == scopeFilter
            val matchesQuery = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.categoryName.contains(searchQuery, ignoreCase = true)
            matchesScope && matchesQuery
        }
    }

    val alertBudgets = remember(state.budgetStatuses) {
        state.budgetStatuses.filter { it.thresholdLevel != BudgetThresholdLevel.NONE }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "İşlem Ekle")
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            DashboardSkeletonLoader(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCardsGrid(
                    metrics = state.metrics,
                    syncStatus = state.syncStatus,
                    onSyncClick = onSyncClick,
                    onQuickAdd = onAddClick,
                    onMonthlyExpenseClick = onMonthlyExpenseClick,
                    onUpcomingPaymentsClick = onUpcomingPaymentsClick
                )
            }

            item {
                PeriodFilterRow(
                    selected = state.selectedPeriod,
                    onSelected = onPeriodSelected
                )
            }

            item {
                AnalyticsSection(
                    slices = state.metrics.categorySlices,
                    periodIncome = state.metrics.periodIncome,
                    periodExpense = state.metrics.periodExpense
                )
            }

            if (alertBudgets.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        alertBudgets.forEach { status ->
                            val isExceeded = status.thresholdLevel == BudgetThresholdLevel.EXCEEDED_100
                            val alertColor = if (isExceeded) ExpenseRed else WarningAmber
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = alertColor)
                                        Text(
                                            text = if (isExceeded) {
                                                "${status.budget.categoryName} bütçe limiti aşıldı (%${(status.usageRatio * 100).toInt()})"
                                            } else {
                                                "${status.budget.categoryName} limitinin %${(status.usageRatio * 100).toInt()}'ine ulaşıldı"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = alertColor
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { status.usageRatio.toFloat().coerceIn(0f, 1f) },
                                        color = alertColor,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Son İşlemler (Sil: Sola / Düzenle: Sağa Kaydır)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("İşlem veya kategori ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = scopeFilter == null,
                            onClick = { scopeFilter = null },
                            label = { Text("Tümü") }
                        )
                        FilterChip(
                            selected = scopeFilter == DataScope.FAMILY,
                            onClick = { scopeFilter = DataScope.FAMILY },
                            label = { Text("Aile") }
                        )
                        FilterChip(
                            selected = scopeFilter == DataScope.PERSONAL,
                            onClick = { scopeFilter = DataScope.PERSONAL },
                            label = { Text("Kişisel") }
                        )
                    }
                }
            }

            if (displayedTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Bu zaman diliminde henüz işlem yok.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Sağ alttaki + butonuna basarak Aile veya Kişisel harcama ekleyebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedTransactions, key = { it.id }) { tx ->
                    SwipeableTransactionItem(
                        transaction = tx,
                        onDelete = onDeleteTransaction,
                        onEdit = onEditTransaction
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selected: TimePeriod,
    onSelected: (TimePeriod) -> Unit
) {
    val labels = mapOf(
        TimePeriod.DAILY to "Günlük",
        TimePeriod.WEEKLY to "Haftalık",
        TimePeriod.MONTHLY to "Aylık",
        TimePeriod.YEARLY to "Yıllık"
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TimePeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(index, TimePeriod.entries.size)
            ) {
                Text(labels[period].orEmpty(), maxLines = 1)
            }
        }
    }
}
