package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.BillingCycle
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetPeriod
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.TimePeriod
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DashboardMetricsCalculatorTest {

    private val calculator = DashboardMetricsCalculator()

    @Test
    fun `butce ve gelir birlesimi ile bugunku harcama dogru hesaplanmali`() {
        val now = 1_700_000_000_000L
        val txs = listOf(
            Transaction("1", "Maaş", 30000.0, TransactionType.INCOME, DataScope.FAMILY, "c1", "Gelir", now),
            Transaction("2", "Market", 1500.0, TransactionType.EXPENSE, DataScope.FAMILY, "cat_market", "Market", now),
            Transaction("3", "Eski Gider", 500.0, TransactionType.EXPENSE, DataScope.PERSONAL, "cat_giyim", "Giyim", now - 5 * 86_400_000L)
        )
        val budgets = listOf(
            Budget("b_total", null, "Toplam", 10000.0, BudgetPeriod.MONTHLY)
        )

        val metrics = calculator.calculate(txs, budgets, TimePeriod.MONTHLY, now)

        assertEquals(28000.0, metrics.totalAssets, 0.01)
        assertEquals(40000.0, metrics.totalBudgetPool, 0.01)
        assertEquals(1500.0, metrics.todayExpense, 0.01)
        assertEquals(38000.0, metrics.remainingMonthlyLimit, 0.01)
    }

    @Test
    fun `odeme gunu gecmis abonelik aylik harcamaya ilerideki abonelik gelecek odemelere yazilmali`() {
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 15) }
        val now = cal.timeInMillis
        val subs = listOf(
            Subscription("s1", "Netflix", 200.0, BillingCycle.MONTHLY, billingDay = 10, categoryName = "Eğlence"),
            Subscription("s2", "İnternet", 450.0, BillingCycle.MONTHLY, billingDay = 25, categoryName = "Fatura")
        )

        val metrics = calculator.calculate(emptyList(), emptyList(), TimePeriod.MONTHLY, now, subs)

        assertEquals(200.0, metrics.monthlyTotalExpense, 0.01)
        assertEquals(450.0, metrics.upcomingPaymentsTotal, 0.01)
        assertEquals(1, metrics.upcomingPaymentsCount)
    }

    @Test
    fun `gunluk filtre yalnizca son 24 saati dondurmeli`() {
        val now = 1_700_000_000_000L
        val txs = listOf(
            Transaction("1", "Bugün", 100.0, TransactionType.EXPENSE, DataScope.FAMILY, "cat_market", "Market", now - 1000L),
            Transaction("2", "3 Gün Önce", 200.0, TransactionType.EXPENSE, DataScope.FAMILY, "cat_market", "Market", now - 3 * 86_400_000L)
        )

        val filtered = calculator.filterByPeriod(txs, TimePeriod.DAILY, now)

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.first().id)
    }
}
