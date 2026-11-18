package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * 4-tier platform role hierarchy:
 * OWNER > ADMIN > PUBLISHER > VIEWER (backward-compatible with legacy "member").
 */
enum class AppRole(val key: String, val label: String) {
    OWNER("owner", "Owner"),
    ADMIN("admin", "Admin"),
    PUBLISHER("publisher", "Publisher/Distributor"),
    VIEWER("viewer", "Viewer");

    val isOwner: Boolean get() = this == OWNER
    val isAdminOrOwner: Boolean get() = this == OWNER || this == ADMIN
    val canManageAdmins: Boolean get() = this == OWNER
    val canModerateContent: Boolean get() = this == OWNER || this == ADMIN
    val canManageSubscriptions: Boolean get() = this == OWNER || this == ADMIN
    val canManagePublisherRoles: Boolean get() = this == OWNER || this == ADMIN

    companion object {
        fun fromKey(raw: String?): AppRole = when (raw?.trim()?.lowercase()) {
            "owner" -> OWNER
            "admin" -> ADMIN
            "publisher" -> PUBLISHER
            "viewer", "member", "", null -> VIEWER
            else -> VIEWER
        }
    }
}

enum class PublisherStatus(val key: String, val label: String) {
    NONE("none", "Not Applied"),
    PENDING("pending", "Pending"),
    UNDER_REVIEW("under_review", "Under Review"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected"),
    SUSPENDED("suspended", "Suspended"),
    REVOKED("revoked", "Revoked");

    companion object {
        fun fromKey(raw: String?): PublisherStatus = when (raw?.trim()?.lowercase()) {
            "pending" -> PENDING
            "under_review" -> UNDER_REVIEW
            "approved" -> APPROVED
            "rejected" -> REJECTED
            "suspended" -> SUSPENDED
            "revoked" -> REVOKED
            "not_applied", "none", "", null -> NONE
            else -> NONE
        }
    }
}

enum class ContentReviewStatus(val key: String, val label: String) {
    PENDING_REVIEW("pending_review", "Pending Review"),
    UNDER_REVIEW("under_review", "Under Review"),
    APPROVED("approved", "Approved"),
    CHANGES_REQUESTED("changes_requested", "Changes Requested"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun fromKey(raw: String?): ContentReviewStatus = when (raw?.trim()?.lowercase()) {
            "pending_review" -> PENDING_REVIEW
            "under_review" -> UNDER_REVIEW
            "approved" -> APPROVED
            "changes_requested" -> CHANGES_REQUESTED
            "rejected" -> REJECTED
            else -> PENDING_REVIEW
        }
    }
}

object GovernanceConstants {
    const val PUBLISHER_APPLICATION_FORM_URL = "https://forms.gle/Bj6NsHnmAxrumCL66"
    const val PUBLISHER_APPLICATION_DISCLAIMER =
        "Submitting an application does not guarantee approval. Notflix will review your application and contact you using the email address provided with its decision. If approved as a Publisher/Distributor, all submitted movies and television shows must undergo individual content review before becoming publicly available on Notflix. Approval as a Publisher does not automatically approve submitted content."
}

@IgnoreExtraProperties
data class OwnerRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val establishedBy: String = "bootstrap",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "email" to email,
        "establishedBy" to establishedBy,
        "createdAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class PublisherApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val publisherName: String = "",
    val contactEmail: String = "",
    val organizationDescription: String = "",
    val intendedContentDetails: String = "",
    val status: String = PublisherStatus.NONE.key, // none | pending | under_review | approved | rejected
    val reviewerNotes: String = "",
    val reviewedByUid: String? = null,
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val parsedStatus: PublisherStatus get() = PublisherStatus.fromKey(status)

    fun toApplicantWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("userId", userId)
        put("publisherName", publisherName.trim().take(120))
        put("contactEmail", contactEmail.trim().take(200))
        put("organizationDescription", organizationDescription.trim().take(1000))
        put("intendedContentDetails", intendedContentDetails.trim().take(1000))
        put("status", PublisherStatus.PENDING.key)
        put("reviewerNotes", "")
        if (isCreate) put("submittedAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class SubmissionEpisodeDraft(
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val title: String = "",
    val synopsis: String = "",
    val runtimeMinutes: Int = 42,
    val thumbnailUrl: String? = null,
    val videoUrl: String = "",
    val videoSourceType: String = "auto",
    val providerName: String? = null,
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("seasonNumber", seasonNumber.coerceIn(1, 100))
        put("episodeNumber", episodeNumber.coerceIn(1, 1000))
        put("title", title.trim().ifBlank { "Episode $episodeNumber" }.take(200))
        put("synopsis", synopsis.trim().ifBlank { "Episode $episodeNumber" }.take(1500))
        put("runtimeMinutes", runtimeMinutes.coerceIn(1, 600))
        if (!thumbnailUrl.isNullOrBlank()) put("thumbnailUrl", thumbnailUrl.trim().take(1000))
        put("videoUrl", videoUrl.trim().take(1000))
        put("videoSourceType", com.example.util.VideoSourceUtils.normalizeFirestoreSourceType(videoSourceType))
        if (!providerName.isNullOrBlank()) put("providerName", providerName.trim().take(80))
    }
}

data class SubmissionSeasonDraft(
    val seasonNumber: Int = 1,
    val title: String = "Season 1",
    val year: Int = 2025,
    val episodes: List<SubmissionEpisodeDraft> = emptyList(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "seasonNumber" to seasonNumber.coerceIn(1, 100),
        "title" to title.trim().ifBlank { "Season $seasonNumber" }.take(120),
        "year" to year.coerceIn(1900, 2100),
        "episodes" to episodes.take(100).map { it.toMap() },
    )
}

@IgnoreExtraProperties
data class ContentSubmission(
    @DocumentId val id: String = "",
    val publisherUid: String = "",
    val publisherName: String = "",
    val mediaType: String = "movie", // movie | series | documentary | short_film
    val title: String = "",
    val synopsis: String = "",
    val genres: List<String> = emptyList(),
    val releaseYear: Int = 2025,
    val runtimeMinutes: Int = 90,
    val ageRating: String = "PG-13",
    val directorOrCreator: String = "",
    val cast: List<String> = emptyList(),
    val posterKey: String = "img_poster_chronicle",
    val posterUrl: String? = null,
    val backdropKey: String = "img_hero_nebula",
    val backdropUrl: String? = null,
    val trailerUrl: String? = null,
    val videoUrl: String = "",
    val videoSourceType: String = "auto",
    val providerName: String? = null,
    val hlsUrl: String? = null,
    val isKidsFriendly: Boolean = false,
    val tags: List<String> = emptyList(),
    val seasons: List<SubmissionSeasonDraft> = emptyList(),
    val distributionAuthorized: Boolean = false,
    val distributionRightsStatement: String = "",
    val reviewStatus: String = ContentReviewStatus.PENDING_REVIEW.key,
    val privateReviewerNotes: String = "",
    val lastReviewedByUid: String? = null,
    val lastReviewedAt: Timestamp? = null,
    val publishedCatalogId: String? = null,
    val isRevision: Boolean = false,
    val revisionNumber: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val parsedStatus: ContentReviewStatus get() = ContentReviewStatus.fromKey(reviewStatus)
    val isSeriesSubmission: Boolean get() = mediaType == "series"
    val canPublisherEditInPlace: Boolean
        get() = parsedStatus == ContentReviewStatus.PENDING_REVIEW ||
            parsedStatus == ContentReviewStatus.CHANGES_REQUESTED

    fun toPublisherWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        val normalizedType = com.example.util.VideoSourceUtils.normalizeFirestoreSourceType(videoSourceType)
        val normalizedMedia = when (mediaType.lowercase()) {
            "series", "documentary", "short_film" -> mediaType.lowercase()
            else -> "movie"
        }
        put("publisherUid", publisherUid)
        put("publisherName", publisherName.trim().ifBlank { "Publisher" }.take(120))
        put("mediaType", normalizedMedia)
        put("title", title.trim().take(200))
        put("synopsis", synopsis.trim().take(2000))
        put("genres", genres.filter { it.isNotBlank() }.take(8).ifEmpty { listOf("Drama") })
        put("releaseYear", releaseYear.coerceIn(1900, 2100))
        put("runtimeMinutes", runtimeMinutes.coerceIn(1, 1000))
        put("ageRating", ageRating.trim().ifBlank { "PG-13" }.take(20))
        put("directorOrCreator", directorOrCreator.trim().ifBlank { publisherName.ifBlank { "Independent Creator" } }.take(120))
        put("cast", cast.filter { it.isNotBlank() }.take(12))
        put("posterKey", posterKey.ifBlank { "img_poster_chronicle" }.take(128))
        if (!posterUrl.isNullOrBlank()) put("posterUrl", posterUrl.trim().take(1000))
        put("backdropKey", backdropKey.ifBlank { "img_hero_nebula" }.take(128))
        if (!backdropUrl.isNullOrBlank()) put("backdropUrl", backdropUrl.trim().take(1000))
        if (!trailerUrl.isNullOrBlank()) put("trailerUrl", trailerUrl.trim().take(1000))
        put("videoUrl", videoUrl.trim().take(1000))
        put("videoSourceType", normalizedType)
        if (!providerName.isNullOrBlank()) put("providerName", providerName.trim().take(80))
        if (!hlsUrl.isNullOrBlank()) put("hlsUrl", hlsUrl.trim().take(1000))
        put("isKidsFriendly", isKidsFriendly)
        put("tags", tags.map { it.trim() }.filter { it.isNotBlank() }.take(12))
        put("seasons", seasons.take(30).map { it.toMap() })
        put("distributionAuthorized", distributionAuthorized)
        put("distributionRightsStatement", distributionRightsStatement.trim().take(500))
        put("reviewStatus", ContentReviewStatus.PENDING_REVIEW.key)
        if (isCreate) {
            put("privateReviewerNotes", "")
            put("isRevision", isRevision)
            put("revisionNumber", revisionNumber.coerceAtLeast(1))
            if (!publishedCatalogId.isNullOrBlank()) put("publishedCatalogId", publishedCatalogId)
            put("createdAt", FieldValue.serverTimestamp())
        }
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class ContentReviewDecision(
    @DocumentId val id: String = "",
    val submissionId: String = "",
    val reviewerUid: String = "",
    val reviewerRole: String = "admin",
    val decision: String = ContentReviewStatus.UNDER_REVIEW.key,
    val notes: String = "",
    val videoSourceVerified: Boolean = false,
    val rightsConfirmed: Boolean = false,
    val metadataVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "submissionId" to submissionId,
        "reviewerUid" to reviewerUid,
        "reviewerRole" to reviewerRole,
        "decision" to decision,
        "notes" to notes.trim().take(2000),
        "videoSourceVerified" to videoSourceVerified,
        "rightsConfirmed" to rightsConfirmed,
        "metadataVerified" to metadataVerified,
        "createdAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class PlatformSettings(
    @DocumentId val id: String = "global",
    val freeAccessEnabled: Boolean = true,
    val freeAccessNotice: String = "Platform-wide Free Access is currently enabled. Existing subscription records are preserved.",
    val updatedByUid: String = "system",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(actorUid: String): Map<String, Any> = mapOf(
        "freeAccessEnabled" to freeAccessEnabled,
        "freeAccessNotice" to freeAccessNotice.trim().take(400),
        "updatedByUid" to actorUid,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorRole: String = "admin",
    val action: String = "",
    val targetUid: String? = null,
    val targetContentId: String? = null,
    val targetSubscriptionId: String? = null,
    val previousValue: String = "",
    val newValue: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("actorUid", actorUid)
        put("actorRole", actorRole)
        put("action", action.trim().take(120))
        if (!targetUid.isNullOrBlank()) put("targetUid", targetUid.take(128))
        if (!targetContentId.isNullOrBlank()) put("targetContentId", targetContentId.take(128))
        if (!targetSubscriptionId.isNullOrBlank()) put("targetSubscriptionId", targetSubscriptionId.take(128))
        put("previousValue", previousValue.take(500))
        put("newValue", newValue.take(500))
        put("details", details.take(1000))
        put("createdAt", FieldValue.serverTimestamp())
    }
}

/**
 * Pure role governance helper for UI and repository validation (mirrored strictly in Firestore rules).
 */
object RoleGovernance {
    fun resolveEffectiveRole(
        accountRole: String?,
        hasOwnerRecord: Boolean,
        hasAdminRecord: Boolean,
        isBootstrappedOwner: Boolean = false
    ): AppRole {
        if (hasOwnerRecord || isBootstrappedOwner || AppRole.fromKey(accountRole) == AppRole.OWNER) {
            return AppRole.OWNER
        }
        if (hasAdminRecord || AppRole.fromKey(accountRole) == AppRole.ADMIN) {
            return AppRole.ADMIN
        }
        return AppRole.fromKey(accountRole)
    }

    fun canActorChangeRole(
        actorRole: AppRole,
        targetCurrentRole: AppRole,
        desiredRole: AppRole
    ): Boolean {
        if (targetCurrentRole == AppRole.OWNER) return false
        if (desiredRole == AppRole.OWNER) return false
        return when (actorRole) {
            AppRole.OWNER -> true
            AppRole.ADMIN -> {
                targetCurrentRole != AppRole.ADMIN &&
                    desiredRole != AppRole.ADMIN
            }
            else -> false
        }
    }

    fun canActorSuspendTarget(actorRole: AppRole, targetCurrentRole: AppRole): Boolean {
        if (targetCurrentRole == AppRole.OWNER) return false
        return when (actorRole) {
            AppRole.OWNER -> true
            AppRole.ADMIN -> targetCurrentRole != AppRole.ADMIN
            else -> false
        }
    }

    fun isApprovedPublisherAccess(
        effectiveRole: AppRole,
        publisherStatus: PublisherStatus
    ): Boolean {
        if (effectiveRole.isAdminOrOwner) return true
        return effectiveRole == AppRole.PUBLISHER && publisherStatus == PublisherStatus.APPROVED
    }
}
