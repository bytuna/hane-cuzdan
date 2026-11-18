package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetPeriod
import com.hanecuzdan.app.domain.model.BudgetThresholdLevel
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetEvaluatorTest {

    private val evaluator = BudgetEvaluator()

    @Test
    fun `harcama yuzde 80 altindaysa NONE donmeli`() {
        val budget = Budget("b1", "cat_market", "Market", 1000.0, BudgetPeriod.MONTHLY)
        val txs = listOf(expense("t1", "cat_market", 700.0))

        val result = evaluator.evaluate(listOf(budget), txs, nowMillis = 1_000_000L)

        assertEquals(1, result.size)
        assertEquals(BudgetThresholdLevel.NONE, result.first().thresholdLevel)
        assertEquals(700.0, result.first().spentAmount, 0.01)
    }

    @Test
    fun `harcama yuzde 80 ve uzerindeyse WARNING_80 donmeli`() {
        val budget = Budget("b1", "cat_market", "Market", 1000.0, BudgetPeriod.MONTHLY)
        val txs = listOf(expense("t1", "cat_market", 850.0))

        val result = evaluator.evaluate(listOf(budget), txs, nowMillis = 1_000_000L)

        assertEquals(BudgetThresholdLevel.WARNING_80, result.first().thresholdLevel)
    }

    @Test
    fun `harcama yuzde 100 ve uzerindeyse EXCEEDED_100 donmeli`() {
        val budget = Budget("b1", null, "Toplam Bütçe", 2000.0, BudgetPeriod.MONTHLY)
        val txs = listOf(
            expense("t1", "cat_market", 1200.0),
            expense("t2", "cat_fatura", 900.0)
        )

        val result = evaluator.evaluate(listOf(budget), txs, nowMillis = 1_000_000L)

        assertEquals(BudgetThresholdLevel.EXCEEDED_100, result.first().thresholdLevel)
        assertEquals(2100.0, result.first().spentAmount, 0.01)
    }

    private fun expense(id: String, categoryId: String, amount: Double) = Transaction(
        id = id,
        title = "Test",
        amount = amount,
        type = TransactionType.EXPENSE,
        scope = DataScope.FAMILY,
        categoryId = categoryId,
        categoryName = "Test",
        timestamp = 900_000L
    )
}
