package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetPeriod
import com.hanecuzdan.app.domain.model.Loan
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.TimePeriod
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import java.util.Calendar

/** Kategori bazlı grafik dilimi ve analiz satırı. */
data class CategorySlice(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val count: Int = 1
)

/** Dashboard hızlı özet kartları ve grafik metrikleri. */
data class DashboardMetrics(
    val totalAssets: Double = 0.0,
    val totalBudgetPool: Double = 0.0, // Girilen Bütçe + Dönem Geliri birleşimi
    val remainingMonthlyLimit: Double = 0.0,
    val todayExpense: Double = 0.0,
    val monthlyTotalExpense: Double = 0.0, // Aylık giderler + günü gelmiş abonelikler
    val upcomingPaymentsTotal: Double = 0.0,
    val upcomingPaymentsCount: Int = 0,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val categorySlices: List<CategorySlice> = emptyList()
)

/** Dashboard metriklerini, bütçe birleşimini ve abonelik ödeme günü kurallarını hesaplar. */
class DashboardMetricsCalculator {

    fun filterByPeriod(
        transactions: List<Transaction>,
        period: TimePeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Transaction> {
        val windowMillis = when (period) {
            TimePeriod.DAILY -> 86_400_000L
            TimePeriod.WEEKLY -> 7L * 86_400_000L
            TimePeriod.MONTHLY -> 30L * 86_400_000L
            TimePeriod.YEARLY -> 365L * 86_400_000L
        }
        val cutoff = nowMillis - windowMillis
        return transactions.filter { it.timestamp >= cutoff }
    }

    fun calculate(
        allTransactions: List<Transaction>,
        budgets: List<Budget>,
        selectedPeriod: TimePeriod,
        nowMillis: Long = System.currentTimeMillis(),
        subscriptions: List<Subscription> = emptyList(),
        loans: List<Loan> = emptyList()
    ): DashboardMetrics {
        val totalIncome = allTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = allTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalAssets = totalIncome - totalExpense

        val dayStart = nowMillis - 86_400_000L
        val todayExpense = allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= dayStart }
            .sumOf { it.amount }

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        // Gerçek takvim ayı başlangıcı (Ayın 1'i saat 00:00:00) -> Ay bittiğinde otomatik yeni aya geçer
        val monthStart = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthlyTxExpense = allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= monthStart }
            .sumOf { it.amount }

        val activeSubs = subscriptions.filter { it.isActive }
        // Ödeme günü bugün veya geçmiş olan abonelikler bu ay harcama sayılır
        val dueSubsExpense = activeSubs.filter { it.billingDay <= todayDayOfMonth }.sumOf { it.monthlyEquivalent() }
        // Ödeme günü bugünden ileride olan abonelikler Gelecek Ödemeler modülünde görünür
        val upcomingSubs = activeSubs.filter { it.billingDay > todayDayOfMonth }

        // Aktif krediler (geçmiş başlangıçlı krediler dahil)
        val activeLoanSummaries = loans.map { LoanCalculator.summarize(it, nowMillis) }.filter { it.isActive }
        val dueLoansExpense = activeLoanSummaries.filter { it.loan.paymentDay <= todayDayOfMonth }.sumOf { it.monthlyInstallment }
        val upcomingLoans = activeLoanSummaries.filter { it.loan.paymentDay > todayDayOfMonth }

        val dueRecurringTotal = dueSubsExpense + dueLoansExpense
        val upcomingTotal = upcomingSubs.sumOf { it.monthlyEquivalent() } + upcomingLoans.sumOf { it.monthlyInstallment }

        val monthlyTotalExpense = monthlyTxExpense + dueRecurringTotal

        val monthlyBudgetLimit = budgets
            .firstOrNull { it.categoryId == null && it.period == BudgetPeriod.MONTHLY }
            ?.limitAmount
            ?: budgets.filter { it.period == BudgetPeriod.MONTHLY }.sumOf { it.limitAmount }

        val periodTxs = filterByPeriod(allTransactions, selectedPeriod, nowMillis)
        val periodIncome = periodTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val periodExpense = periodTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } + dueRecurringTotal

        // Bütçe + Dönem Geliri birleşimi; bütçe girilmişse bütçe havuzu eşitlenir/yansıtılır
        val totalBudgetPool = monthlyBudgetLimit + periodIncome
        val remainingMonthlyLimit = (totalBudgetPool - monthlyTotalExpense).coerceAtLeast(0.0)

        val slices = periodTxs
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId to it.categoryName }
            .map { (key, list) ->
                CategorySlice(key.first, key.second, list.sumOf { it.amount }, list.size)
            }
            .sortedByDescending { it.amount }

        return DashboardMetrics(
            totalAssets = totalAssets,
            totalBudgetPool = totalBudgetPool,
            remainingMonthlyLimit = remainingMonthlyLimit,
            todayExpense = todayExpense,
            monthlyTotalExpense = monthlyTotalExpense,
            upcomingPaymentsTotal = upcomingTotal,
            upcomingPaymentsCount = upcomingSubs.size + upcomingLoans.size,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            categorySlices = slices
        )
    }
}
