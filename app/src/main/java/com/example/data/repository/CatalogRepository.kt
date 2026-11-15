package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.PlaybackProgress
import com.example.data.model.Season
import com.example.data.model.SeasonDraft
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.seed.SeedCatalog
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CatalogRepository(
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

    // --- Catalog Observation ---

    fun observeCatalog(includeHiddenForAdmin: Boolean = false): Flow<List<CatalogItem>> {
        val colRef = db.collection("catalog")
        val query = if (includeHiddenForAdmin) {
            colRef
        } else {
            colRef.whereEqualTo("isVisible", true)
        }
        return query.snapshots()
            .map { snapshot ->
                val remoteItems = snapshot.toObjects(
                    CatalogItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remoteItems.isEmpty() && !includeHiddenForAdmin) {
                    SeedCatalog.defaultItems
                } else if (remoteItems.isEmpty()) {
                    SeedCatalog.defaultItems
                } else {
                    remoteItems.sortedByDescending { it.trendingScore }
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

    fun observeGenres(includeInactiveForAdmin: Boolean = false): Flow<List<Genre>> {
        val colRef = db.collection("genres")
        val query = if (includeInactiveForAdmin) colRef else colRef.whereEqualTo("isActive", true)
        return query.snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    Genre::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    SeedCatalog.defaultGenres
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

    fun observeSeasons(contentId: String, includeHiddenForAdmin: Boolean = false): Flow<List<Season>> {
        val colRef = db.collection("catalog").document(contentId).collection("seasons")
        val query = if (includeHiddenForAdmin) colRef else colRef.whereEqualTo("isVisible", true)
        return query.snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    Season::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    SeedCatalog.defaultSeasonsForSeries(contentId)
                } else {
                    remote.sortedBy { it.seasonNumber }
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

    fun observeEpisodes(
        contentId: String,
        seasonId: String,
        seasonNumber: Int,
        includeHiddenForAdmin: Boolean = false
    ): Flow<List<Episode>> {
        val colRef = db.collection("catalog").document(contentId)
            .collection("seasons").document(seasonId)
            .collection("episodes")
        val query = if (includeHiddenForAdmin) colRef else colRef.whereEqualTo("isVisible", true)
        return query.snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    Episode::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote.isEmpty()) {
                    SeedCatalog.defaultEpisodesForSeason(contentId, seasonNumber)
                } else {
                    remote.sortedBy { it.episodeNumber }
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

    // --- Recommendation Engine ---

    fun computeRecommendations(
        catalog: List<CatalogItem>,
        profile: UserProfile?,
        progress: List<PlaybackProgress>,
        ratings: List<ContentRating>
    ): List<CatalogItem> {
        val visible = catalog.filter { item ->
            item.isVisible && (profile?.isKids != true || item.isKidsFriendly)
        }
        if (visible.isEmpty()) return emptyList()

        val dislikedIds = ratings.filter { it.liked < 0 }.map { it.contentId }.toSet()
        val likedIds = ratings.filter { it.liked > 0 || it.stars >= 4 }.map { it.contentId }.toSet()
        val watchedIds = progress.map { it.contentId }.toSet()

        val affinityWeights = mutableMapOf<String, Int>()
        profile?.preferredGenres?.forEach { genre ->
            affinityWeights[genre.lowercase()] = (affinityWeights[genre.lowercase()] ?: 0) + 30
        }
        visible.filter { it.id in watchedIds || it.id in likedIds }.forEach { watched ->
            val boost = if (watched.id in likedIds) 35 else 20
            watched.genres.forEach { genre ->
                affinityWeights[genre.lowercase()] = (affinityWeights[genre.lowercase()] ?: 0) + boost
            }
        }

        return visible
            .filter { it.id !in dislikedIds }
            .sortedByDescending { item ->
                val genreScore = item.genres.sumOf { affinityWeights[it.lowercase()] ?: 0 }
                val unwatchedBonus = if (item.id !in watchedIds) 15 else 0
                genreScore + unwatchedBonus + (item.trendingScore / 20)
            }
    }

    fun computeSimilarTitles(target: CatalogItem, catalog: List<CatalogItem>, isKids: Boolean): List<CatalogItem> {
        val targetGenres = target.genres.map { it.lowercase() }.toSet()
        return catalog
            .filter { it.id != target.id && it.isVisible && (!isKids || it.isKidsFriendly) }
            .sortedByDescending { candidate ->
                val overlap = candidate.genres.count { it.lowercase() in targetGenres }
                val sameType = if (candidate.mediaType == target.mediaType) 1 else 0
                overlap * 100 + sameType * 20 + candidate.trendingScore / 50
            }
            .take(8)
    }

    // --- Admin Catalog & Platform Management ---

    suspend fun seedDefaultCatalogIfEmpty(): Result<Boolean> = runCatching {
        requireUserId()
        val existingCatalog = db.collection("catalog").limit(1).get().await()
        var seeded = false
        if (existingCatalog.isEmpty) {
            SeedCatalog.defaultItems.forEach { item ->
                val docRef = db.collection("catalog").document(item.id)
                docRef.set(item.toWriteMap(isCreate = true)).await()
                if (item.isSeries) {
                    val seasons = SeedCatalog.defaultSeasonsForSeries(item.id)
                    seasons.forEach { season ->
                        val sRef = docRef.collection("seasons").document(season.id)
                        sRef.set(season.toWriteMap(isCreate = true)).await()
                        val episodes = SeedCatalog.defaultEpisodesForSeason(item.id, season.seasonNumber)
                        episodes.forEach { ep ->
                            sRef.collection("episodes").document(ep.id)
                                .set(ep.toWriteMap(isCreate = true)).await()
                        }
                    }
                }
            }
            seeded = true
        }
        val existingGenres = db.collection("genres").limit(1).get().await()
        if (existingGenres.isEmpty) {
            SeedCatalog.defaultGenres.forEach { genre ->
                db.collection("genres").document(genre.id)
                    .set(genre.toWriteMap(isCreate = true)).await()
            }
            seeded = true
        }
        seeded
    }

    suspend fun saveCatalogItem(item: CatalogItem): Result<String> = runCatching {
        requireUserId()
        val isNewId = item.id.isBlank()
        val docRef = if (isNewId) {
            db.collection("catalog").document()
        } else {
            db.collection("catalog").document(item.id)
        }
        try {
            val exists = if (isNewId) false else docRef.get().await().exists()
            if (!exists) {
                docRef.set(item.toWriteMap(isCreate = true)).await()
            } else {
                docRef.update(item.toWriteMap(isCreate = false)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isNewId) OperationType.CREATE else OperationType.UPDATE,
                docRef.path
            )
            throw e
        }
        docRef.id
    }

    suspend fun loadSeriesStructure(contentId: String): Result<List<SeasonDraft>> = runCatching {
        requireUserId()
        val seasonsRef = db.collection("catalog").document(contentId).collection("seasons")
        val remoteSeasons = try {
            seasonsRef.get().await().toObjects(
                Season::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.seasonNumber }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, seasonsRef.path)
            throw e
        }

        if (remoteSeasons.isEmpty()) {
            val fallbackSeasons = SeedCatalog.defaultSeasonsForSeries(contentId)
            return@runCatching fallbackSeasons.map { season ->
                SeasonDraft(
                    season = season,
                    episodes = SeedCatalog.defaultEpisodesForSeason(contentId, season.seasonNumber)
                )
            }
        }

        remoteSeasons.map { season ->
            val epRef = seasonsRef.document(season.id).collection("episodes")
            val remoteEps = epRef.get().await().toObjects(
                Episode::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.episodeNumber }
            SeasonDraft(
                season = season,
                episodes = remoteEps.ifEmpty {
                    SeedCatalog.defaultEpisodesForSeason(contentId, season.seasonNumber)
                }
            )
        }
    }

    suspend fun saveSeriesWithSeasons(
        series: CatalogItem,
        seasonDrafts: List<SeasonDraft>,
        removedSeasonIds: Set<String> = emptySet(),
        removedEpisodeRefs: List<Pair<String, String>> = emptyList()
    ): Result<String> = runCatching {
        requireUserId()
        val contentId = saveCatalogItem(series.copy(mediaType = "series")).getOrThrow()
        val catalogDoc = db.collection("catalog").document(contentId)

        // Delete explicitly removed episodes first
        for ((seasonId, episodeId) in removedEpisodeRefs) {
            if (seasonId.isNotBlank() && episodeId.isNotBlank()) {
                val epRef = catalogDoc.collection("seasons").document(seasonId)
                    .collection("episodes").document(episodeId)
                try {
                    epRef.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, epRef.path)
                    throw e
                }
            }
        }

        // Delete explicitly removed seasons (and their episodes)
        for (seasonId in removedSeasonIds) {
            if (seasonId.isNotBlank()) {
                deleteSeason(contentId, seasonId).getOrThrow()
            }
        }

        // Upsert each season and its episodes
        for (draft in seasonDrafts.sortedBy { it.season.seasonNumber }) {
            val seasonId = draft.season.id.ifBlank { "season-${draft.season.seasonNumber}" }
            val seasonRef = catalogDoc.collection("seasons").document(seasonId)
            try {
                val seasonExists = seasonRef.get().await().exists()
                if (!seasonExists) {
                    seasonRef.set(draft.season.toWriteMap(isCreate = true)).await()
                } else {
                    seasonRef.update(draft.season.toWriteMap(isCreate = false)).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, seasonRef.path)
                throw e
            }

            for (ep in draft.episodes.sortedBy { it.episodeNumber }) {
                val resolvedEpId = ep.id.ifBlank { "s${draft.season.seasonNumber}e${ep.episodeNumber}" }
                val epRef = seasonRef.collection("episodes").document(resolvedEpId)
                val normalizedEp = ep.copy(seasonNumber = draft.season.seasonNumber)
                try {
                    val epExists = epRef.get().await().exists()
                    if (!epExists) {
                        epRef.set(normalizedEp.toWriteMap(isCreate = true)).await()
                    } else {
                        epRef.update(normalizedEp.toWriteMap(isCreate = false)).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, epRef.path)
                    throw e
                }
            }
        }

        contentId
    }

    suspend fun deleteCatalogItem(contentId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("catalog").document(contentId)
        try {
            val seasonsSnap = docRef.collection("seasons").get().await()
            for (seasonDoc in seasonsSnap.documents) {
                val epsSnap = seasonDoc.reference.collection("episodes").get().await()
                for (epDoc in epsSnap.documents) {
                    epDoc.reference.delete().await()
                }
                seasonDoc.reference.delete().await()
            }
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun addSeason(contentId: String, season: Season): Result<String> = runCatching {
        requireUserId()
        val colRef = db.collection("catalog").document(contentId).collection("seasons")
        val docRef = if (season.id.isBlank()) colRef.document("season-${season.seasonNumber}") else colRef.document(season.id)
        try {
            val exists = docRef.get().await().exists()
            if (!exists) {
                docRef.set(season.toWriteMap(isCreate = true)).await()
            } else {
                docRef.update(season.toWriteMap(isCreate = false)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun deleteSeason(contentId: String, seasonId: String): Result<Unit> = runCatching {
        requireUserId()
        val seasonRef = db.collection("catalog").document(contentId)
            .collection("seasons").document(seasonId)
        try {
            val eps = seasonRef.collection("episodes").get().await()
            for (epDoc in eps.documents) {
                epDoc.reference.delete().await()
            }
            seasonRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, seasonRef.path)
            throw e
        }
    }

    suspend fun addEpisode(contentId: String, seasonId: String, episode: Episode): Result<String> = runCatching {
        requireUserId()
        val colRef = db.collection("catalog").document(contentId)
            .collection("seasons").document(seasonId)
            .collection("episodes")
        val docRef = if (episode.id.isBlank()) colRef.document() else colRef.document(episode.id)
        try {
            val exists = docRef.get().await().exists()
            if (!exists) {
                docRef.set(episode.toWriteMap(isCreate = true)).await()
            } else {
                docRef.update(episode.toWriteMap(isCreate = false)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun deleteEpisode(contentId: String, seasonId: String, episodeId: String): Result<Unit> = runCatching {
        requireUserId()
        val epRef = db.collection("catalog").document(contentId)
            .collection("seasons").document(seasonId)
            .collection("episodes").document(episodeId)
        try {
            epRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, epRef.path)
            throw e
        }
    }

    suspend fun saveGenre(genre: Genre): Result<String> = runCatching {
        requireUserId()
        val docRef = if (genre.id.isBlank()) {
            val slug = genre.name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "genre" }
            db.collection("genres").document(slug)
        } else {
            db.collection("genres").document(genre.id)
        }
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(genre.toWriteMap(isCreate = false)).await()
            } else {
                docRef.set(genre.toWriteMap(isCreate = true)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun deleteGenre(genreId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("genres").document(genreId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserAccount>> {
        val colRef = db.collection("users")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserAccount::class.java,
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

    suspend fun setUserAdminRole(targetUser: UserAccount, makeAdmin: Boolean): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val userRef = db.collection("users").document(targetUser.userId)
        val adminRef = db.collection("admins").document(targetUser.userId)
        try {
            userRef.update(
                mapOf(
                    "role" to if (makeAdmin) "admin" else "member",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (makeAdmin) {
                adminRef.set(
                    mapOf(
                        "uid" to targetUser.userId,
                        "email" to targetUser.email,
                        "grantedBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                adminRef.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }
}
