package com.hanecuzdan.app.domain.model

import kotlinx.serialization.Serializable

/** İşlem türü: Gelir veya Gider. */
@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE
}

/** Veri kapsamı: Aile havuzu (bulut senkronizasyonlu) veya Kişisel (yalnızca yerel). */
@Serializable
enum class DataScope {
    FAMILY,
    PERSONAL
}

/** Çevrimdışı senkronizasyon durumu. */
@Serializable
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED,
    LOCAL_ONLY
}

/** Dashboard analitik zaman dilimi filtresi. */
enum class TimePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/** Bütçe hareketi domain modeli. */
@Serializable
data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val scope: DataScope,
    val categoryId: String,
    val categoryName: String,
    val timestamp: Long,
    val note: String = "",
    val receiptPath: String? = null,
    val createdByUserId: String = "local_user",
    val familyId: String? = null,
    val syncStatus: SyncStatus = if (scope == DataScope.PERSONAL) SyncStatus.LOCAL_ONLY else SyncStatus.PENDING
)
