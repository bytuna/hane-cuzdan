package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetPeriod
import com.hanecuzdan.app.domain.model.BudgetStatus
import com.hanecuzdan.app.domain.model.BudgetThresholdLevel
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import java.util.Calendar

/** Bütçe limitlerini harcamalara göre değerlendirip %80 ve %100 uyarı seviyelerini hesaplar. */
class BudgetEvaluator {

    fun evaluate(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<BudgetStatus> {
        return budgets.map { budget ->
            val startMillis = when (budget.period) {
                BudgetPeriod.WEEKLY -> nowMillis - 7L * 86_400_000L
                BudgetPeriod.MONTHLY -> Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val spent = transactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE && it.timestamp >= startMillis }
                .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                .sumOf { it.amount }

            val ratio = if (budget.limitAmount > 0.0) spent / budget.limitAmount else 0.0
            val level = when {
                ratio >= 1.0 -> BudgetThresholdLevel.EXCEEDED_100
                ratio >= 0.8 -> BudgetThresholdLevel.WARNING_80
                else -> BudgetThresholdLevel.NONE
            }

            BudgetStatus(
                budget = budget,
                spentAmount = spent,
                usageRatio = ratio,
                thresholdLevel = level
            )
        }
    }
}
