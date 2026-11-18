package com.hanecuzdan.app.data.repository

import com.hanecuzdan.app.data.local.dao.BudgetDao
import com.hanecuzdan.app.data.local.entity.BudgetEntity
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSnapshot(): List<Budget> =
        budgetDao.getAllSnapshot().map { it.toDomain() }

    override suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsert(BudgetEntity.fromDomain(budget))
    }

    override suspend fun deleteBudget(budgetId: String) {
        budgetDao.deleteById(budgetId)
    }
}
