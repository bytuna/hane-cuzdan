package com.hanecuzdan.app.domain.model

/** Üst bardaki canlı senkronizasyon durumu. */
enum class SyncConnectionState {
    SYNCED,
    SYNCING,
    OFFLINE_PENDING,
    ERROR
}

/** Senkronizasyon özet bilgisi. */
data class SyncStatusSummary(
    val state: SyncConnectionState = SyncConnectionState.SYNCED,
    val pendingCount: Int = 0,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null
)
