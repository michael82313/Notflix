package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ContentReviewStatus
import com.example.data.model.ContentSubmission
import com.example.data.model.Genre
import com.example.data.model.SubmissionEpisodeDraft
import com.example.data.model.SubmissionSeasonDraft
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

@Composable
fun PublisherDashboardScreen(
    isApprovedPublisher: Boolean,
    publisherUid: String,
    publisherName: String,
    submissions: List<ContentSubmission>,
    genres: List<Genre>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSubmitContent: (ContentSubmission, () -> Unit) -> Unit,
    onSubmitRevision: (original: ContentSubmission, revised: ContentSubmission, onComplete: () -> Unit) -> Unit
) {
    if (!isApprovedPublisher) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VeloraBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = VeloraError, modifier = Modifier.size(56.dp))
                Text(
                    "Approved Publisher/Distributor Required",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VeloraOnBackground
                )
                Text(
                    "Apply for Publisher/Distributor status in Account & Settings. Only approved Publishers can access this dashboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeloraOnSurfaceMuted
                )
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)) {
                    Text("Return")
                }
            }
        }
        return
    }

    var statusFilter by rememberSaveable { mutableStateOf("all") }
    var editorMediaType by remember { mutableStateOf<String?>(null) }
    var editingSubmission by remember { mutableStateOf<ContentSubmission?>(null) }
    var revisionSource by remember { mutableStateOf<ContentSubmission?>(null) }

    val pendingCount = submissions.count {
        it.parsedStatus == ContentReviewStatus.PENDING_REVIEW || it.parsedStatus == ContentReviewStatus.UNDER_REVIEW
    }
    val approvedCount = submissions.count { it.parsedStatus == ContentReviewStatus.APPROVED }
    val changesCount = submissions.count { it.parsedStatus == ContentReviewStatus.CHANGES_REQUESTED }
    val rejectedCount = submissions.count { it.parsedStatus == ContentReviewStatus.REJECTED }

    val filteredSubmissions = remember(submissions, statusFilter) {
        when (statusFilter) {
            "pending" -> submissions.filter {
                it.parsedStatus == ContentReviewStatus.PENDING_REVIEW || it.parsedStatus == ContentReviewStatus.UNDER_REVIEW
            }
            "approved" -> submissions.filter { it.parsedStatus == ContentReviewStatus.APPROVED }
            "changes" -> submissions.filter { it.parsedStatus == ContentReviewStatus.CHANGES_REQUESTED }
            "rejected" -> submissions.filter { it.parsedStatus == ContentReviewStatus.REJECTED }
            else -> submissions
        }
    }

    Scaffold(containerColor = VeloraBackground) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("publisher_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VeloraOnBackground)
                        }
                        Column {
                            Text("Publisher Dashboard", style = MaterialTheme.typography.headlineSmall, color = VeloraOnBackground)
                            Text(
                                text = "$publisherName • All submissions require individual review",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )
                        }
                    }
                }
            }

            // Overview Stats
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PublisherStatCard("Pending", pendingCount.toString(), VeloraGold, Modifier.weight(1f))
                    PublisherStatCard("Approved", approvedCount.toString(), VeloraMatchGreen, Modifier.weight(1f))
                    PublisherStatCard("Changes", changesCount.toString(), VeloraCoral, Modifier.weight(1f))
                    PublisherStatCard("Rejected", rejectedCount.toString(), VeloraError, Modifier.weight(1f))
                }
            }

            // Submit Actions
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { editorMediaType = "movie" },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("publisher_submit_movie_button")
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit Movie")
                    }
                    OutlinedButton(
                        onClick = { editorMediaType = "series" },
                        border = BorderStroke(1.dp, VeloraCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("publisher_submit_series_button")
                    ) {
                        Icon(Icons.Filled.LiveTv, contentDescription = null, tint = VeloraCrimson, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit TV Show", color = VeloraCrimson)
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "all" to "My Submissions (${submissions.size})",
                        "pending" to "Pending Reviews ($pendingCount)",
                        "changes" to "Changes Requested ($changesCount)",
                        "approved" to "Approved ($approvedCount)",
                        "rejected" to "Rejected ($rejectedCount)"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (filteredSubmissions.isEmpty()) {
                item {
                    Surface(
                        color = VeloraSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, VeloraOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No submissions in this view", style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                            Text(
                                "Submit a Movie, Documentary, Short Film, or TV Show above. Every title is reviewed privately by Notflix moderators before going public.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredSubmissions, key = { idx, item -> "${item.id.ifBlank { "sub" }}_$idx" }) { _, sub ->
                    val statusColor = when (sub.parsedStatus) {
                        ContentReviewStatus.APPROVED -> VeloraMatchGreen
                        ContentReviewStatus.CHANGES_REQUESTED -> VeloraCoral
                        ContentReviewStatus.REJECTED -> VeloraError
                        ContentReviewStatus.UNDER_REVIEW -> VeloraKidsCyan
                        ContentReviewStatus.PENDING_REVIEW -> VeloraGold
                    }
                    Surface(
                        color = VeloraSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
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
                                        text = if (sub.isRevision) "${sub.title} (Rev #${sub.revisionNumber})" else sub.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = VeloraOnBackground
                                    )
                                    Text(
                                        text = "${sub.mediaType.replace('_', ' ').uppercase()} • ${sub.releaseYear} • ${sub.ageRating}",
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

                            if (sub.privateReviewerNotes.isNotBlank()) {
                                Surface(
                                    color = VeloraBackground,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.RateReview, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Private Reviewer Feedback", style = MaterialTheme.typography.labelMedium, color = statusColor)
                                            Text(sub.privateReviewerNotes, style = MaterialTheme.typography.bodySmall, color = VeloraOnBackground)
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (sub.canPublisherEditInPlace) {
                                    OutlinedButton(
                                        onClick = { editingSubmission = sub },
                                        border = BorderStroke(1.dp, VeloraOutline)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (sub.parsedStatus == ContentReviewStatus.CHANGES_REQUESTED) "Edit & Resubmit" else "Edit Submission",
                                            color = VeloraOnBackground
                                        )
                                    }
                                } else if (sub.parsedStatus == ContentReviewStatus.APPROVED) {
                                    OutlinedButton(
                                        onClick = { revisionSource = sub },
                                        border = BorderStroke(1.dp, VeloraGold.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Submit Revision for Review", color = VeloraGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeEditorInitial = editingSubmission ?: revisionSource
    if (editorMediaType != null || activeEditorInitial != null) {
        val isRevisionMode = revisionSource != null
        PublisherSubmissionEditorDialog(
            initial = activeEditorInitial,
            initialMediaType = activeEditorInitial?.mediaType ?: (editorMediaType ?: "movie"),
            publisherUid = publisherUid,
            publisherName = publisherName,
            genres = genres,
            isRevisionMode = isRevisionMode,
            isSaving = isSaving,
            onDismiss = {
                editorMediaType = null
                editingSubmission = null
                revisionSource = null
            },
            onSave = { built ->
                if (isRevisionMode && revisionSource != null) {
                    onSubmitRevision(revisionSource!!, built) {
                        editorMediaType = null
                        editingSubmission = null
                        revisionSource = null
                    }
                } else {
                    onSubmitContent(built) {
                        editorMediaType = null
                        editingSubmission = null
                        revisionSource = null
                    }
                }
            }
        )
    }
}

@Composable
private fun PublisherStatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = VeloraSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, VeloraOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = VeloraOnSurfaceMuted, maxLines = 1)
        }
    }
}

@Composable
private fun PublisherSubmissionEditorDialog(
    initial: ContentSubmission?,
    initialMediaType: String,
    publisherUid: String,
    publisherName: String,
    genres: List<Genre>,
    isRevisionMode: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ContentSubmission) -> Unit
) {
    val seed = remember(initial, initialMediaType, publisherName) {
        initial ?: ContentSubmission(
            mediaType = initialMediaType,
            releaseYear = 2025,
            runtimeMinutes = 90,
            ageRating = "PG-13",
            directorOrCreator = publisherName,
            genres = listOf(genres.firstOrNull()?.name ?: "Drama")
        )
    }
    var mediaType by remember { mutableStateOf(seed.mediaType) }
    var title by remember { mutableStateOf(seed.title) }
    var synopsis by remember { mutableStateOf(seed.synopsis) }
    var releaseYear by remember { mutableStateOf(seed.releaseYear.toString()) }
    var runtime by remember { mutableStateOf(seed.runtimeMinutes.toString()) }
    var ageRating by remember { mutableStateOf(seed.ageRating) }
    var director by remember { mutableStateOf(seed.directorOrCreator) }
    var castText by remember { mutableStateOf(seed.cast.joinToString(", ")) }
    var genresText by remember {
        mutableStateOf(seed.genres.joinToString(", ").ifBlank { genres.firstOrNull()?.name ?: "Drama" })
    }
    var posterUrl by remember { mutableStateOf(seed.posterUrl.orEmpty()) }
    var backdropUrl by remember { mutableStateOf(seed.backdropUrl.orEmpty()) }
    var trailerUrl by remember { mutableStateOf(seed.trailerUrl.orEmpty()) }
    var videoUrl by remember { mutableStateOf(seed.videoUrl) }
    var isKidsFriendly by remember { mutableStateOf(seed.isKidsFriendly) }
    var rightsConfirmed by remember { mutableStateOf(seed.distributionAuthorized) }
    var rightsStatement by remember { mutableStateOf(seed.distributionRightsStatement) }
    var seasons by remember {
        mutableStateOf(
            seed.seasons.ifEmpty {
                if (mediaType == "series") {
                    listOf(
                        SubmissionSeasonDraft(
                            seasonNumber = 1,
                            title = "Season 1",
                            year = 2025,
                            episodes = listOf(
                                SubmissionEpisodeDraft(seasonNumber = 1, episodeNumber = 1, title = "Episode 1")
                            )
                        )
                    )
                } else emptyList()
            }
        )
    }

    val detectedSource = remember(videoUrl) {
        VideoSourceUtils.resolveForPlayback(videoUrl, "auto", null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = {
            Text(
                text = when {
                    isRevisionMode -> "Submit Revision for Review"
                    initial != null -> "Edit & Resubmit Content"
                    else -> "Submit Content for Review"
                },
                color = VeloraOnBackground
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isRevisionMode) {
                    Text(
                        "Editing an approved title creates a pending revision. The published public listing stays unchanged until an Admin approves your changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraGold
                    )
                }

                // Content category chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "movie" to "Movie",
                        "series" to "TV Show",
                        "documentary" to "Documentary",
                        "short_film" to "Short Film"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = mediaType == key,
                            onClick = {
                                mediaType = key
                                if (key == "series" && seasons.isEmpty()) {
                                    seasons = listOf(
                                        SubmissionSeasonDraft(
                                            seasonNumber = 1,
                                            title = "Season 1",
                                            year = releaseYear.toIntOrNull() ?: 2025,
                                            episodes = listOf(
                                                SubmissionEpisodeDraft(seasonNumber = 1, episodeNumber = 1, title = "Episode 1")
                                            )
                                        )
                                    )
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = synopsis, onValueChange = { synopsis = it }, label = { Text("Description / Synopsis *") }, minLines = 3, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = releaseYear, onValueChange = { releaseYear = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = runtime, onValueChange = { runtime = it }, label = { Text("Runtime (min)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ageRating, onValueChange = { ageRating = it }, label = { Text("Rating") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = genresText, onValueChange = { genresText = it }, label = { Text("Genres (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = director, onValueChange = { director = it }, label = { Text("Director / Creator") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = castText, onValueChange = { castText = it }, label = { Text("Cast (comma-separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster Image HTTPS URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = backdropUrl, onValueChange = { backdropUrl = it }, label = { Text("Banner / Backdrop HTTPS URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = trailerUrl, onValueChange = { trailerUrl = it }, label = { Text("Trailer URL (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (mediaType != "series") {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("Video Source URL (MP4, HLS, YouTube, Vimeo, Dailymotion) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (videoUrl.isNotBlank()) {
                        Text(
                            text = "Detected: ${detectedSource.providerName ?: detectedSource.detectedKind.label} (${detectedSource.sourceType.label})",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeloraMatchGreen
                        )
                    }
                } else {
                    Text("Seasons & Episodes", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                    seasons.forEachIndexed { sIdx, season ->
                        Surface(
                            color = VeloraBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VeloraOutline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Season ${season.seasonNumber}", style = MaterialTheme.typography.titleSmall, color = VeloraGold)
                                    TextButton(onClick = {
                                        val nextEp = season.episodes.size + 1
                                        val updatedEps = season.episodes + SubmissionEpisodeDraft(
                                            seasonNumber = season.seasonNumber,
                                            episodeNumber = nextEp,
                                            title = "Episode $nextEp"
                                        )
                                        seasons = seasons.toMutableList().also { it[sIdx] = season.copy(episodes = updatedEps) }
                                    }) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Add Ep")
                                    }
                                }
                                season.episodes.forEachIndexed { eIdx, ep ->
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = ep.title,
                                                onValueChange = { v ->
                                                    val eps = season.episodes.toMutableList().also { it[eIdx] = ep.copy(title = v) }
                                                    seasons = seasons.toMutableList().also { it[sIdx] = season.copy(episodes = eps) }
                                                },
                                                label = { Text("S${season.seasonNumber}E${ep.episodeNumber} Title") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (season.episodes.size > 1) {
                                                IconButton(onClick = {
                                                    val eps = season.episodes.filterIndexed { i, _ -> i != eIdx }
                                                    seasons = seasons.toMutableList().also { it[sIdx] = season.copy(episodes = eps) }
                                                }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Remove episode", tint = VeloraError)
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = ep.videoUrl,
                                            onValueChange = { v ->
                                                val eps = season.episodes.toMutableList().also { it[eIdx] = ep.copy(videoUrl = v) }
                                                seasons = seasons.toMutableList().also { it[sIdx] = season.copy(episodes = eps) }
                                            },
                                            label = { Text("Episode Video URL") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val nextS = seasons.size + 1
                            seasons = seasons + SubmissionSeasonDraft(
                                seasonNumber = nextS,
                                title = "Season $nextS",
                                year = releaseYear.toIntOrNull() ?: 2025,
                                episodes = listOf(SubmissionEpisodeDraft(seasonNumber = nextS, episodeNumber = 1, title = "Episode 1"))
                            )
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Season")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kids-Safe / Family Friendly", style = MaterialTheme.typography.bodyMedium, color = VeloraOnBackground)
                    Switch(checked = isKidsFriendly, onCheckedChange = { isKidsFriendly = it })
                }

                OutlinedTextField(
                    value = rightsStatement,
                    onValueChange = { rightsStatement = it },
                    label = { Text("Distribution Rights / License Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = VeloraBackground,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (rightsConfirmed) VeloraMatchGreen else VeloraCoral)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rightsConfirmed,
                            onCheckedChange = { rightsConfirmed = it },
                            modifier = Modifier.testTag("publisher_rights_checkbox")
                        )
                        Text(
                            text = "I confirm I own or hold legal distribution authorization to publish this title on Notflix, and understand all submissions undergo individual content review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnBackground
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasMedia = if (mediaType == "series") {
                seasons.any { s -> s.episodes.any { it.videoUrl.isNotBlank() } }
            } else {
                videoUrl.isNotBlank()
            }
            Button(
                enabled = !isSaving && title.isNotBlank() && synopsis.isNotBlank() && rightsConfirmed && hasMedia,
                onClick = {
                    val primaryUrl = if (mediaType == "series") {
                        seasons.firstOrNull()?.episodes?.firstOrNull()?.videoUrl.orEmpty()
                    } else videoUrl
                    val resolved = VideoSourceUtils.resolveForPlayback(primaryUrl, "auto", null)
                    val built = ContentSubmission(
                        id = if (isRevisionMode) "" else (initial?.id ?: ""),
                        publisherUid = publisherUid,
                        publisherName = publisherName,
                        mediaType = mediaType,
                        title = title.trim(),
                        synopsis = synopsis.trim(),
                        genres = genresText.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Drama") },
                        releaseYear = releaseYear.toIntOrNull() ?: 2025,
                        runtimeMinutes = runtime.toIntOrNull() ?: 90,
                        ageRating = ageRating.trim().ifBlank { "PG-13" },
                        directorOrCreator = director.trim().ifBlank { publisherName },
                        cast = castText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        posterUrl = posterUrl.trim().takeIf { it.isNotBlank() },
                        backdropUrl = backdropUrl.trim().takeIf { it.isNotBlank() },
                        trailerUrl = trailerUrl.trim().takeIf { it.isNotBlank() },
                        videoUrl = primaryUrl.trim(),
                        videoSourceType = "auto",
                        providerName = resolved.providerName,
                        isKidsFriendly = isKidsFriendly,
                        seasons = if (mediaType == "series") seasons else emptyList(),
                        distributionAuthorized = rightsConfirmed,
                        distributionRightsStatement = rightsStatement.trim(),
                        reviewStatus = ContentReviewStatus.PENDING_REVIEW.key
                    )
                    onSave(built)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                modifier = Modifier.testTag("publisher_confirm_submit_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isSaving) "Submitting..." else "Submit for Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VeloraOnSurfaceMuted) }
        }
    )
}
