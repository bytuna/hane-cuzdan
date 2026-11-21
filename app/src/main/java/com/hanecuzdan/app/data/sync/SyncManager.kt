package com.hanecuzdan.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.hanecuzdan.app.data.local.dao.BudgetDao
import com.hanecuzdan.app.data.local.dao.SyncQueueDao
import com.hanecuzdan.app.data.local.dao.TransactionDao
import com.hanecuzdan.app.data.local.entity.BudgetEntity
import com.hanecuzdan.app.data.local.entity.TransactionEntity
import com.hanecuzdan.app.data.remote.FamilyMember
import com.hanecuzdan.app.data.remote.FamilyRemoteDataSource
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.SyncConnectionState
import com.hanecuzdan.app.domain.model.SyncStatus
import com.hanecuzdan.app.domain.model.SyncStatusSummary
import com.hanecuzdan.app.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Çevrimdışı kuyruğu izler ve ağ geldiğinde Aile Havuzu verilerini buluta eşitler. */
class SyncManager(
    context: Context,
    private val syncQueueDao: SyncQueueDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val remoteDataSource: FamilyRemoteDataSource,
    private val familyIdProvider: () -> String?,
    private val memberIdProvider: () -> String,
    private val userNameProvider: () -> String
) : SyncRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var remoteObserverJob: Job? = null
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _syncStatus = MutableStateFlow(SyncStatusSummary())
    override val syncStatus: StateFlow<SyncStatusSummary> = _syncStatus.asStateFlow()

    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    override val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    init {
        observeQueueAndNetwork()
        observeRemoteFamilyChanges()
    }

    override fun refreshFamilyListener() {
        observeRemoteFamilyChanges()
    }

    private fun observeRemoteFamilyChanges() {
        remoteObserverJob?.cancel()
        val familyId = familyIdProvider()?.ifBlank { "default_family" } ?: "default_family"
        remoteObserverJob = scope.launch {
            launch {
                runCatching {
                    remoteDataSource.registerFamilyMember(
                        familyId = familyId,
                        memberId = memberIdProvider(),
                        userName = userNameProvider()
                    )
                }
            }
            launch {
                runCatching {
                    remoteDataSource.observeFamilyMembers(familyId).collect { members ->
                        _familyMembers.value = members
                    }
                }
            }
            launch {
                runCatching {
                    remoteDataSource.observeFamilyTransactions(familyId).collect { remoteTxs ->
                        val pendingIds = syncQueueDao.getAllPending().map { it.entityId }.toSet()
                        val remoteIds = remoteTxs.map { it.id }.toSet()

                        remoteTxs
                            .filter { it.id !in pendingIds }
                            .forEach { tx ->
                                transactionDao.upsert(TransactionEntity.fromDomain(tx))
                            }

                        transactionDao.getAllSnapshot()
                            .filter {
                                it.scope == DataScope.FAMILY.name &&
                                    it.syncStatus == SyncStatus.SYNCED.name &&
                                    it.id !in pendingIds &&
                                    it.id !in remoteIds
                            }
                            .forEach { localDeleted ->
                                transactionDao.deleteById(localDeleted.id)
                            }
                    }
                }
            }
            launch {
                runCatching {
                    remoteDataSource.observeFamilyBudgets(familyId).collect { remoteBudgets ->
                        val pendingIds = syncQueueDao.getAllPending().map { it.entityId }.toSet()
                        val remoteIds = remoteBudgets.map { it.id }.toSet()

                        remoteBudgets
                            .filter { it.id !in pendingIds }
                            .forEach { budget ->
                                budgetDao.upsert(BudgetEntity.fromDomain(budget))
                            }

                        budgetDao.getAllSnapshot()
                            .filter { it.scope == DataScope.FAMILY.name && it.id !in pendingIds && it.id !in remoteIds }
                            .forEach { localDeleted ->
                                budgetDao.deleteById(localDeleted.id)
                            }
                    }
                }
            }
        }
    }

    private fun observeQueueAndNetwork() {
        scope.launch {
            syncQueueDao.observePendingCount().collect { count ->
                val online = isOnline()
                val currentState = when {
                    count > 0 && !online -> SyncConnectionState.OFFLINE_PENDING
                    count > 0 && online -> {
                        triggerSync()
                        _syncStatus.value.state
                    }
                    else -> SyncConnectionState.SYNCED
                }
                _syncStatus.value = _syncStatus.value.copy(
                    state = currentState,
                    pendingCount = count
                )
            }
        }

        runCatching {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch { triggerSync() }
                }

                override fun onLost(network: Network) {
                    val count = _syncStatus.value.pendingCount
                    if (count > 0) {
                        _syncStatus.value = _syncStatus.value.copy(
                            state = SyncConnectionState.OFFLINE_PENDING
                        )
                    }
                }
            })
        }
    }

    override suspend fun triggerSync(): Boolean {
        val pending = syncQueueDao.getAllPending()
        if (pending.isEmpty()) {
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncConnectionState.SYNCED,
                pendingCount = 0
            )
            return true
        }

        if (!isOnline()) {
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncConnectionState.OFFLINE_PENDING,
                pendingCount = pending.size
            )
            return false
        }

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncConnectionState.SYNCING,
            pendingCount = pending.size
        )

        val familyId = familyIdProvider() ?: "default_family"
        var allOk = true

        for (item in pending) {
            val pushed = runCatching { remoteDataSource.pushSyncItem(familyId, item) }.getOrDefault(false)
            if (pushed) {
                syncQueueDao.remove(item.entityId)
                if (item.entityType == "TRANSACTION" && item.action == "UPSERT") {
                    transactionDao.updateSyncStatus(item.entityId, SyncStatus.SYNCED.name)
                }
            } else {
                allOk = false
            }
        }

        val remaining = syncQueueDao.getAllPending().size
        _syncStatus.value = SyncStatusSummary(
            state = if (allOk && remaining == 0) SyncConnectionState.SYNCED else SyncConnectionState.ERROR,
            pendingCount = remaining,
            lastSyncedAt = if (allOk) System.currentTimeMillis() else _syncStatus.value.lastSyncedAt,
            errorMessage = if (!allOk) "Bazı aile kayıtları eşitlenemedi" else null
        )
        return allOk
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
