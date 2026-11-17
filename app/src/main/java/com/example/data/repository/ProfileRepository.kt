package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.PlaybackProgress
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserProfile
import com.example.data.model.WatchlistItem
import com.example.data.remote.FirestoreDocumentMapper
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.profileDataStore by preferencesDataStore(name = "velora_active_profile")

class ProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        appContext = context.applicationContext
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    private fun activeProfileKey(userId: String) = stringPreferencesKey("active_profile_$userId")

    suspend fun getSavedActiveProfileId(userId: String): String? {
        val ctx = appContext ?: return null
        return runCatching {
            ctx.profileDataStore.data.map { prefs -> prefs[activeProfileKey(userId)] }.first()
        }.getOrNull()
    }

    suspend fun setSavedActiveProfileId(userId: String, profileId: String) {
        val ctx = appContext ?: return
        runCatching {
            ctx.profileDataStore.edit { prefs ->
                prefs[activeProfileKey(userId)] = profileId
            }
        }
    }

    // --- Profiles ---

    fun observeProfiles(userId: String): Flow<List<UserProfile>> {
        val colRef = db.collection("users").document(userId).collection("profiles")
        return colRef.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toUserProfiles(snapshot)
                    .sortedBy { it.createdAt?.seconds ?: 0L }
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

    suspend fun ensureDefaultProfilesExist(userId: String, defaultName: String): Result<List<UserProfile>> =
        runCatching {
            val uid = requireUserId()
            require(uid == userId) { "UID mismatch" }
            val colRef = db.collection("users").document(userId).collection("profiles")
            val existing = FirestoreDocumentMapper.toUserProfiles(colRef.get().await())
            if (existing.isNotEmpty()) return@runCatching existing

            val mainProfile = UserProfile(
                userId = userId,
                name = defaultName.trim().take(24).ifBlank { "Main" },
                avatarKey = "crimson",
                isKids = false,
                preferredGenres = listOf("Sci-Fi", "Thriller", "Drama"),
                language = "English",
                subtitlesEnabled = true
            )
            val kidsProfile = UserProfile(
                userId = userId,
                name = "Kids",
                avatarKey = "sky",
                isKids = true,
                preferredGenres = listOf("Animation", "Adventure"),
                language = "English",
                subtitlesEnabled = true
            )
            val mainRef = colRef.document("primary")
            val kidsRef = colRef.document("kids")
            try {
                mainRef.set(mainProfile.toCreateMap()).await()
                kidsRef.set(kidsProfile.toCreateMap()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, colRef.path)
                throw e
            }
            listOf(mainProfile.copy(id = "primary"), kidsProfile.copy(id = "kids"))
        }

    suspend fun createProfile(
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>,
        language: String = "English",
        subtitlesEnabled: Boolean = true
    ): Result<String> = createProfileForUser(
        targetUserId = requireUserId(),
        name = name,
        avatarKey = avatarKey,
        isKids = isKids,
        preferredGenres = preferredGenres,
        language = language,
        subtitlesEnabled = subtitlesEnabled
    )

    suspend fun createProfileForUser(
        targetUserId: String,
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>,
        language: String = "English",
        subtitlesEnabled: Boolean = true
    ): Result<String> = runCatching {
        requireUserId()
        val colRef = db.collection("users").document(targetUserId).collection("profiles")
        val docRef = colRef.document()
        val profile = UserProfile(
            userId = targetUserId,
            name = name.trim().take(40).ifBlank { "Viewer" },
            avatarKey = avatarKey,
            isKids = isKids,
            preferredGenres = preferredGenres.take(10),
            language = language,
            subtitlesEnabled = subtitlesEnabled
        )
        try {
            docRef.set(profile.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun updateProfile(
        profileId: String,
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>,
        language: String,
        subtitlesEnabled: Boolean
    ): Result<Unit> = updateProfileForUser(
        targetUserId = requireUserId(),
        profileId = profileId,
        name = name,
        avatarKey = avatarKey,
        isKids = isKids,
        preferredGenres = preferredGenres,
        language = language,
        subtitlesEnabled = subtitlesEnabled
    )

    suspend fun updateProfileForUser(
        targetUserId: String,
        profileId: String,
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>,
        language: String,
        subtitlesEnabled: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        require(targetUserId.isNotBlank() && profileId.isNotBlank()) { "Invalid user or profile ID." }
        val docRef = db.collection("users").document(targetUserId)
            .collection("profiles").document(profileId)
        val updates = mapOf(
            "name" to name.trim().take(40).ifBlank { "Viewer" },
            "avatarKey" to avatarKey,
            "isKids" to isKids,
            "preferredGenres" to preferredGenres.take(10),
            "language" to language,
            "subtitlesEnabled" to subtitlesEnabled,
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

    suspend fun deleteProfile(profileId: String): Result<Unit> =
        deleteProfileForUser(targetUserId = requireUserId(), profileId = profileId)

    suspend fun deleteProfileForUser(targetUserId: String, profileId: String): Result<Unit> = runCatching {
        requireUserId()
        require(targetUserId.isNotBlank() && profileId.isNotBlank()) { "Invalid user or profile ID." }
        val docRef = db.collection("users").document(targetUserId)
            .collection("profiles").document(profileId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Watchlist (My List) ---

    fun observeWatchlist(userId: String, profileId: String): Flow<List<WatchlistItem>> {
        val colRef = db.collection("users").document(userId)
            .collection("profiles").document(profileId)
            .collection("watchlist")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WatchlistItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.addedAt?.seconds ?: 0L }
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

    suspend fun toggleWatchlist(
        profileId: String,
        item: CatalogItem,
        currentlyInList: Boolean
    ): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("watchlist").document(item.id)
        try {
            if (currentlyInList) {
                docRef.delete().await()
                false
            } else {
                val entry = WatchlistItem(
                    contentId = item.id,
                    mediaType = item.mediaType,
                    title = item.title
                )
                docRef.set(entry.toCreateMap()).await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (currentlyInList) OperationType.DELETE else OperationType.CREATE,
                docRef.path
            )
            throw e
        }
    }

    // --- Playback Progress & Watch History ---

    fun observeProgress(userId: String, profileId: String): Flow<List<PlaybackProgress>> {
        val colRef = db.collection("users").document(userId)
            .collection("profiles").document(profileId)
            .collection("progress")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PlaybackProgress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
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

    suspend fun savePlaybackProgress(
        profileId: String,
        contentId: String,
        episodeId: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        title: String,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val progressId = if (episodeId.isNullOrBlank()) contentId else "${contentId}_${episodeId}"
        val docRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("progress").document(progressId)

        val completed = com.example.ui.state.CompletionTracker.isPlaybackCompleted(positionMs, durationMs)
        val payload = PlaybackProgress(
            contentId = contentId,
            episodeId = episodeId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            title = title,
            positionMs = positionMs,
            durationMs = durationMs,
            completed = completed
        ).toWriteMap()

        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProgressItem(profileId: String, progressId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("progress").document(progressId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun clearAllProgress(profileId: String, progressIds: List<String>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("progress")
        try {
            progressIds.forEach { id ->
                colRef.document(id).delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            throw e
        }
    }

    // --- Ratings & Likes ---

    fun observeRatings(userId: String, profileId: String): Flow<List<ContentRating>> {
        val colRef = db.collection("users").document(userId)
            .collection("profiles").document(profileId)
            .collection("ratings")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ContentRating::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
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

    suspend fun saveRating(
        profileId: String,
        contentId: String,
        liked: Int,
        stars: Int
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("ratings").document(contentId)
        val payload = ContentRating(
            contentId = contentId,
            liked = liked.coerceIn(-1, 1),
            stars = stars.coerceIn(0, 5)
        ).toWriteMap()
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Search History ---

    fun observeSearchHistory(userId: String, profileId: String): Flow<List<SearchHistoryItem>> {
        val colRef = db.collection("users").document(userId)
            .collection("profiles").document(profileId)
            .collection("searchHistory")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SearchHistoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.searchedAt?.seconds ?: 0L }
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

    suspend fun recordSearchQuery(profileId: String, rawQuery: String): Result<Unit> = runCatching {
        val trimmed = rawQuery.trim()
        if (trimmed.length < 2) return@runCatching
        val uid = requireUserId()
        val slugId = trimmed.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            .take(64).ifBlank { "query" }
        val docRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("searchHistory").document(slugId)
        try {
            docRef.set(SearchHistoryItem(query = trimmed).toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSearchHistoryItem(profileId: String, queryId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("searchHistory").document(queryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
