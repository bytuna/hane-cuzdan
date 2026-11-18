package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.SyncStatus
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val scope: String,
    val categoryId: String,
    val categoryName: String,
    val timestamp: Long,
    val note: String,
    val receiptPath: String?,
    val createdByUserId: String,
    val familyId: String?,
    val syncStatus: String
) {
    fun toDomain() = Transaction(
        id = id,
        title = title,
        amount = amount,
        type = TransactionType.valueOf(type),
        scope = DataScope.valueOf(scope),
        categoryId = categoryId,
        categoryName = categoryName,
        timestamp = timestamp,
        note = note,
        receiptPath = receiptPath,
        createdByUserId = createdByUserId,
        familyId = familyId,
        syncStatus = SyncStatus.valueOf(syncStatus)
    )

    companion object {
        fun fromDomain(model: Transaction) = TransactionEntity(
            id = model.id,
            title = model.title,
            amount = model.amount,
            type = model.type.name,
            scope = model.scope.name,
            categoryId = model.categoryId,
            categoryName = model.categoryName,
            timestamp = model.timestamp,
            note = model.note,
            receiptPath = model.receiptPath,
            createdByUserId = model.createdByUserId,
            familyId = model.familyId,
            syncStatus = model.syncStatus.name
        )
    }
}
