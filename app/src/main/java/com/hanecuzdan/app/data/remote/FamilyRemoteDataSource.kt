package com.hanecuzdan.app.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.hanecuzdan.app.data.local.entity.SyncQueueEntity
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.Loan
import com.hanecuzdan.app.domain.model.SavingsGoal
import com.hanecuzdan.app.domain.model.Subscription
import com.hanecuzdan.app.domain.model.SyncStatus
import com.hanecuzdan.app.domain.model.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FamilyMember(
    val memberId: String,
    val userName: String,
    val lastSeenAt: Long = 0L
)

/**
 * Aile havuzu bulut senkronizasyonu için uzak veri kaynağı sözleşmesi.
 */
interface FamilyRemoteDataSource {
    suspend fun pushSyncItem(familyId: String, item: SyncQueueEntity): Boolean
    fun observeFamilyTransactions(familyId: String): Flow<List<Transaction>>
    fun observeFamilyBudgets(familyId: String): Flow<List<Budget>>
    fun observeFamilySubscriptions(familyId: String): Flow<List<Subscription>>
    fun observeFamilySavingsGoals(familyId: String): Flow<List<SavingsGoal>>
    fun observeFamilyLoans(familyId: String): Flow<List<Loan>>
    fun observeFamilyCategories(familyId: String): Flow<List<Category>>
    suspend fun registerFamilyMember(familyId: String, memberId: String, userName: String)
    fun observeFamilyMembers(familyId: String): Flow<List<FamilyMember>>
}

/**
 * Gerçek Firebase Firestore uzak veri kaynağı.
 * Koleksiyon yapısı: families/{familyId}/{transactions|categories|budgets}/{entityId}
 */
class FirestoreFamilyDataSource : FamilyRemoteDataSource {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun pushSyncItem(familyId: String, item: SyncQueueEntity): Boolean {
        val collection = when (item.entityType) {
            "TRANSACTION" -> "transactions"
            "CATEGORY" -> "categories"
            "BUDGET" -> "budgets"
            "SUBSCRIPTION" -> "subscriptions"
            "SAVINGS_GOAL" -> "savings_goals"
            "LOAN" -> "loans"
            else -> "misc"
        }

        val docRef = db.collection("families")
            .document(familyId)
            .collection(collection)
            .document(item.entityId)

        return runCatching {
            if (item.action == "DELETE") {
                docRef.delete().awaitTask()
            } else {
                val payload = mapOf(
                    "entityId" to item.entityId,
                    "entityType" to item.entityType,
                    "payloadJson" to item.payloadJson,
                    "updatedAt" to item.createdAt
                )
                docRef.set(payload).awaitTask()
            }
            true
        }.onFailure { ex ->
            Log.e("FirestoreSync", "pushSyncItem failed for ${item.entityId}", ex)
        }.getOrDefault(false)
    }

    override fun observeFamilyTransactions(familyId: String): Flow<List<Transaction>> = callbackFlow {
        val registration = db.collection("families")
            .document(familyId)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "observeFamilyTransactions error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val items = snapshot.documents.mapNotNull { doc ->
                    val rawJson = doc.getString("payloadJson") ?: return@mapNotNull null
                    runCatching {
                        json.decodeFromString<Transaction>(rawJson)
                            .copy(syncStatus = SyncStatus.SYNCED)
                    }.getOrNull()
                }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override fun observeFamilyBudgets(familyId: String): Flow<List<Budget>> = callbackFlow {
        val registration = db.collection("families")
            .document(familyId)
            .collection("budgets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "observeFamilyBudgets error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val items = snapshot.documents.mapNotNull { doc ->
                    val rawJson = doc.getString("payloadJson") ?: return@mapNotNull null
                    runCatching {
                        json.decodeFromString<Budget>(rawJson)
                    }.getOrNull()
                }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override fun observeFamilySubscriptions(familyId: String): Flow<List<Subscription>> =
        observeCollection(familyId, "subscriptions")

    override fun observeFamilySavingsGoals(familyId: String): Flow<List<SavingsGoal>> =
        observeCollection(familyId, "savings_goals")

    override fun observeFamilyLoans(familyId: String): Flow<List<Loan>> =
        observeCollection(familyId, "loans")

    override fun observeFamilyCategories(familyId: String): Flow<List<Category>> =
        observeCollection(familyId, "categories")

    private inline fun <reified T> observeCollection(
        familyId: String,
        collectionName: String
    ): Flow<List<T>> = callbackFlow {
        val registration = db.collection("families")
            .document(familyId)
            .collection(collectionName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "observe $collectionName error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val items = snapshot.documents.mapNotNull { doc ->
                    val rawJson = doc.getString("payloadJson") ?: return@mapNotNull null
                    runCatching { json.decodeFromString<T>(rawJson) }.getOrNull()
                }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun registerFamilyMember(familyId: String, memberId: String, userName: String) {
        val docRef = db.collection("families")
            .document(familyId)
            .collection("members")
            .document(memberId)

        runCatching {
            val payload = mapOf(
                "memberId" to memberId,
                "userName" to userName,
                "lastSeenAt" to System.currentTimeMillis()
            )
            docRef.set(payload).awaitTask()
        }.onFailure { ex ->
            Log.e("FirestoreSync", "registerFamilyMember failed", ex)
        }
    }

    override fun observeFamilyMembers(familyId: String): Flow<List<FamilyMember>> = callbackFlow {
        val registration = db.collection("families")
            .document(familyId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "observeFamilyMembers error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                val members = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("memberId") ?: doc.id
                    val name = doc.getString("userName") ?: return@mapNotNull null
                    val lastSeen = doc.getLong("lastSeenAt") ?: 0L
                    FamilyMember(memberId = id, userName = name, lastSeenAt = lastSeen)
                }.sortedBy { it.userName.lowercase() }
                trySend(members)
            }
        awaitClose { registration.remove() }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { ex -> cont.resumeWithException(ex) }
    }
}
