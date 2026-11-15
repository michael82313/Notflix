package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PlanFeature
import com.example.data.model.SubscriptionCatalogDefaults
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserSubscription
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
            val existing = snapshot.toObject(
                UserSubscription::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
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
                snapshot.toObject(
                    UserSubscription::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: SubscriptionCatalogDefaults.defaultFreeSubscription(userId)
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
            colRef.whereEqualTo("isActive", true)
        }
        return query.snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    SubscriptionPlan::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    if (includeInactiveForAdmin) {
                        SubscriptionCatalogDefaults.allBlueprintTiers
                    } else {
                        listOf(SubscriptionCatalogDefaults.freePlan)
                    }
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

    fun observePlanFeatures(planId: String): Flow<List<PlanFeature>> {
        val colRef = db.collection("subscriptionPlans").document(planId).collection("features")
        return colRef.snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    PlanFeature::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
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
            .map { snapshot ->
                snapshot.toObjects(
                    UserSubscription::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
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
