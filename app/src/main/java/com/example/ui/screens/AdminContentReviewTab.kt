package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ContentReviewDecision
import com.example.data.model.ContentReviewStatus
import com.example.data.model.ContentSubmission
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraKidsCyan
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated
import com.example.util.VideoSourceUtils
import kotlinx.coroutines.flow.Flow

@Composable
fun AdminContentReviewTab(
    submissions: List<ContentSubmission>,
    mediaTypeFilter: String?, // null = all, "movies" = movie/documentary/short_film, "series" = TV shows
    isSaving: Boolean,
    observeReviewHistory: (String) -> Flow<List<ContentReviewDecision>>,
    onReviewSubmission: (
        submission: ContentSubmission,
        decision: ContentReviewStatus,
        notes: String,
        videoVerified: Boolean,
        rightsConfirmed: Boolean,
        metadataVerified: Boolean,
        onComplete: () -> Unit
    ) -> Unit
) {
    var statusFilter by rememberSaveable(mediaTypeFilter) { mutableStateOf("pending") }
    var activeReviewItem by remember { mutableStateOf<ContentSubmission?>(null) }

    val scoped = remember(submissions, mediaTypeFilter) {
        when (mediaTypeFilter) {
            "movies" -> submissions.filter { it.mediaType != "series" }
            "series" -> submissions.filter { it.mediaType == "series" }
            else -> submissions
        }
    }

    val filtered = remember(scoped, statusFilter) {
        when (statusFilter) {
            "pending" -> scoped.filter {
                it.parsedStatus == ContentReviewStatus.PENDING_REVIEW || it.parsedStatus == ContentReviewStatus.UNDER_REVIEW
            }
            "changes" -> scoped.filter { it.parsedStatus == ContentReviewStatus.CHANGES_REQUESTED }
            "approved" -> scoped.filter { it.parsedStatus == ContentReviewStatus.APPROVED }
            "rejected" -> scoped.filter { it.parsedStatus == ContentReviewStatus.REJECTED }
            else -> scoped
        }
    }

    val pendingCount = scoped.count {
        it.parsedStatus == ContentReviewStatus.PENDING_REVIEW || it.parsedStatus == ContentReviewStatus.UNDER_REVIEW
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = VeloraCrimson.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraCrimson.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (mediaTypeFilter) {
                            "movies" -> "Movie, Documentary & Short Film Submissions"
                            "series" -> "TV Show Submissions"
                            else -> "Content Review & Moderation Queue"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = VeloraOnBackground
                    )
                    Text(
                        text = "$pendingCount title(s) awaiting moderation. Unapproved submissions remain strictly hidden from public catalog, search, and recommendations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "pending" to "Pending ($pendingCount)",
                    "changes" to "Changes Requested",
                    "approved" to "Approved",
                    "rejected" to "Rejected",
                    "all" to "All (${scoped.size})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Text(
                        text = "No submissions match this review filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeloraOnSurfaceMuted,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            itemsIndexed(filtered, key = { idx, item -> "${item.id.ifBlank { "rev" }}_$idx" }) { _, sub ->
                val statusColor = when (sub.parsedStatus) {
                    ContentReviewStatus.APPROVED -> VeloraMatchGreen
                    ContentReviewStatus.CHANGES_REQUESTED -> VeloraCoral
                    ContentReviewStatus.REJECTED -> VeloraError
                    ContentReviewStatus.UNDER_REVIEW -> VeloraKidsCyan
                    ContentReviewStatus.PENDING_REVIEW -> VeloraGold
                }
                val detected = remember(sub.id, sub.videoUrl) {
                    VideoSourceUtils.resolveForPlayback(sub.videoUrl, sub.videoSourceType, sub.providerName)
                }

                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (sub.isRevision) "${sub.title} • Revision #${sub.revisionNumber}" else sub.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = VeloraOnBackground
                                )
                                Text(
                                    text = "By ${sub.publisherName} • ${sub.mediaType.replace('_', ' ').uppercase()} • ${sub.releaseYear} • ${sub.ageRating}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloraOnSurfaceMuted
                                )
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = sub.parsedStatus.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = sub.synopsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted,
                            maxLines = 2
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sub.distributionAuthorized) Icons.Filled.GppGood else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (sub.distributionAuthorized) VeloraMatchGreen else VeloraError,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (sub.distributionAuthorized) "Rights Confirmed" else "Rights Unconfirmed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sub.distributionAuthorized) VeloraMatchGreen else VeloraError
                            )
                            Text(
                                text = "• Source: ${detected.providerName ?: detected.detectedKind.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraKidsCyan
                            )
                        }

                        Button(
                            onClick = { activeReviewItem = sub },
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_inspect_submission_${sub.id}")
                        ) {
                            Text("Open Detailed Review")
                        }
                    }
                }
            }
        }
    }

    activeReviewItem?.let { item ->
        val historyFlow = remember(item.id) { observeReviewHistory(item.id) }
        val reviewHistory by historyFlow.collectAsStateWithLifecycle(initialValue = emptyList())
        ContentReviewDetailDialog(
            submission = item,
            reviewHistory = reviewHistory,
            isSaving = isSaving,
            onDismiss = { activeReviewItem = null },
            onDecision = { decision, notes, videoOk, rightsOk, metaOk ->
                onReviewSubmission(item, decision, notes, videoOk, rightsOk, metaOk) {
                    activeReviewItem = null
                }
            }
        )
    }
}

@Composable
private fun ContentReviewDetailDialog(
    submission: ContentSubmission,
    reviewHistory: List<ContentReviewDecision>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onDecision: (
        decision: ContentReviewStatus,
        notes: String,
        videoVerified: Boolean,
        rightsConfirmed: Boolean,
        metadataVerified: Boolean
    ) -> Unit
) {
    var notes by remember(submission.id) { mutableStateOf(submission.privateReviewerNotes) }
    var videoVerified by remember { mutableStateOf(false) }
    var rightsConfirmed by remember { mutableStateOf(submission.distributionAuthorized) }
    var metadataVerified by remember { mutableStateOf(false) }
    var pendingConfirmDecision by remember { mutableStateOf<ContentReviewStatus?>(null) }

    val resolvedSource = remember(submission.videoUrl, submission.videoSourceType) {
        VideoSourceUtils.resolveForPlayback(submission.videoUrl, submission.videoSourceType, submission.providerName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = {
            Column {
                Text(submission.title, color = VeloraOnBackground, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Submitted by ${submission.publisherName} (${submission.publisherUid.take(10)}…)",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloraOnSurfaceMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!submission.posterUrl.isNullOrBlank() || !submission.backdropUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = submission.backdropUrl ?: submission.posterUrl,
                        contentDescription = "Submission artwork preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Text("Synopsis", style = MaterialTheme.typography.labelMedium, color = VeloraGold)
                Text(submission.synopsis, style = MaterialTheme.typography.bodySmall, color = VeloraOnBackground)

                Text(
                    text = "Type: ${submission.mediaType} • Year: ${submission.releaseYear} • Rating: ${submission.ageRating} • Runtime: ${submission.runtimeMinutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloraOnSurfaceMuted
                )
                Text(
                    text = "Genres: ${submission.genres.joinToString(", ")} • Creator: ${submission.directorOrCreator}",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloraOnSurfaceMuted
                )

                Surface(
                    color = VeloraBackground,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = VeloraKidsCyan, modifier = Modifier.size(18.dp))
                            Text("Video Source Compatibility", style = MaterialTheme.typography.labelMedium, color = VeloraKidsCyan)
                        }
                        Text(
                            text = "URL: ${submission.videoUrl.ifBlank { "(Episodes below)" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnBackground
                        )
                        Text(
                            text = "Detected: ${resolvedSource.providerName ?: resolvedSource.detectedKind.label} • Type: ${resolvedSource.sourceType.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeloraMatchGreen
                        )
                        if (submission.seasons.isNotEmpty()) {
                            Text(
                                text = "TV Structure: ${submission.seasons.size} season(s), ${submission.seasons.sumOf { it.episodes.size }} episode(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraGold
                            )
                        }
                    }
                }

                Surface(
                    color = VeloraBackground,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (submission.distributionAuthorized) VeloraMatchGreen else VeloraError)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (submission.distributionAuthorized) "Distribution Authorization Confirmed by Publisher" else "Distribution Authorization Missing",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (submission.distributionAuthorized) VeloraMatchGreen else VeloraError
                        )
                        if (submission.distributionRightsStatement.isNotBlank()) {
                            Text(submission.distributionRightsStatement, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                    }
                }

                // Moderation checklist
                Text("Reviewer Verification Checklist", style = MaterialTheme.typography.labelMedium, color = VeloraOnBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = metadataVerified, onCheckedChange = { metadataVerified = it })
                    Text("Metadata, artwork & age rating verified", style = MaterialTheme.typography.bodySmall, color = VeloraOnBackground)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = videoVerified, onCheckedChange = { videoVerified = it })
                    Text("Video source URL & playback compatibility checked", style = MaterialTheme.typography.bodySmall, color = VeloraOnBackground)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rightsConfirmed, onCheckedChange = { rightsConfirmed = it })
                    Text("Distribution authorization statement verified", style = MaterialTheme.typography.bodySmall, color = VeloraOnBackground)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Private Reviewer Notes (visible to publisher & moderators)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (reviewHistory.isNotEmpty()) {
                    Text("Review History (${reviewHistory.size})", style = MaterialTheme.typography.labelMedium, color = VeloraGold)
                    reviewHistory.take(5).forEach { entry ->
                        Text(
                            text = "• ${entry.decision.uppercase()} (${entry.reviewerRole}): ${entry.notes.ifBlank { "No notes" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                }

                // Moderation action buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !isSaving,
                        onClick = { pendingConfirmDecision = ContentReviewStatus.APPROVED },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraMatchGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_approve_submission_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (submission.isRevision) "Approve & Update Live Catalog" else "Approve & Publish to Notflix")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            enabled = !isSaving,
                            onClick = { pendingConfirmDecision = ContentReviewStatus.CHANGES_REQUESTED },
                            border = BorderStroke(1.dp, VeloraCoral),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, tint = VeloraCoral, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Request Changes", color = VeloraCoral)
                        }
                        OutlinedButton(
                            enabled = !isSaving,
                            onClick = { pendingConfirmDecision = ContentReviewStatus.REJECTED },
                            border = BorderStroke(1.dp, VeloraError),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, tint = VeloraError, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", color = VeloraError)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = VeloraOnSurfaceMuted) }
        }
    )

    pendingConfirmDecision?.let { decision ->
        AlertDialog(
            onDismissRequest = { pendingConfirmDecision = null },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Confirm ${decision.label}", color = VeloraOnBackground) },
            text = {
                Text(
                    text = when (decision) {
                        ContentReviewStatus.APPROVED ->
                            "Approve \"${submission.title}\" and publish it to the live public Notflix catalog?"
                        ContentReviewStatus.CHANGES_REQUESTED ->
                            "Return \"${submission.title}\" to the publisher with your private feedback notes? It will remain hidden from viewers."
                        ContentReviewStatus.REJECTED ->
                            "Reject \"${submission.title}\"? It will remain hidden from viewers and the decision will be logged."
                        else -> "Update status to ${decision.label}?"
                    },
                    color = VeloraOnSurfaceMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirmDecision = null
                        onDecision(decision, notes, videoVerified, rightsConfirmed, metadataVerified)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (decision == ContentReviewStatus.APPROVED) VeloraMatchGreen else VeloraCrimson
                    )
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmDecision = null }) { Text("Cancel", color = VeloraOnSurfaceMuted) }
            }
        )
    }
}
