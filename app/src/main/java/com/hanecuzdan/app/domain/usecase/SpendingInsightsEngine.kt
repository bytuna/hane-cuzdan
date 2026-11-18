package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import java.util.Calendar
import kotlin.math.roundToInt

enum class SpendingPaceStatus {
    UNDER_PACE,
    ON_PACE,
    OVER_PACE
}

enum class RecommendationSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class CategoryComparison(
    val categoryName: String,
    val thisMonth: Double,
    val lastMonth: Double,
    val changePercent: Int
)

data class SpendingRecommendation(
    val title: String,
    val description: String,
    val potentialMonthlySaving: Double,
    val severity: RecommendationSeverity
)

data class SpendingInsightsReport(
    val thisMonthExpense: Double = 0.0,
    val lastMonthExpense: Double = 0.0,
    val monthlyBudgetPool: Double = 0.0,
    val todayDay: Int = 1,
    val daysInMonth: Int = 30,
    val idealPaceRatio: Float = 0.5f,
    val actualSpendRatio: Float = 0f,
    val paceStatus: SpendingPaceStatus = SpendingPaceStatus.ON_PACE,
    val dailySafeSpend: Double = 0.0,
    val projectedNextMonthExpense: Double = 0.0,
    val dailyCumulativeSpend: List<Double> = emptyList(),
    val categoryComparisons: List<CategoryComparison> = emptyList(),
    val recommendations: List<SpendingRecommendation> = emptyList()
)

/** Geçmiş işlemler, bütçe ve aboneliklere göre gelecek ay tasarruf tavsiyeleri ve grafik verileri üretir. */
class SpendingInsightsEngine {

    fun analyze(
        transactions: List<Transaction>,
        monthlyBudgetPool: Double,
        subscriptions: List<Subscription>,
        nowMillis: Long = System.currentTimeMillis()
    ): SpendingInsightsReport {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val thisMonthStart = startOfMonthMillis(cal, monthDelta = 0)
        val lastMonthStart = startOfMonthMillis(cal, monthDelta = -1)

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val thisMonthTxs = expenses.filter { it.timestamp >= thisMonthStart }
        val lastMonthTxs = expenses.filter { it.timestamp in lastMonthStart until thisMonthStart }

        val activeSubs = subscriptions.filter { it.isActive }
        val dueSubsExpense = activeSubs.filter { it.billingDay <= todayDay }.sumOf { it.monthlyEquivalent() }
        val totalSubsMonthly = activeSubs.sumOf { it.monthlyEquivalent() }

        val thisMonthExpense = thisMonthTxs.sumOf { it.amount } + dueSubsExpense
        val lastMonthExpense = lastMonthTxs.sumOf { it.amount }

        val idealPaceRatio = (todayDay.toFloat() / daysInMonth.coerceAtLeast(1)).coerceIn(0f, 1f)
        val actualSpendRatio = if (monthlyBudgetPool > 0.0) {
            (thisMonthExpense / monthlyBudgetPool).toFloat().coerceAtLeast(0f)
        } else 0f

        val paceStatus = when {
            monthlyBudgetPool <= 0.0 -> SpendingPaceStatus.ON_PACE
            actualSpendRatio > idealPaceRatio + 0.08f -> SpendingPaceStatus.OVER_PACE
            actualSpendRatio < idealPaceRatio - 0.08f -> SpendingPaceStatus.UNDER_PACE
            else -> SpendingPaceStatus.ON_PACE
        }

        val remainingDays = (daysInMonth - todayDay + 1).coerceAtLeast(1)
        val dailySafeSpend = ((monthlyBudgetPool - thisMonthExpense).coerceAtLeast(0.0)) / remainingDays

        // Ayın 1'inden bugüne kümülatif günlük harcama eğrisi
        val dailySums = DoubleArray(todayDay + 1)
        val txCal = Calendar.getInstance()
        thisMonthTxs.forEach { tx ->
            txCal.timeInMillis = tx.timestamp
            val day = txCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, todayDay)
            dailySums[day] += tx.amount
        }
        val cumulative = mutableListOf<Double>()
        var running = 0.0
        for (d in 1..todayDay) {
            running += dailySums[d]
            cumulative.add(running)
        }

        // Kategori bazlı Bu Ay vs Geçen Ay karşılaştırması
        val thisByCat = thisMonthTxs.groupBy { it.categoryName }.mapValues { (_, l) -> l.sumOf { it.amount } }
        val lastByCat = lastMonthTxs.groupBy { it.categoryName }.mapValues { (_, l) -> l.sumOf { it.amount } }
        val allCats = (thisByCat.keys + lastByCat.keys).distinct()

        val comparisons = allCats.map { cat ->
            val cur = thisByCat[cat] ?: 0.0
            val prev = lastByCat[cat] ?: 0.0
            val pct = when {
                prev > 0.0 -> (((cur - prev) / prev) * 100.0).roundToInt()
                cur > 0.0 -> 100
                else -> 0
            }
            CategoryComparison(cat, cur, prev, pct)
        }.sortedByDescending { maxOf(it.thisMonth, it.lastMonth) }

        // Gelecek ay tahmini gider (mevcut hız + tüm abonelikler)
        val dailyAvg = if (todayDay > 0) thisMonthTxs.sumOf { it.amount } / todayDay else 0.0
        val projectedNextMonth = (dailyAvg * daysInMonth) + totalSubsMonthly

        val recommendations = buildRecommendations(
            paceStatus = paceStatus,
            dailySafeSpend = dailySafeSpend,
            comparisons = comparisons,
            activeSubs = activeSubs,
            totalSubsMonthly = totalSubsMonthly,
            thisMonthTxs = thisMonthTxs
        )

        return SpendingInsightsReport(
            thisMonthExpense = thisMonthExpense,
            lastMonthExpense = lastMonthExpense,
            monthlyBudgetPool = monthlyBudgetPool,
            todayDay = todayDay,
            daysInMonth = daysInMonth,
            idealPaceRatio = idealPaceRatio,
            actualSpendRatio = actualSpendRatio,
            paceStatus = paceStatus,
            dailySafeSpend = dailySafeSpend,
            projectedNextMonthExpense = projectedNextMonth,
            dailyCumulativeSpend = cumulative,
            categoryComparisons = comparisons,
            recommendations = recommendations
        )
    }

    private fun buildRecommendations(
        paceStatus: SpendingPaceStatus,
        dailySafeSpend: Double,
        comparisons: List<CategoryComparison>,
        activeSubs: List<Subscription>,
        totalSubsMonthly: Double,
        thisMonthTxs: List<Transaction>
    ): List<SpendingRecommendation> {
        val list = mutableListOf<SpendingRecommendation>()

        if (paceStatus == SpendingPaceStatus.OVER_PACE) {
            list.add(
                SpendingRecommendation(
                    title = "Harcama Hızınız Bütçe Temposunun Üzerinde",
                    description = "Ay sonuna kadar bütçe açığı vermemek için günlük harcamanızı ortalama ${dailySafeSpend.toInt()} ₺ altında tutmanız önerilir.",
                    potentialMonthlySaving = (dailySafeSpend * 0.15 * 10).coerceAtLeast(100.0),
                    severity = RecommendationSeverity.CRITICAL
                )
            )
        }

        comparisons
            .filter { it.lastMonth > 0.0 && it.changePercent >= 20 }
            .take(2)
            .forEach { comp ->
                val diff = comp.thisMonth - comp.lastMonth
                val targetCut = (diff * 0.6).coerceAtLeast(50.0)
                list.add(
                    SpendingRecommendation(
                        title = "${comp.categoryName} Harcamalarında %${comp.changePercent} Artış",
                        description = "Geçen aya göre +${diff.toInt()} ₺ fazla harcandı. Gelecek ay bu kategoriye tavan limit koyarak yaklaşık ${targetCut.toInt()} ₺ tasarruf sağlayabilirsiniz.",
                        potentialMonthlySaving = targetCut,
                        severity = RecommendationSeverity.WARNING
                    )
                )
            }

        if (activeSubs.isNotEmpty()) {
            val highestSub = activeSubs.maxByOrNull { it.monthlyEquivalent() }
            val annualTotal = totalSubsMonthly * 12.0
            list.add(
                SpendingRecommendation(
                    title = "Abonelik Optimizasyonu (Yıllık ${annualTotal.toInt()} ₺)",
                    description = "Sabit abonelikleriniz aylık ${totalSubsMonthly.toInt()} ₺ tutuyor. En az kullandığınız (${highestSub?.name ?: "abonelik"}) planını düşürmek gelecek ay bütçenize alan açar.",
                    potentialMonthlySaving = (highestSub?.monthlyEquivalent() ?: totalSubsMonthly * 0.25),
                    severity = RecommendationSeverity.INFO
                )
            )
        }

        val repeatedSmall = thisMonthTxs
            .filter { it.amount < 300.0 }
            .groupBy { it.title.trim().lowercase() }
            .filter { it.value.size >= 3 }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }

        if (repeatedSmall != null) {
            val sum = repeatedSmall.value.sumOf { it.amount }
            list.add(
                SpendingRecommendation(
                    title = "Sık Tekrarlanan Küçük Gider: ${repeatedSmall.value.first().title}",
                    description = "Bu ay ${repeatedSmall.value.size} kez toplam ${sum.toInt()} ₺ harcandı. Sıklığı yarıya indirmek ayda ${(sum / 2).toInt()} ₺ kazandırır.",
                    potentialMonthlySaving = sum / 2.0,
                    severity = RecommendationSeverity.INFO
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                SpendingRecommendation(
                    title = "Harcama Temponuz Dengeli Görünüyor",
                    description = "Bütçe hedeflerinize uyumlu ilerliyorsunuz. Gelecek ay gelirinizin %10'unu otomatik olarak Hedefler (Kumbara) modülüne aktarabilirsiniz.",
                    potentialMonthlySaving = 0.0,
                    severity = RecommendationSeverity.INFO
                )
            )
        }

        return list
    }

    private fun startOfMonthMillis(baseCal: Calendar, monthDelta: Int): Long {
        return (baseCal.clone() as Calendar).apply {
            add(Calendar.MONTH, monthDelta)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
