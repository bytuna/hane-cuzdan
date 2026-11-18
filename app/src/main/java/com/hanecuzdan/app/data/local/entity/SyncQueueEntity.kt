package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** İnternet yokken yapılan Aile Havuzu işlemlerini tutan çevrimdışı kuyruk tablosu. */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val entityId: String,
    val entityType: String, // TRANSACTION, CATEGORY, BUDGET
    val action: String,     // UPSERT, DELETE
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
