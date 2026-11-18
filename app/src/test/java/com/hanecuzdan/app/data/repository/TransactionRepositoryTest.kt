package com.hanecuzdan.app.data.repository

import com.hanecuzdan.app.data.local.dao.SyncQueueDao
import com.hanecuzdan.app.data.local.dao.TransactionDao
import com.hanecuzdan.app.data.local.entity.SyncQueueEntity
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransactionRepositoryTest {

    private val transactionDao: TransactionDao = mockk(relaxed = true)
    private val syncQueueDao: SyncQueueDao = mockk(relaxed = true)
    private val repository = TransactionRepositoryImpl(transactionDao, syncQueueDao)

    @Test
    fun `kisisel islem sync kuyruguna eklenmemeli`() = runTest {
        val personalTx = Transaction(
            id = "p1",
            title = "Kahve",
            amount = 90.0,
            type = TransactionType.EXPENSE,
            scope = DataScope.PERSONAL,
            categoryId = "cat_eglence",
            categoryName = "Eğlence",
            timestamp = 1000L
        )

        repository.upsertTransaction(personalTx)

        coVerify(exactly = 1) { transactionDao.upsert(any()) }
        coVerify(exactly = 0) { syncQueueDao.enqueue(any()) }
    }

    @Test
    fun `aile islemi sync kuyruguna eklenmeli`() = runTest {
        val familyTx = Transaction(
            id = "f1",
            title = "Kira",
            amount = 15000.0,
            type = TransactionType.EXPENSE,
            scope = DataScope.FAMILY,
            categoryId = "cat_kira",
            categoryName = "Kira",
            timestamp = 1000L
        )

        repository.upsertTransaction(familyTx)

        coVerify(exactly = 1) { transactionDao.upsert(any()) }
        coVerify(exactly = 1) { syncQueueDao.enqueue(match<SyncQueueEntity> { it.entityId == "f1" }) }
    }
}
