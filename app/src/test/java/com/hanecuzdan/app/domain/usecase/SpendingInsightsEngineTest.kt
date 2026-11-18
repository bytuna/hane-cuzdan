package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.BillingCycle
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpendingInsightsEngineTest {

    private val engine = SpendingInsightsEngine()

    @Test
    fun `gecen aya gore artan kategori icin tasarruf tavsiyesi uretilmeli`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val now = cal.timeInMillis

        val lastMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val lastMonthTs = lastMonthCal.timeInMillis

        val txs = listOf(
            // Geçen ay Market: 2000 TL
            Transaction("1", "Market", 2000.0, TransactionType.EXPENSE, DataScope.FAMILY, "cat_market", "Market", lastMonthTs),
            // Bu ay Market: 3200 TL (+%60 artış)
            Transaction("2", "Market", 3200.0, TransactionType.EXPENSE, DataScope.FAMILY, "cat_market", "Market", now - 86_400_000L)
        )

        val report = engine.analyze(
            transactions = txs,
            monthlyBudgetPool = 5000.0,
            subscriptions = emptyList(),
            nowMillis = now
        )

        assertTrue(report.categoryComparisons.any { it.categoryName == "Market" && it.changePercent >= 50 })
        assertTrue(report.recommendations.any { it.title.contains("Market") && it.potentialMonthlySaving > 0.0 })
    }

    @Test
    fun `butce hizini asan harcamada OVER_PACE ve gunluk limit hesaplanmali`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val now = cal.timeInMillis

        // 30 günlük ayın 10. gününde (%33) bütçenin %80'i harcanmış
        val txs = listOf(
            Transaction("1", "Büyük Harcama", 8000.0, TransactionType.EXPENSE, DataScope.FAMILY, "cat_kira", "Kira", now)
        )
        val subs = listOf(
            Subscription("s1", "Netflix", 300.0, BillingCycle.MONTHLY, billingDay = 20, categoryName = "Eğlence")
        )

        val report = engine.analyze(
            transactions = txs,
            monthlyBudgetPool = 10000.0,
            subscriptions = subs,
            nowMillis = now
        )

        assertEquals(SpendingPaceStatus.OVER_PACE, report.paceStatus)
        assertTrue(report.dailySafeSpend in 90.0..110.0)
        assertTrue(report.recommendations.isNotEmpty())
    }
}
