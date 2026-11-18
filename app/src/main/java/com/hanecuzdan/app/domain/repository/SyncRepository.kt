package com.hanecuzdan.app.domain.repository

import com.hanecuzdan.app.domain.model.SyncStatusSummary
import kotlinx.coroutines.flow.StateFlow

/** Çevrimdışı kuyruk ve bulut senkronizasyonu sözleşmesi. */
interface SyncRepository {
    val syncStatus: StateFlow<SyncStatusSummary>
    suspend fun triggerSync(): Boolean
}
