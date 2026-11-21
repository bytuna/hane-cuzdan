package com.hanecuzdan.app.data.repository

import com.hanecuzdan.app.data.local.dao.BudgetDao
import com.hanecuzdan.app.data.local.dao.SyncQueueDao
import com.hanecuzdan.app.data.local.entity.BudgetEntity
import com.hanecuzdan.app.data.local.entity.SyncQueueEntity
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val syncQueueDao: SyncQueueDao
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSnapshot(): List<Budget> =
        budgetDao.getAllSnapshot().map { it.toDomain() }

    override suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsert(BudgetEntity.fromDomain(budget))
        if (budget.scope == DataScope.FAMILY) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityId = budget.id,
                    entityType = "BUDGET",
                    action = "UPSERT",
                    payloadJson = Json.encodeToString(budget)
                )
            )
        }
    }

    override suspend fun deleteBudget(budgetId: String) {
        budgetDao.deleteById(budgetId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityId = budgetId,
                entityType = "BUDGET",
                action = "DELETE",
                payloadJson = "{}"
            )
        )
    }
}
