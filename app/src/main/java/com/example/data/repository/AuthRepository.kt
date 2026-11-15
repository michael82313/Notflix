package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "michael.tollackson82313@gmail.com"
        private const val TAG = "AuthRepository"
    }

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: throw IllegalStateException("User must be signed in.")

    fun isBootstrappedAdmin(user: FirebaseUser? = auth.currentUser): Boolean {
        if (user == null) return false
        return user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    suspend fun ensureAccountInitialized(): Result<UserAccount> = runCatching {
        val user = requireUser()
        val docRef = db.collection("users").document(user.uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val existing = snapshot.toObject(
                UserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (existing != null) return@runCatching existing
        }

        val resolvedName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: user.phoneNumber?.takeIf { it.isNotBlank() }
            ?: "Notflix Member"

        val initialAccount = UserAccount(
            id = user.uid,
            userId = user.uid,
            email = user.email ?: "",
            phoneNumber = user.phoneNumber?.takeIf { it.isNotBlank() },
            displayName = resolvedName,
            photoUrl = user.photoUrl?.toString(),
            role = if (isBootstrappedAdmin(user)) "admin" else "member",
            isSuspended = false,
            languagePreference = "English",
            autoplayNext = true,
            defaultQuality = "Auto",
            subtitlesEnabled = true,
            subtitleSize = "Medium"
        )
        try {
            docRef.set(initialAccount.toCreateMap()).await()
            if (isBootstrappedAdmin(user)) {
                ensureAdminRecord(user)
            }
            seedWelcomeNotification(user.uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        initialAccount
    }

    private suspend fun ensureAdminRecord(user: FirebaseUser) {
        val adminRef = db.collection("admins").document(user.uid)
        try {
            val snap = adminRef.get().await()
            if (!snap.exists()) {
                adminRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: BOOTSTRAPPED_ADMIN_EMAIL),
                        "grantedBy" to "bootstrap",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Admin record sync skipped: ${e.message}")
        }
    }

    private suspend fun seedWelcomeNotification(uid: String) {
        val notifRef = db.collection("users").document(uid)
            .collection("notifications").document("welcome_notflix")
        try {
            val snap = notifRef.get().await()
            if (!snap.exists()) {
                notifRef.set(
                    UserNotification(
                        userId = uid,
                        title = "Welcome to Notflix",
                        message = "All movies, series, profiles, and HD/HLS playback are completely free with zero ads.",
                        contentId = "tears-of-steel",
                        read = false
                    ).toCreateMap()
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Welcome notification skipped: ${e.message}")
        }
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
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

    fun observeIsAdmin(userId: String): Flow<Boolean> {
        val adminRef = db.collection("admins").document(userId)
        return adminRef.snapshots()
            .map { snapshot ->
                snapshot.exists() || isBootstrappedAdmin(auth.currentUser)
            }
            .catch { _ ->
                emit(isBootstrappedAdmin(auth.currentUser))
            }
    }

    suspend fun updatePreferences(
        userId: String,
        displayName: String,
        languagePreference: String,
        autoplayNext: Boolean,
        defaultQuality: String,
        subtitlesEnabled: Boolean,
        subtitleSize: String
    ): Result<Unit> = runCatching {
        val uid = requireUser().uid
        require(uid == userId) { "Cannot update another user's preferences." }
        val docRef = db.collection("users").document(userId)
        val updates = mapOf(
            "displayName" to displayName.trim().take(100).ifBlank { "Notflix Viewer" },
            "languagePreference" to languagePreference,
            "autoplayNext" to autoplayNext,
            "defaultQuality" to defaultQuality,
            "subtitlesEnabled" to subtitlesEnabled,
            "subtitleSize" to subtitleSize,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> {
        val colRef = db.collection("users").document(userId).collection("notifications")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
        try {
            docRef.update("read", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
