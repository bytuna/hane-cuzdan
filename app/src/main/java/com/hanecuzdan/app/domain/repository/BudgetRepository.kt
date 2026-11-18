package com.hanecuzdan.app.domain.repository

import com.hanecuzdan.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

/** Bütçe limitleri için repository sözleşmesi. */
interface BudgetRepository {
    fun observeBudgets(): Flow<List<Budget>>
    suspend fun getAllSnapshot(): List<Budget>
    suspend fun upsertBudget(budget: Budget)
    suspend fun deleteBudget(budgetId: String)
}
