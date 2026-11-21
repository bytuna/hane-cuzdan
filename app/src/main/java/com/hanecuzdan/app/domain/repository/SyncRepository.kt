package com.hanecuzdan.app.domain.repository

import com.hanecuzdan.app.data.remote.FamilyMember
import com.hanecuzdan.app.domain.model.SyncStatusSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Çevrimdışı kuyruk ve bulut senkronizasyonu sözleşmesi. */
interface SyncRepository {
    val syncStatus: StateFlow<SyncStatusSummary>
    val familyMembers: StateFlow<List<FamilyMember>>
        get() = MutableStateFlow(emptyList())
    suspend fun triggerSync(): Boolean
    fun refreshFamilyListener() {}
}
