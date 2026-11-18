package com.hanecuzdan.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanecuzdan.app.core.notification.BudgetNotificationHelper
import com.hanecuzdan.app.data.local.dao.LoanDao
import com.hanecuzdan.app.data.local.dao.SubscriptionDao
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetStatus
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.Loan
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.SyncStatusSummary
import com.hanecuzdan.app.domain.model.TimePeriod
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.repository.BudgetRepository
import com.hanecuzdan.app.domain.repository.CategoryRepository
import com.hanecuzdan.app.domain.repository.SyncRepository
import com.hanecuzdan.app.domain.repository.TransactionRepository
import com.hanecuzdan.app.domain.usecase.BudgetEvaluator
import com.hanecuzdan.app.domain.usecase.DashboardMetrics
import com.hanecuzdan.app.domain.usecase.DashboardMetricsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: TimePeriod = TimePeriod.MONTHLY,
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val syncStatus: SyncStatusSummary = SyncStatusSummary()
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val syncRepository: SyncRepository,
    private val subscriptionDao: SubscriptionDao,
    private val loanDao: LoanDao,
    private val metricsCalculator: DashboardMetricsCalculator,
    private val budgetEvaluator: BudgetEvaluator,
    private val notificationHelper: BudgetNotificationHelper
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(TimePeriod.MONTHLY)

    private val baseFlow = combine(
        transactionRepository.observeAllTransactions(),
        categoryRepository.observeCategories(),
        budgetRepository.observeBudgets(),
        subscriptionDao.observeAll().map { list -> list.map { it.toDomain() } },
        loanDao.observeAll().map { list -> list.map { it.toDomain() } }
    ) { transactions, categories, budgets, subscriptions, loans ->
        Quint(transactions, categories, budgets, subscriptions, loans)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        baseFlow,
        syncRepository.syncStatus,
        selectedPeriod
    ) { (transactions, categories, budgets, subscriptions, loans), syncStatus, period ->
        val now = System.currentTimeMillis()
        val filtered = metricsCalculator.filterByPeriod(transactions, period, now)
        val metrics = metricsCalculator.calculate(transactions, budgets, period, now, subscriptions, loans)
        val budgetStatuses = budgetEvaluator.evaluate(budgets, transactions, now)

        DashboardUiState(
            isLoading = false,
            selectedPeriod = period,
            transactions = transactions,
            filteredTransactions = filtered,
            categories = categories,
            budgets = budgets,
            budgetStatuses = budgetStatuses,
            subscriptions = subscriptions,
            loans = loans,
            metrics = metrics,
            syncStatus = syncStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun selectPeriod(period: TimePeriod) {
        selectedPeriod.value = period
    }

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.upsertTransaction(transaction)
            val allTxs = transactionRepository.getAllSnapshot()
            val allBudgets = budgetRepository.getAllSnapshot()
            val statuses = budgetEvaluator.evaluate(allBudgets, allTxs)
            notificationHelper.notifyIfNeeded(statuses)
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.triggerSync()
        }
    }

    private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val budgetRepository: BudgetRepository,
        private val syncRepository: SyncRepository,
        private val subscriptionDao: SubscriptionDao,
        private val loanDao: LoanDao,
        private val metricsCalculator: DashboardMetricsCalculator,
        private val budgetEvaluator: BudgetEvaluator,
        private val notificationHelper: BudgetNotificationHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                transactionRepository,
                categoryRepository,
                budgetRepository,
                syncRepository,
                subscriptionDao,
                loanDao,
                metricsCalculator,
                budgetEvaluator,
                notificationHelper
            ) as T
        }
    }
}
