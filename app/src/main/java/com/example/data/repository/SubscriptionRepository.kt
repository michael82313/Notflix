package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PlanFeature
import com.example.data.model.SubscriptionCatalogDefaults
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserSubscription
import com.example.data.remote.FirestoreDocumentMapper
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Isolated subscription data layer prepared for future tier expansion.
 * Launches 100% free: every user is automatically enrolled on the permanent Free plan
 * and no features or playback capabilities are restricted behind paid plans.
 */
class SubscriptionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    suspend fun ensureFreeSubscription(): Result<UserSubscription> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("userSubscriptions").document(uid)
        val snapshot = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
        if (snapshot.exists()) {
            val existing = FirestoreDocumentMapper.toUserSubscription(snapshot)
            if (existing != null) return@runCatching existing
        }
        val freeSub = SubscriptionCatalogDefaults.defaultFreeSubscription(uid)
        try {
            docRef.set(freeSub.toFreeEnrollmentMap(uid)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        freeSub
    }

    fun observeUserSubscription(userId: String): Flow<UserSubscription> {
        val docRef = db.collection("userSubscriptions").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toUserSubscription(snapshot)
                    ?: SubscriptionCatalogDefaults.defaultFreeSubscription(userId)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeSubscriptionPlans(includeInactiveForAdmin: Boolean = false): Flow<List<SubscriptionPlan>> {
        val colRef = db.collection("subscriptionPlans")
        val query = if (includeInactiveForAdmin) {
            colRef
        } else {
            colRef
                .whereEqualTo("isActive", true)
                .whereEqualTo("isPublic", true)
                .whereEqualTo("isArchived", false)
        }
        return query.snapshots()
            .map { snapshot ->
                val remote = FirestoreDocumentMapper.toSubscriptionPlans(snapshot)
                val filtered = if (includeInactiveForAdmin) {
                    remote
                } else {
                    remote.filter { it.isSelectableByViewer }
                }
                if (filtered.isEmpty()) {
                    if (includeInactiveForAdmin) {
                        SubscriptionCatalogDefaults.allBlueprintTiers
                    } else {
                        listOf(SubscriptionCatalogDefaults.freePlan)
                    }
                } else {
                    filtered.sortedBy { it.displayOrder }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun saveSubscriptionPlan(plan: SubscriptionPlan): Result<String> = runCatching {
        requireUserId()
        val planId = plan.id.ifBlank {
            plan.tierCode.ifBlank { plan.name }
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "plan-${System.currentTimeMillis()}" }
                .take(48)
        }
        val docRef = db.collection("subscriptionPlans").document(planId)
        val exists = docRef.get().await().exists()
        val normalized = if (planId == "free" || plan.isPermanentFree) {
            plan.copy(
                id = planId,
                isPermanentFree = true,
                isActive = true,
                isPublic = true,
                isArchived = false,
                monthlyPriceCents = 0,
                yearlyPriceCents = 0,
                billingInterval = "free"
            )
        } else {
            plan.copy(id = planId)
        }
        if (exists) {
            docRef.update(normalized.toWriteMap(isCreate = false)).await()
        } else {
            docRef.set(normalized.toWriteMap(isCreate = true)).await()
        }
        planId
    }

    suspend fun archiveSubscriptionPlan(plan: SubscriptionPlan, archived: Boolean): Result<Unit> = runCatching {
        requireUserId()
        require(!plan.isPermanentFree && plan.id != "free") {
            "The permanent Free plan cannot be archived."
        }
        val docRef = db.collection("subscriptionPlans").document(plan.id)
        docRef.update(
            mapOf(
                "isArchived" to archived,
                "isActive" to if (archived) false else plan.isActive,
                "isPublic" to if (archived) false else plan.isPublic,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun assignUserSubscription(
        targetUserId: String,
        plan: SubscriptionPlan,
        status: String = "active"
    ): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        require(targetUserId.isNotBlank()) { "Target user ID is required." }
        val docRef = db.collection("userSubscriptions").document(targetUserId)
        val existing = docRef.get().await()
        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "planId" to plan.id.ifBlank { plan.tierCode },
                    "planName" to plan.name,
                    "status" to status,
                    "isPermanentFree" to plan.isPermanentFree,
                    "assignedBy" to actorUid,
                    "expiresAt" to null,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to targetUserId,
                    "planId" to plan.id.ifBlank { plan.tierCode },
                    "planName" to plan.name,
                    "status" to status,
                    "isPermanentFree" to plan.isPermanentFree,
                    "assignedBy" to actorUid,
                    "startsAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "expiresAt" to null,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun updateUserSubscriptionStatus(
        targetUserId: String,
        newStatus: String
    ): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        require(newStatus in setOf("active", "paused", "expired", "canceled")) { "Invalid subscription status." }
        val docRef = db.collection("userSubscriptions").document(targetUserId)
        docRef.update(
            mapOf(
                "status" to newStatus,
                "assignedBy" to actorUid,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Viewer self-service selection restricted strictly to enabled, public, non-archived plans.
     * Enforced independently by Firestore security rules.
     */
    suspend fun selectPublicPlanForSelf(plan: SubscriptionPlan): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(plan.isSelectableByViewer) {
            "This subscription plan is not publicly available."
        }
        val planId = plan.id.ifBlank { plan.tierCode }
        val docRef = db.collection("userSubscriptions").document(uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            ensureFreeSubscription().getOrThrow()
        }
        docRef.update(
            mapOf(
                "planId" to planId,
                "planName" to plan.name,
                "status" to "active",
                "isPermanentFree" to plan.isPermanentFree,
                "assignedBy" to "self",
                "expiresAt" to null,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun observePlanFeatures(planId: String): Flow<List<PlanFeature>> {
        val colRef = db.collection("subscriptionPlans").document(planId).collection("features")
        return colRef.snapshots()
            .map { snapshot ->
                val remote = FirestoreDocumentMapper.toPlanFeatures(snapshot)
                if (remote.isEmpty() && planId == "free") {
                    SubscriptionCatalogDefaults.freePlanFeatures
                } else {
                    remote.sortedBy { it.displayOrder }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observeAllUserSubscriptionsForAdmin(): Flow<List<UserSubscription>> {
        val colRef = db.collection("userSubscriptions")
        return colRef.snapshots()
            .map { snapshot -> FirestoreDocumentMapper.toUserSubscriptions(snapshot) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun seedSubscriptionArchitectureIfEmpty(): Result<Boolean> = runCatching {
        requireUserId()
        val colRef = db.collection("subscriptionPlans")
        val existing = colRef.limit(1).get().await()
        if (!existing.isEmpty) return@runCatching false

        try {
            SubscriptionCatalogDefaults.allBlueprintTiers.forEach { plan ->
                val planRef = colRef.document(plan.id)
                planRef.set(plan.toWriteMap(isCreate = true)).await()
                if (plan.id == "free") {
                    SubscriptionCatalogDefaults.freePlanFeatures.forEach { feature ->
                        planRef.collection("features").document(feature.id)
                            .set(feature.toWriteMap(isCreate = true)).await()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            throw e
        }
        true
    }
}
