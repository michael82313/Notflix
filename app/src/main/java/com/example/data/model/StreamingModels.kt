package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = "member",
    val isSuspended: Boolean = false,
    val languagePreference: String = "English",
    val autoplayNext: Boolean = true,
    val defaultQuality: String = "Auto",
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: String = "Medium",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("email", email)
        if (!phoneNumber.isNullOrBlank()) put("phoneNumber", phoneNumber)
        put("displayName", displayName.ifBlank { "Notflix Viewer" })
        if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl)
        put("role", role)
        put("isSuspended", isSuspended)
        put("languagePreference", languagePreference)
        put("autoplayNext", autoplayNext)
        put("defaultQuality", defaultQuality)
        put("subtitlesEnabled", subtitlesEnabled)
        put("subtitleSize", subtitleSize)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val avatarKey: String = "crimson",
    val isKids: Boolean = false,
    val preferredGenres: List<String> = emptyList(),
    val language: String = "English",
    val subtitlesEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.trim().take(40).ifBlank { "Viewer" },
        "avatarKey" to avatarKey,
        "isKids" to isKids,
        "preferredGenres" to preferredGenres.take(10),
        "language" to language,
        "subtitlesEnabled" to subtitlesEnabled,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class WatchlistItem(
    @DocumentId val id: String = "",
    val contentId: String = "",
    val mediaType: String = "movie",
    val title: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "contentId" to contentId,
        "mediaType" to mediaType,
        "title" to title,
        "addedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class PlaybackProgress(
    @DocumentId val id: String = "",
    val contentId: String = "",
    val episodeId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    fun toWriteMap(): Map<String, Any> = buildMap {
        put("contentId", contentId)
        if (!episodeId.isNullOrBlank()) put("episodeId", episodeId)
        if (seasonNumber != null) put("seasonNumber", seasonNumber)
        if (episodeNumber != null) put("episodeNumber", episodeNumber)
        put("title", title)
        put("positionMs", positionMs.coerceAtLeast(0L))
        put("durationMs", durationMs.coerceAtLeast(0L))
        put("completed", completed)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class ContentRating(
    @DocumentId val id: String = "",
    val contentId: String = "",
    val liked: Int = 0, // -1 dislike, 0 neutral, 1 like
    val stars: Int = 0, // 0..5
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(): Map<String, Any> = mapOf(
        "contentId" to contentId,
        "liked" to liked.coerceIn(-1, 1),
        "stars" to stars.coerceIn(0, 5),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class SearchHistoryItem(
    @DocumentId val id: String = "",
    val query: String = "",
    @ServerTimestamp val searchedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "query" to query.trim().take(120),
        "searchedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val contentId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("title", title.take(120))
        put("message", message.take(500))
        if (!contentId.isNullOrBlank()) put("contentId", contentId)
        put("read", read)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class CatalogItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val synopsis: String = "",
    val mediaType: String = "movie", // "movie" | "series"
    val genres: List<String> = emptyList(),
    val releaseYear: Int = 2025,
    val runtimeMinutes: Int = 90,
    val ageRating: String = "PG-13",
    val director: String = "",
    val cast: List<String> = emptyList(),
    val posterKey: String = "img_poster_chronicle",
    val posterUrl: String? = null,
    val backdropKey: String = "img_hero_nebula",
    val backdropUrl: String? = null,
    val trailerUrl: String? = null,
    val videoUrl: String = "",
    val videoSourceType: String = "mp4", // "youtube" | "mp4" | "hls"
    val hlsUrl: String? = null,
    val isFeatured: Boolean = false,
    val isVisible: Boolean = true,
    val isKidsFriendly: Boolean = false,
    val trendingScore: Int = 80,
    val popularityRank: Int = 10,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val isSeries: Boolean get() = mediaType == "series"

    val preferredStreamUrl: String
        get() = when (videoSourceType.lowercase()) {
            "youtube" -> videoUrl
            "hls" -> hlsUrl?.takeIf { it.isNotBlank() } ?: videoUrl
            else -> videoUrl.ifBlank { hlsUrl.orEmpty() }
        }

    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        val normalizedType = com.example.util.VideoSourceType.fromKey(videoSourceType).key
        put("title", title.trim().take(200))
        put("synopsis", synopsis.trim().take(2000))
        put("mediaType", if (mediaType == "series") "series" else "movie")
        put("genres", genres.filter { it.isNotBlank() }.take(8).ifEmpty { listOf("Drama") })
        put("releaseYear", releaseYear.coerceIn(1900, 2100))
        put("runtimeMinutes", runtimeMinutes.coerceIn(1, 1000))
        put("ageRating", ageRating.ifBlank { "PG-13" }.take(20))
        put("director", director.trim().ifBlank { "Notflix Studios" }.take(120))
        put("cast", cast.filter { it.isNotBlank() }.take(12))
        put("posterKey", posterKey.ifBlank { "img_poster_chronicle" })
        if (!posterUrl.isNullOrBlank()) {
            put("posterUrl", posterUrl.trim())
        } else if (!isCreate) {
            put("posterUrl", FieldValue.delete())
        }
        put("backdropKey", backdropKey.ifBlank { "img_hero_nebula" })
        if (!backdropUrl.isNullOrBlank()) {
            put("backdropUrl", backdropUrl.trim())
        } else if (!isCreate) {
            put("backdropUrl", FieldValue.delete())
        }
        if (!trailerUrl.isNullOrBlank()) {
            put("trailerUrl", trailerUrl.trim())
        } else if (!isCreate) {
            put("trailerUrl", FieldValue.delete())
        }
        put("videoUrl", videoUrl.trim())
        put("videoSourceType", normalizedType)
        if (!hlsUrl.isNullOrBlank()) {
            put("hlsUrl", hlsUrl.trim())
        } else if (!isCreate) {
            put("hlsUrl", FieldValue.delete())
        }
        put("isFeatured", isFeatured)
        put("isVisible", isVisible)
        put("isKidsFriendly", isKidsFriendly)
        put("trendingScore", trendingScore.coerceIn(0, 100000))
        put("popularityRank", popularityRank.coerceIn(0, 10000))
        if (isCreate) {
            put("createdAt", FieldValue.serverTimestamp())
        }
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class Season(
    @DocumentId val id: String = "",
    val seasonNumber: Int = 1,
    val title: String = "Season 1",
    val year: Int = 2025,
    val isVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("seasonNumber", seasonNumber.coerceIn(1, 100))
        put("title", title.trim().ifBlank { "Season $seasonNumber" }.take(120))
        put("year", year.coerceIn(1900, 2100))
        put("isVisible", isVisible)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class Episode(
    @DocumentId val id: String = "",
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val title: String = "",
    val synopsis: String = "",
    val runtimeMinutes: Int = 42,
    val thumbnailKey: String = "img_hero_nebula",
    val thumbnailUrl: String? = null,
    val videoUrl: String = "",
    val videoSourceType: String = "mp4", // "youtube" | "mp4" | "hls"
    val hlsUrl: String? = null,
    val isVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val preferredStreamUrl: String
        get() = when (videoSourceType.lowercase()) {
            "youtube" -> videoUrl
            "hls" -> hlsUrl?.takeIf { it.isNotBlank() } ?: videoUrl
            else -> videoUrl.ifBlank { hlsUrl.orEmpty() }
        }

    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        val normalizedType = com.example.util.VideoSourceType.fromKey(videoSourceType).key
        put("seasonNumber", seasonNumber.coerceIn(1, 100))
        put("episodeNumber", episodeNumber.coerceIn(1, 1000))
        put("title", title.trim().take(200))
        put("synopsis", synopsis.trim().ifBlank { "Episode $episodeNumber" }.take(1500))
        put("runtimeMinutes", runtimeMinutes.coerceIn(1, 600))
        put("thumbnailKey", thumbnailKey.ifBlank { "img_hero_nebula" })
        if (!thumbnailUrl.isNullOrBlank()) {
            put("thumbnailUrl", thumbnailUrl.trim())
        } else if (!isCreate) {
            put("thumbnailUrl", FieldValue.delete())
        }
        put("videoUrl", videoUrl.trim())
        put("videoSourceType", normalizedType)
        if (!hlsUrl.isNullOrBlank()) {
            put("hlsUrl", hlsUrl.trim())
        } else if (!isCreate) {
            put("hlsUrl", FieldValue.delete())
        }
        put("isVisible", isVisible)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class SeasonDraft(
    val season: Season,
    val episodes: List<Episode>
)

@IgnoreExtraProperties
data class Genre(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("name", name.trim().take(60))
        put("slug", slug.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "genre" }.take(60))
        put("displayOrder", displayOrder.coerceIn(0, 1000))
        put("isActive", isActive)
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
