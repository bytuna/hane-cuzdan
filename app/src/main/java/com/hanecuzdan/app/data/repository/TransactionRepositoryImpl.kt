package com.hanecuzdan.app.data.repository

import com.hanecuzdan.app.data.local.dao.SyncQueueDao
import com.hanecuzdan.app.data.local.dao.TransactionDao
import com.hanecuzdan.app.data.local.entity.SyncQueueEntity
import com.hanecuzdan.app.data.local.entity.TransactionEntity
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.SyncStatus
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val syncQueueDao: SyncQueueDao
) : TransactionRepository {

    override fun observeAllTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByScope(scope: DataScope): Flow<List<Transaction>> =
        transactionDao.observeByScope(scope.name).map { list -> list.map { it.toDomain() } }

    override fun observeWithReceipts(): Flow<List<Transaction>> =
        transactionDao.observeWithReceipts().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSnapshot(): List<Transaction> =
        transactionDao.getAllSnapshot().map { it.toDomain() }

    override suspend fun upsertTransaction(transaction: Transaction) {
        val adjusted = if (transaction.scope == DataScope.PERSONAL) {
            transaction.copy(syncStatus = SyncStatus.LOCAL_ONLY, familyId = null)
        } else {
            transaction.copy(syncStatus = SyncStatus.PENDING)
        }

        transactionDao.upsert(TransactionEntity.fromDomain(adjusted))

        // Yalnızca Aile Havuzu işlemleri bulut senkronizasyon kuyruğuna eklenir.
        if (adjusted.scope == DataScope.FAMILY) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityId = adjusted.id,
                    entityType = "TRANSACTION",
                    action = "UPSERT",
                    payloadJson = Json.encodeToString(adjusted)
                )
            )
        }
    }

    override suspend fun deleteTransaction(transactionId: String) {
        transactionDao.deleteById(transactionId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityId = transactionId,
                entityType = "TRANSACTION",
                action = "DELETE",
                payloadJson = "{}"
            )
        )
    }
}
