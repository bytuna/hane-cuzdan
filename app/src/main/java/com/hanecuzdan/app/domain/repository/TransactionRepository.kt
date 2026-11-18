package com.hanecuzdan.app.domain.repository

import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Gelir/Gider işlemleri için repository sözleşmesi. */
interface TransactionRepository {
    fun observeAllTransactions(): Flow<List<Transaction>>
    fun observeByScope(scope: DataScope): Flow<List<Transaction>>
    fun observeWithReceipts(): Flow<List<Transaction>>
    suspend fun getAllSnapshot(): List<Transaction>
    suspend fun upsertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)
}
