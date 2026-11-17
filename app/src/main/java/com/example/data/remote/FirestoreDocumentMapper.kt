package com.example.data.remote

import android.util.Log
import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.HomepageRowRule
import com.example.data.model.HomepageSettings
import com.example.data.model.PlanFeature
import com.example.data.model.Season
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

/**
 * Defensive Firestore document deserialization helpers.
 *
 * Unlike raw [DocumentSnapshot.toObject], these helpers never throw on explicit JSON nulls,
 * legacy "id" fields inside document data, missing optional keys, or numeric/Timestamp coercion
 * mismatches, preventing single malformed documents in Firestore from crashing listeners or UI.
 */
object FirestoreDocumentMapper {

    private const val TAG = "FirestoreDocMapper"

    fun toUserAccount(doc: DocumentSnapshot): UserAccount? {
        val data = doc.safeData() ?: return null
        return toUserAccount(doc.id, data)
    }

    fun toUserAccount(id: String, data: Map<String, Any?>): UserAccount = UserAccount(
        id = id,
        userId = data.str("userId", id),
        email = data.str("email", ""),
        phoneNumber = data.optStr("phoneNumber"),
        displayName = data.str("displayName", ""),
        photoUrl = data.optStr("photoUrl"),
        role = data.str("role", "member"),
        isSuspended = data.bool("isSuspended", false),
        languagePreference = data.str("languagePreference", "English"),
        autoplayNext = data.bool("autoplayNext", true),
        defaultQuality = data.str("defaultQuality", "Auto"),
        subtitlesEnabled = data.bool("subtitlesEnabled", true),
        subtitleSize = data.str("subtitleSize", "Medium"),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toUserAccounts(snapshot: QuerySnapshot): List<UserAccount> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toUserAccount(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed user document ${doc.id}", it) }
                .getOrNull()
        }

    fun toUserProfile(doc: DocumentSnapshot): UserProfile? {
        val data = doc.safeData() ?: return null
        return toUserProfile(doc.id, data)
    }

    fun toUserProfile(id: String, data: Map<String, Any?>): UserProfile = UserProfile(
        id = id,
        userId = data.str("userId", ""),
        name = data.str("name", "Viewer"),
        avatarKey = data.str("avatarKey", "crimson"),
        isKids = data.bool("isKids", false),
        preferredGenres = data.strList("preferredGenres"),
        language = data.str("language", "English"),
        subtitlesEnabled = data.bool("subtitlesEnabled", true),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toUserProfiles(snapshot: QuerySnapshot): List<UserProfile> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toUserProfile(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed profile document ${doc.id}", it) }
                .getOrNull()
        }

    fun toCatalogItem(doc: DocumentSnapshot): CatalogItem? {
        val data = doc.safeData() ?: return null
        return toCatalogItem(doc.id, data)
    }

    fun toCatalogItem(id: String, data: Map<String, Any?>): CatalogItem = CatalogItem(
        id = id,
        title = data.str("title", "Untitled Title"),
        synopsis = data.str("synopsis", ""),
        mediaType = data.str("mediaType", "movie").ifBlank { "movie" },
        genres = data.strList("genres"),
        releaseYear = data.int("releaseYear", 2025),
        runtimeMinutes = data.int("runtimeMinutes", 90),
        ageRating = data.str("ageRating", "PG-13").ifBlank { "PG-13" },
        director = data.str("director", ""),
        cast = data.strList("cast"),
        posterKey = data.str("posterKey", "img_poster_chronicle").ifBlank { "img_poster_chronicle" },
        posterUrl = data.optStr("posterUrl"),
        backdropKey = data.str("backdropKey", "img_hero_nebula").ifBlank { "img_hero_nebula" },
        backdropUrl = data.optStr("backdropUrl"),
        trailerUrl = data.optStr("trailerUrl"),
        videoUrl = data.str("videoUrl", ""),
        videoSourceType = data.str("videoSourceType", "auto").ifBlank { "auto" },
        providerName = data.optStr("providerName"),
        hlsUrl = data.optStr("hlsUrl"),
        isFeatured = data.bool("isFeatured", false),
        isVisible = data.bool("isVisible", true),
        isKidsFriendly = data.bool("isKidsFriendly", false),
        trendingScore = data.int("trendingScore", 80),
        popularityRank = data.int("popularityRank", 10),
        tags = data.strList("tags"),
        keywords = data.strList("keywords"),
        isStaffPick = data.bool("isStaffPick", false),
        isAwardWinning = data.bool("isAwardWinning", false),
        isComingSoon = data.bool("isComingSoon", false),
        expectedReleaseDate = data.optStr("expectedReleaseDate"),
        leavingSoonAt = data.optStr("leavingSoonAt"),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toCatalogItems(snapshot: QuerySnapshot): List<CatalogItem> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toCatalogItem(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed catalog document ${doc.id}", it) }
                .getOrNull()
        }

    fun toSeason(doc: DocumentSnapshot): Season? {
        val data = doc.safeData() ?: return null
        return toSeason(doc.id, data)
    }

    fun toSeason(id: String, data: Map<String, Any?>): Season {
        val sNum = data.int("seasonNumber", 1).coerceAtLeast(1)
        return Season(
            id = id,
            seasonNumber = sNum,
            title = data.str("title", "Season $sNum").ifBlank { "Season $sNum" },
            year = data.int("year", 2025),
            isVisible = data.bool("isVisible", true),
            createdAt = data.ts("createdAt"),
            updatedAt = data.ts("updatedAt")
        )
    }

    fun toSeasons(snapshot: QuerySnapshot): List<Season> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toSeason(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed season document ${doc.id}", it) }
                .getOrNull()
        }

    fun toEpisode(doc: DocumentSnapshot): Episode? {
        val data = doc.safeData() ?: return null
        return toEpisode(doc.id, data)
    }

    fun toEpisode(id: String, data: Map<String, Any?>): Episode = Episode(
        id = id,
        seasonNumber = data.int("seasonNumber", 1).coerceAtLeast(1),
        episodeNumber = data.int("episodeNumber", 1).coerceAtLeast(1),
        title = data.str("title", ""),
        synopsis = data.str("synopsis", ""),
        runtimeMinutes = data.int("runtimeMinutes", 42),
        thumbnailKey = data.str("thumbnailKey", "img_hero_nebula").ifBlank { "img_hero_nebula" },
        thumbnailUrl = data.optStr("thumbnailUrl"),
        videoUrl = data.str("videoUrl", ""),
        videoSourceType = data.str("videoSourceType", "auto").ifBlank { "auto" },
        providerName = data.optStr("providerName"),
        hlsUrl = data.optStr("hlsUrl"),
        isVisible = data.bool("isVisible", true),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toEpisodes(snapshot: QuerySnapshot): List<Episode> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toEpisode(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed episode document ${doc.id}", it) }
                .getOrNull()
        }

    fun toGenre(doc: DocumentSnapshot): Genre? {
        val data = doc.safeData() ?: return null
        return toGenre(doc.id, data)
    }

    fun toGenre(id: String, data: Map<String, Any?>): Genre = Genre(
        id = id,
        name = data.str("name", id),
        slug = data.str("slug", id),
        displayOrder = data.int("displayOrder", 0),
        isActive = data.bool("isActive", true),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toGenres(snapshot: QuerySnapshot): List<Genre> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toGenre(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed genre document ${doc.id}", it) }
                .getOrNull()
        }

    fun toSubscriptionPlan(doc: DocumentSnapshot): SubscriptionPlan? {
        val data = doc.safeData() ?: return null
        return toSubscriptionPlan(doc.id, data)
    }

    fun toSubscriptionPlan(id: String, data: Map<String, Any?>): SubscriptionPlan = SubscriptionPlan(
        id = id,
        tierCode = data.str("tierCode", id.ifBlank { "free" }),
        name = data.str("name", "Free"),
        description = data.str("description", ""),
        isPermanentFree = data.bool("isPermanentFree", false),
        isActive = data.bool("isActive", false),
        monthlyPriceCents = data.int("monthlyPriceCents", 0),
        yearlyPriceCents = data.int("yearlyPriceCents", 0),
        maxQuality = data.str("maxQuality", "Auto"),
        maxSimultaneousStreams = data.int("maxSimultaneousStreams", 4),
        maxProfiles = data.int("maxProfiles", 5),
        featuresSummary = data.strList("featuresSummary"),
        displayOrder = data.int("displayOrder", 0),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toSubscriptionPlans(snapshot: QuerySnapshot): List<SubscriptionPlan> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toSubscriptionPlan(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed plan document ${doc.id}", it) }
                .getOrNull()
        }

    fun toPlanFeature(doc: DocumentSnapshot): PlanFeature? {
        val data = doc.safeData() ?: return null
        return toPlanFeature(doc.id, data)
    }

    fun toPlanFeature(id: String, data: Map<String, Any?>): PlanFeature = PlanFeature(
        id = id,
        featureKey = data.str("featureKey", id),
        label = data.str("label", ""),
        description = data.str("description", ""),
        enabled = data.bool("enabled", true),
        displayOrder = data.int("displayOrder", 0),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toPlanFeatures(snapshot: QuerySnapshot): List<PlanFeature> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toPlanFeature(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed plan feature document ${doc.id}", it) }
                .getOrNull()
        }

    fun toUserSubscription(doc: DocumentSnapshot): UserSubscription? {
        val data = doc.safeData() ?: return null
        return toUserSubscription(doc.id, data)
    }

    fun toUserSubscription(id: String, data: Map<String, Any?>): UserSubscription = UserSubscription(
        id = id,
        userId = data.str("userId", id),
        planId = data.str("planId", "free"),
        planName = data.str("planName", "Free"),
        status = data.str("status", "active"),
        isPermanentFree = data.bool("isPermanentFree", true),
        assignedBy = data.str("assignedBy", "system"),
        startsAt = data.ts("startsAt"),
        expiresAt = data.ts("expiresAt"),
        createdAt = data.ts("createdAt"),
        updatedAt = data.ts("updatedAt")
    )

    fun toUserSubscriptions(snapshot: QuerySnapshot): List<UserSubscription> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toUserSubscription(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed subscription document ${doc.id}", it) }
                .getOrNull()
        }

    fun toHomepageSettings(doc: DocumentSnapshot): HomepageSettings? {
        val data = doc.safeData() ?: return null
        return toHomepageSettings(doc.id, data)
    }

    fun toHomepageSettings(id: String, data: Map<String, Any?>): HomepageSettings = HomepageSettings(
        id = id.ifBlank { "global" },
        maxItemsPerRow = data.int("maxItemsPerRow", 10).coerceIn(3, 30),
        maxBecauseYouWatchedRows = data.int("maxBecauseYouWatchedRows", 4).coerceIn(0, 10),
        enabledGenres = data.strList("enabledGenres"),
        updatedAt = data.ts("updatedAt")
    )

    fun toHomepageRowRule(doc: DocumentSnapshot): HomepageRowRule? {
        val data = doc.safeData() ?: return null
        return toHomepageRowRule(doc.id, data)
    }

    fun toHomepageRowRule(id: String, data: Map<String, Any?>): HomepageRowRule = HomepageRowRule(
        id = id,
        rowKey = data.str("rowKey", id).ifBlank { id },
        title = data.str("title", id),
        enabled = data.bool("enabled", true),
        displayOrder = data.int("displayOrder", 0).coerceAtLeast(0),
        maxItems = data.int("maxItems", 10).coerceIn(1, 30),
        personalized = data.bool("personalized", false),
        updatedAt = data.ts("updatedAt")
    )

    fun toHomepageRowRules(snapshot: QuerySnapshot): List<HomepageRowRule> =
        snapshot.documents.mapNotNull { doc ->
            runCatching { toHomepageRowRule(doc) }
                .onFailure { Log.w(TAG, "Skipping malformed homepage row rule ${doc.id}", it) }
                .getOrNull()
        }

    private fun DocumentSnapshot.safeData(): Map<String, Any?>? {
        if (!exists()) return null
        return getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: data
    }

    private fun Map<String, Any?>.str(key: String, default: String): String {
        val value = this[key] ?: return default
        return (value as? String) ?: value.toString()
    }

    private fun Map<String, Any?>.optStr(key: String): String? {
        val value = this[key] ?: return null
        return ((value as? String) ?: value.toString()).takeIf { it.isNotBlank() }
    }

    private fun Map<String, Any?>.bool(key: String, default: Boolean): Boolean {
        return when (val value = this[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: default
            else -> default
        }
    }

    private fun Map<String, Any?>.int(key: String, default: Int): Int {
        return when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.trim().toIntOrNull() ?: default
            else -> default
        }
    }

    private fun Map<String, Any?>.strList(key: String): List<String> {
        val raw = this[key] ?: return emptyList()
        return (raw as? List<*>)
            ?.mapNotNull { element -> element?.toString()?.takeIf { it.isNotBlank() } }
            ?: emptyList()
    }

    private fun Map<String, Any?>.ts(key: String): Timestamp? = this[key] as? Timestamp
}
