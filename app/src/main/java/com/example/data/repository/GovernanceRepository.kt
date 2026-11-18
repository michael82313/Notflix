package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AppRole
import com.example.data.model.AuditLogEntry
import com.example.data.model.CatalogItem
import com.example.data.model.ContentReviewDecision
import com.example.data.model.ContentReviewStatus
import com.example.data.model.ContentSubmission
import com.example.data.model.Episode
import com.example.data.model.PlatformSettings
import com.example.data.model.PublisherApplication
import com.example.data.model.PublisherStatus
import com.example.data.model.RoleGovernance
import com.example.data.model.Season
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.remote.FirestoreDocumentMapper
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Governance, publisher onboarding, content moderation, role administration,
 * platform settings, and immutable audit logging repository.
 */
class GovernanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private companion object {
        const val TAG = "GovernanceRepository"
    }

    private fun requireActorUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    // --- Publisher Applications ---

    fun observeMyPublisherApplication(userId: String): Flow<PublisherApplication?> {
        val docRef = db.collection("publisherApplications").document(userId)
        return docRef.snapshots()
            .map { snapshot -> FirestoreDocumentMapper.toPublisherApplication(snapshot) }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(null)
            }
    }

    fun observeAllPublisherApplications(): Flow<List<PublisherApplication>> {
        val colRef = db.collection("publisherApplications")
        return colRef.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toPublisherApplications(snapshot)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.submittedAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(unwrapFirestore(error) ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    /**
     * Records a viewer's Publisher/Distributor application status as PENDING.
     * Never grants publisher privileges automatically.
     */
    suspend fun submitPublisherApplicationTracker(
        publisherName: String,
        contactEmail: String,
        organizationDescription: String,
        intendedContentDetails: String
    ): Result<Unit> = runCatching {
        val uid = requireActorUid()
        val docRef = db.collection("publisherApplications").document(uid)
        val existing = docRef.get().await()
        val app = PublisherApplication(
            id = uid,
            userId = uid,
            publisherName = publisherName.trim().ifBlank { "Independent Publisher" },
            contactEmail = contactEmail.trim().ifBlank { auth.currentUser?.email.orEmpty() },
            organizationDescription = organizationDescription.trim(),
            intendedContentDetails = intendedContentDetails.trim(),
            status = PublisherStatus.PENDING.key
        )
        if (existing.exists()) {
            val currentStatus = PublisherStatus.fromKey(existing.getString("status"))
            require(currentStatus == PublisherStatus.NONE || currentStatus == PublisherStatus.REJECTED) {
                "Your application is currently ${currentStatus.label}."
            }
            docRef.update(
                mapOf(
                    "publisherName" to app.publisherName.take(120),
                    "contactEmail" to app.contactEmail.take(200),
                    "organizationDescription" to app.organizationDescription.take(1000),
                    "intendedContentDetails" to app.intendedContentDetails.take(1000),
                    "status" to PublisherStatus.PENDING.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(app.toApplicantWriteMap(isCreate = true)).await()
        }
    }

    suspend fun reviewPublisherApplication(
        application: PublisherApplication,
        newStatus: PublisherStatus,
        reviewerNotes: String,
        actorRole: AppRole
    ): Result<Unit> = runCatching {
        require(actorRole.canManagePublisherRoles) { "Only Admins or Owner can review publisher applications." }
        val actorUid = requireActorUid()
        val targetUid = application.userId.ifBlank { application.id }
        require(targetUid.isNotBlank()) { "Applicant user ID missing." }

        val appRef = db.collection("publisherApplications").document(targetUid)
        val previousStatus = application.status
        appRef.update(
            mapOf(
                "status" to newStatus.key,
                "reviewerNotes" to reviewerNotes.trim().take(2000),
                "reviewedByUid" to actorUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Update user account publisher status (and role to publisher on approval if ordinary viewer)
        val userRef = db.collection("users").document(targetUid)
        val userSnap = userRef.get().await()
        if (userSnap.exists()) {
            val targetAccount = FirestoreDocumentMapper.toUserAccount(userSnap)
            val targetRole = targetAccount?.parsedRole ?: AppRole.VIEWER
            if (targetRole != AppRole.OWNER && (actorRole.isOwner || targetRole != AppRole.ADMIN)) {
                val nextRole = when {
                    newStatus == PublisherStatus.APPROVED && targetRole == AppRole.VIEWER -> AppRole.PUBLISHER.key
                    newStatus == PublisherStatus.REVOKED && targetRole == AppRole.PUBLISHER -> AppRole.VIEWER.key
                    else -> targetAccount?.role ?: AppRole.VIEWER.key
                }
                val updates = mutableMapOf<String, Any>(
                    "role" to nextRole,
                    "publisherStatus" to newStatus.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (application.publisherName.isNotBlank()) {
                    updates["publisherName"] = application.publisherName.take(120)
                }
                userRef.update(updates).await()
            }
        }

        // Notify applicant privately
        sendPrivateNotification(
            targetUid = targetUid,
            title = "Publisher Application: ${newStatus.label}",
            message = reviewerNotes.ifBlank {
                when (newStatus) {
                    PublisherStatus.APPROVED ->
                        "Your Publisher/Distributor application was approved. You can now submit titles for individual content review."
                    PublisherStatus.UNDER_REVIEW ->
                        "Your Publisher/Distributor application is currently under review by the Notflix team."
                    PublisherStatus.REJECTED ->
                        "Your Publisher/Distributor application was not approved at this time."
                    else -> "Your Publisher/Distributor application status updated to ${newStatus.label}."
                }
            }
        )

        writeAuditLog(
            actorUid = actorUid,
            actorRole = actorRole,
            action = "REVIEW_PUBLISHER_APPLICATION",
            targetUid = targetUid,
            previousValue = previousStatus,
            newValue = newStatus.key,
            details = reviewerNotes.ifBlank { "Publisher application updated to ${newStatus.label}" }
        )
    }

    // --- Content Submissions & Moderation ---

    fun observeMySubmissions(publisherUid: String): Flow<List<ContentSubmission>> {
        val query = db.collection("contentSubmissions").whereEqualTo("publisherUid", publisherUid)
        return query.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toContentSubmissions(snapshot)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(emptyList())
            }
    }

    fun observeAllSubmissionsForModeration(): Flow<List<ContentSubmission>> {
        val colRef = db.collection("contentSubmissions")
        return colRef.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toContentSubmissions(snapshot)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(unwrapFirestore(error) ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observeReviewHistory(submissionId: String): Flow<List<ContentReviewDecision>> {
        val colRef = db.collection("contentSubmissions").document(submissionId).collection("reviews")
        return colRef.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toContentReviewDecisions(snapshot)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(emptyList())
            }
    }

    suspend fun submitNewContent(submission: ContentSubmission): Result<String> = runCatching {
        val uid = requireActorUid()
        require(submission.distributionAuthorized) {
            "You must confirm you own or have legal authorization to distribute this content on Notflix."
        }
        require(submission.title.isNotBlank()) { "Title is required." }
        require(submission.synopsis.isNotBlank()) { "Description/synopsis is required." }

        val slug = submission.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).ifBlank { "sub" }
        val docId = "${slug}-${UUID.randomUUID().toString().take(8)}"
        val payload = submission.copy(
            id = docId,
            publisherUid = uid,
            reviewStatus = ContentReviewStatus.PENDING_REVIEW.key,
            privateReviewerNotes = "",
            lastReviewedByUid = null,
            lastReviewedAt = null
        ).toPublisherWriteMap(isCreate = true)

        val docRef = db.collection("contentSubmissions").document(docId)
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docId
    }

    suspend fun updateOwnSubmission(submission: ContentSubmission): Result<Unit> = runCatching {
        val uid = requireActorUid()
        require(submission.id.isNotBlank()) { "Submission ID is required." }
        require(submission.publisherUid == uid) { "You can only edit your own submissions." }
        require(submission.canPublisherEditInPlace) {
            "Approved titles cannot be edited in-place. Submit a revision for review instead."
        }
        require(submission.distributionAuthorized) {
            "Distribution authorization confirmation is required."
        }

        val docRef = db.collection("contentSubmissions").document(submission.id)
        val payload = submission.copy(
            publisherUid = uid,
            reviewStatus = ContentReviewStatus.PENDING_REVIEW.key
        ).toPublisherWriteMap(isCreate = false)
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Submitting edits to an already-approved title creates a new PENDING revision record
     * linked to [original.publishedCatalogId], keeping the live public catalog completely intact
     * until an Admin or Owner explicitly approves the revision.
     */
    suspend fun submitRevisionForApprovedContent(
        original: ContentSubmission,
        revised: ContentSubmission
    ): Result<String> = runCatching {
        val uid = requireActorUid()
        require(original.publisherUid == uid) { "You can only submit revisions for your own titles." }
        require(revised.distributionAuthorized) {
            "Distribution authorization confirmation is required."
        }
        val slug = revised.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(36).ifBlank { "rev" }
        val revId = "${slug}-rev-${UUID.randomUUID().toString().take(8)}"
        val payload = revised.copy(
            id = revId,
            publisherUid = uid,
            reviewStatus = ContentReviewStatus.PENDING_REVIEW.key,
            privateReviewerNotes = "",
            lastReviewedByUid = null,
            lastReviewedAt = null,
            publishedCatalogId = original.publishedCatalogId ?: "pub-${original.id}",
            isRevision = true,
            revisionNumber = original.revisionNumber + 1
        ).toPublisherWriteMap(isCreate = true)

        db.collection("contentSubmissions").document(revId).set(payload).await()
        revId
    }

    /**
     * Admin/Owner moderation decision.
     * - APPROVED: atomically publishes (or updates existing catalog entry on revision) to /catalog
     *   including seasons & episodes for TV shows.
     * - CHANGES_REQUESTED / REJECTED / UNDER_REVIEW: keeps submission completely hidden from viewers.
     */
    suspend fun reviewContentSubmission(
        submission: ContentSubmission,
        decision: ContentReviewStatus,
        privateNotes: String,
        videoSourceVerified: Boolean,
        rightsConfirmed: Boolean,
        metadataVerified: Boolean,
        actorRole: AppRole
    ): Result<String?> = runCatching {
        require(actorRole.canModerateContent) { "Only Admins or Owner can moderate content." }
        val actorUid = requireActorUid()
        val subId = submission.id
        require(subId.isNotBlank()) { "Submission ID is required." }

        val previousStatus = submission.reviewStatus
        var publishedCatalogId = submission.publishedCatalogId

        if (decision == ContentReviewStatus.APPROVED) {
            val catalogId = publishedCatalogId?.takeIf { it.isNotBlank() } ?: "pub-$subId"
            publishedCatalogId = catalogId
            publishSubmissionToCatalog(submission, catalogId)
        }

        val subRef = db.collection("contentSubmissions").document(subId)
        val subUpdates = mutableMapOf<String, Any?>(
            "reviewStatus" to decision.key,
            "privateReviewerNotes" to privateNotes.trim().take(2000),
            "lastReviewedByUid" to actorUid,
            "lastReviewedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!publishedCatalogId.isNullOrBlank()) {
            subUpdates["publishedCatalogId"] = publishedCatalogId
        }
        subRef.update(subUpdates).await()

        val reviewId = "rev_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val decisionDoc = ContentReviewDecision(
            id = reviewId,
            submissionId = subId,
            reviewerUid = actorUid,
            reviewerRole = if (actorRole.isOwner) AppRole.OWNER.key else AppRole.ADMIN.key,
            decision = decision.key,
            notes = privateNotes.trim(),
            videoSourceVerified = videoSourceVerified,
            rightsConfirmed = rightsConfirmed,
            metadataVerified = metadataVerified
        )
        subRef.collection("reviews").document(reviewId).set(decisionDoc.toCreateMap()).await()

        // Send private notification to the publisher
        sendPrivateNotification(
            targetUid = submission.publisherUid,
            title = "Submission ${decision.label}: ${submission.title}",
            message = privateNotes.ifBlank {
                when (decision) {
                    ContentReviewStatus.APPROVED -> "\"${submission.title}\" has been approved and published to Notflix."
                    ContentReviewStatus.CHANGES_REQUESTED -> "Reviewers requested changes on \"${submission.title}\". Open your Publisher Dashboard to inspect notes and resubmit."
                    ContentReviewStatus.REJECTED -> "\"${submission.title}\" was not approved during content review."
                    else -> "\"${submission.title}\" is now ${decision.label}."
                }
            },
            contentId = if (decision == ContentReviewStatus.APPROVED) publishedCatalogId else null
        )

        writeAuditLog(
            actorUid = actorUid,
            actorRole = actorRole,
            action = "CONTENT_REVIEW_${decision.key.uppercase()}",
            targetUid = submission.publisherUid,
            targetContentId = publishedCatalogId ?: subId,
            previousValue = previousStatus,
            newValue = decision.key,
            details = privateNotes.ifBlank { "${submission.title} (${submission.mediaType}) -> ${decision.label}" }
        )

        publishedCatalogId
    }

    private suspend fun publishSubmissionToCatalog(submission: ContentSubmission, catalogId: String) {
        val catalogRef = db.collection("catalog").document(catalogId)
        val existingSnap = catalogRef.get().await()
        val existingItem = if (existingSnap.exists()) FirestoreDocumentMapper.toCatalogItem(existingSnap) else null

        val normalizedCatalogMediaType = if (submission.mediaType == "series") "series" else "movie"
        val catalogItem = CatalogItem(
            id = catalogId,
            title = submission.title,
            synopsis = submission.synopsis,
            mediaType = normalizedCatalogMediaType,
            genres = submission.genres.ifEmpty { listOf("Drama") },
            releaseYear = submission.releaseYear,
            runtimeMinutes = submission.runtimeMinutes,
            ageRating = submission.ageRating,
            director = submission.directorOrCreator.ifBlank { submission.publisherName.ifBlank { "Independent Creator" } },
            cast = submission.cast,
            posterKey = submission.posterKey.ifBlank { "img_poster_chronicle" },
            posterUrl = submission.posterUrl,
            backdropKey = submission.backdropKey.ifBlank { "img_hero_nebula" },
            backdropUrl = submission.backdropUrl,
            trailerUrl = submission.trailerUrl,
            videoUrl = submission.videoUrl,
            videoSourceType = submission.videoSourceType,
            providerName = submission.providerName,
            hlsUrl = submission.hlsUrl,
            isFeatured = existingItem?.isFeatured ?: false,
            isVisible = true,
            isKidsFriendly = submission.isKidsFriendly,
            trendingScore = existingItem?.trendingScore ?: 82,
            popularityRank = existingItem?.popularityRank ?: 15,
            tags = (submission.tags + listOfNotNull(
                when (submission.mediaType) {
                    "documentary" -> "Documentary"
                    "short_film" -> "Short Film"
                    else -> null
                }
            )).distinct(),
            keywords = existingItem?.keywords ?: emptyList(),
            isStaffPick = existingItem?.isStaffPick ?: false,
            isAwardWinning = existingItem?.isAwardWinning ?: false,
            isComingSoon = false,
            publisherUid = submission.publisherUid,
            publisherName = submission.publisherName,
            sourceSubmissionId = submission.id,
            contentCategory = submission.mediaType
        )

        if (existingSnap.exists()) {
            catalogRef.update(catalogItem.toWriteMap(isCreate = false)).await()
        } else {
            catalogRef.set(catalogItem.toWriteMap(isCreate = true)).await()
        }

        if (normalizedCatalogMediaType == "series" && submission.seasons.isNotEmpty()) {
            submission.seasons.forEach { seasonDraft ->
                val seasonId = "season-${seasonDraft.seasonNumber}"
                val seasonRef = catalogRef.collection("seasons").document(seasonId)
                val seasonSnap = seasonRef.get().await()
                val seasonModel = Season(
                    id = seasonId,
                    seasonNumber = seasonDraft.seasonNumber,
                    title = seasonDraft.title.ifBlank { "Season ${seasonDraft.seasonNumber}" },
                    year = seasonDraft.year,
                    isVisible = true
                )
                if (seasonSnap.exists()) {
                    seasonRef.update(seasonModel.toWriteMap(isCreate = false)).await()
                } else {
                    seasonRef.set(seasonModel.toWriteMap(isCreate = true)).await()
                }

                seasonDraft.episodes.forEach { epDraft ->
                    val epId = "s${seasonDraft.seasonNumber}e${epDraft.episodeNumber}"
                    val epRef = seasonRef.collection("episodes").document(epId)
                    val epSnap = epRef.get().await()
                    val epModel = Episode(
                        id = epId,
                        seasonNumber = seasonDraft.seasonNumber,
                        episodeNumber = epDraft.episodeNumber,
                        title = epDraft.title.ifBlank { "Episode ${epDraft.episodeNumber}" },
                        synopsis = epDraft.synopsis.ifBlank { "Episode ${epDraft.episodeNumber}" },
                        runtimeMinutes = epDraft.runtimeMinutes,
                        thumbnailKey = "img_hero_nebula",
                        thumbnailUrl = epDraft.thumbnailUrl,
                        videoUrl = epDraft.videoUrl,
                        videoSourceType = epDraft.videoSourceType,
                        providerName = epDraft.providerName,
                        isVisible = true
                    )
                    if (epSnap.exists()) {
                        epRef.update(epModel.toWriteMap(isCreate = false)).await()
                    } else {
                        epRef.set(epModel.toWriteMap(isCreate = true)).await()
                    }
                }
            }
        }
    }

    // --- Role Management with Absolute Owner Protection ---

    suspend fun assignUserRole(
        target: UserAccount,
        desiredRole: AppRole,
        desiredPublisherStatus: PublisherStatus? = null,
        actorRole: AppRole
    ): Result<Unit> = runCatching {
        val actorUid = requireActorUid()
        val targetUid = target.userId.ifBlank { target.id }
        require(targetUid.isNotBlank()) { "Target user ID is missing." }

        // Check trusted /owners/{uid} & /admins/{uid} before mutating
        val isTargetOwner = db.collection("owners").document(targetUid).get().await().exists() ||
            target.parsedRole == AppRole.OWNER
        require(!isTargetOwner) { "Platform Owner account is strictly protected and cannot be modified." }

        val hasAdminDoc = db.collection("admins").document(targetUid).get().await().exists()
        val targetCurrentRole = RoleGovernance.resolveEffectiveRole(
            accountRole = target.role,
            hasOwnerRecord = false,
            hasAdminRecord = hasAdminDoc
        )
        require(RoleGovernance.canActorChangeRole(actorRole, targetCurrentRole, desiredRole)) {
            if (desiredRole == AppRole.ADMIN || targetCurrentRole == AppRole.ADMIN) {
                "Only the Platform Owner can appoint or remove Administrators."
            } else {
                "You do not have permission to change this user's role."
            }
        }

        val resolvedPubStatus = desiredPublisherStatus ?: when (desiredRole) {
            AppRole.PUBLISHER -> PublisherStatus.APPROVED
            else -> target.parsedPublisherStatus
        }

        val userRef = db.collection("users").document(targetUid)
        userRef.update(
            mapOf(
                "role" to desiredRole.key,
                "publisherStatus" to resolvedPubStatus.key,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Owner-only /admins/{uid} registry sync
        val adminRef = db.collection("admins").document(targetUid)
        if (desiredRole == AppRole.ADMIN) {
            adminRef.set(
                mapOf(
                    "uid" to targetUid,
                    "email" to target.email.ifBlank { "admin@notflix.local" },
                    "grantedBy" to actorUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else if (hasAdminDoc && actorRole.isOwner) {
            adminRef.delete().await()
        }

        val actionName = when {
            desiredRole == AppRole.ADMIN -> "APPOINT_ADMIN"
            targetCurrentRole == AppRole.ADMIN -> "REMOVE_ADMIN"
            desiredRole == AppRole.PUBLISHER -> "ASSIGN_PUBLISHER_ROLE"
            targetCurrentRole == AppRole.PUBLISHER -> "REVOKE_PUBLISHER_ROLE"
            else -> "UPDATE_USER_ROLE"
        }

        writeAuditLog(
            actorUid = actorUid,
            actorRole = actorRole,
            action = actionName,
            targetUid = targetUid,
            previousValue = "${targetCurrentRole.key} (publisherStatus=${target.publisherStatus})",
            newValue = "${desiredRole.key} (publisherStatus=${resolvedPubStatus.key})",
            details = "Updated ${target.displayName.ifBlank { target.email }} role."
        )
    }

    suspend fun updatePublisherStatus(
        target: UserAccount,
        newStatus: PublisherStatus,
        actorRole: AppRole
    ): Result<Unit> = runCatching {
        require(actorRole.canManagePublisherRoles) { "Admin or Owner privileges required." }
        val actorUid = requireActorUid()
        val targetUid = target.userId.ifBlank { target.id }
        require(targetUid.isNotBlank()) { "Target user ID missing." }

        val isTargetOwner = db.collection("owners").document(targetUid).get().await().exists() ||
            target.parsedRole == AppRole.OWNER
        require(!isTargetOwner) { "Platform Owner account is protected." }

        val targetRole = target.parsedRole
        require(actorRole.isOwner || targetRole != AppRole.ADMIN) {
            "Only the Platform Owner can modify an Administrator account."
        }

        val nextRole = when (newStatus) {
            PublisherStatus.APPROVED -> if (targetRole == AppRole.VIEWER) AppRole.PUBLISHER.key else target.role
            PublisherStatus.REVOKED -> if (targetRole == AppRole.PUBLISHER) AppRole.VIEWER.key else target.role
            else -> target.role
        }

        db.collection("users").document(targetUid).update(
            mapOf(
                "role" to nextRole,
                "publisherStatus" to newStatus.key,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        writeAuditLog(
            actorUid = actorUid,
            actorRole = actorRole,
            action = "UPDATE_PUBLISHER_STATUS",
            targetUid = targetUid,
            previousValue = "${target.role}/${target.publisherStatus}",
            newValue = "$nextRole/${newStatus.key}",
            details = "Publisher status changed to ${newStatus.label} for ${target.displayName.ifBlank { targetUid }}."
        )
    }

    suspend fun setUserSuspendedWithAudit(
        target: UserAccount,
        suspended: Boolean,
        actorRole: AppRole
    ): Result<Unit> = runCatching {
        val actorUid = requireActorUid()
        val targetUid = target.userId.ifBlank { target.id }
        require(targetUid.isNotBlank()) { "Target user ID missing." }

        val isTargetOwner = db.collection("owners").document(targetUid).get().await().exists() ||
            target.parsedRole == AppRole.OWNER
        require(!isTargetOwner) { "Platform Owner account cannot be suspended." }

        val hasAdminDoc = db.collection("admins").document(targetUid).get().await().exists()
        val targetRole = RoleGovernance.resolveEffectiveRole(target.role, false, hasAdminDoc)
        require(RoleGovernance.canActorSuspendTarget(actorRole, targetRole)) {
            "Only the Platform Owner can suspend or restore an Administrator."
        }

        db.collection("users").document(targetUid).update(
            mapOf(
                "isSuspended" to suspended,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        writeAuditLog(
            actorUid = actorUid,
            actorRole = actorRole,
            action = if (suspended) "SUSPEND_USER" else "RESTORE_USER",
            targetUid = targetUid,
            previousValue = "isSuspended=${target.isSuspended}",
            newValue = "isSuspended=$suspended",
            details = "${if (suspended) "Suspended" else "Restored"} ${target.displayName.ifBlank { target.email }}."
        )
    }

    // --- Platform Settings (Free Access Toggle) ---

    fun observePlatformSettings(): Flow<PlatformSettings> {
        val docRef = db.collection("platformSettings").document("global")
        return docRef.snapshots()
            .map { snapshot -> FirestoreDocumentMapper.toPlatformSettings(snapshot) ?: PlatformSettings() }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(PlatformSettings())
            }
    }

    suspend fun setFreeAccessEnabled(enabled: Boolean, actorRole: AppRole): Result<Unit> = runCatching {
        require(actorRole.canManageSubscriptions) { "Only Admins or Owner can toggle Free Access mode." }
        val actorUid = requireActorUid()
        val docRef = db.collection("platformSettings").document("global")
        val existing = docRef.get().await()
        val prevEnabled = existing.getBoolean("freeAccessEnabled") ?: true

        val updated = PlatformSettings(
            id = "global",
            freeAccessEnabled = enabled,
            freeAccessNotice = if (enabled) {
                "Platform-wide Free Access is currently enabled. Existing subscription records are preserved."
            } else {
                "Standard plan-based access is active. Viewers can review enabled public plans."
            },
            updatedByUid = actorUid
        )
        docRef.set(updated.toWriteMap(actorUid)).await()

        writeAuditLog(
            actorUid = actorUid,
            actorRole = actorRole,
            action = "TOGGLE_FREE_ACCESS_MODE",
            previousValue = "freeAccessEnabled=$prevEnabled",
            newValue = "freeAccessEnabled=$enabled",
            details = "Platform-wide Free Access toggled without modifying or deleting any user subscription records."
        )
    }

    // --- Immutable Audit Logs ---

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> {
        val colRef = db.collection("auditLogs")
        return colRef.snapshots()
            .map { snapshot ->
                FirestoreDocumentMapper.toAuditLogEntries(snapshot)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                emit(emptyList())
            }
    }

    suspend fun writeAuditLog(
        actorUid: String,
        actorRole: AppRole,
        action: String,
        targetUid: String? = null,
        targetContentId: String? = null,
        targetSubscriptionId: String? = null,
        previousValue: String = "",
        newValue: String = "",
        details: String = ""
    ) {
        if (!actorRole.isAdminOrOwner) return
        val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val entry = AuditLogEntry(
            id = logId,
            actorUid = actorUid,
            actorRole = if (actorRole.isOwner) AppRole.OWNER.key else AppRole.ADMIN.key,
            action = action,
            targetUid = targetUid,
            targetContentId = targetContentId,
            targetSubscriptionId = targetSubscriptionId,
            previousValue = previousValue,
            newValue = newValue,
            details = details
        )
        runCatching {
            db.collection("auditLogs").document(logId).set(entry.toCreateMap()).await()
        }.onFailure { err ->
            Log.w(TAG, "Audit log write failed for $action: ${err.message}")
        }
    }

    private suspend fun sendPrivateNotification(
        targetUid: String,
        title: String,
        message: String,
        contentId: String? = null
    ) {
        if (targetUid.isBlank()) return
        val notifId = "gov_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        runCatching {
            db.collection("users").document(targetUid)
                .collection("notifications").document(notifId)
                .set(
                    UserNotification(
                        userId = targetUid,
                        title = title,
                        message = message,
                        contentId = contentId,
                        read = false
                    ).toCreateMap()
                ).await()
        }.onFailure { err ->
            Log.w(TAG, "Private notification skipped: ${err.message}")
        }
    }

    private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private fun isPureCancellation(error: Throwable): Boolean =
        unwrapFirestore(error) == null && error is CancellationException
}
