package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.Season
import com.example.data.model.SeasonDraft
import com.example.data.repository.MediaAssetCategory
import com.example.data.repository.MediaUploadState
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurface
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated
import androidx.compose.runtime.rememberCoroutineScope
import com.example.util.DetectedSourceKind
import com.example.util.PlaybackSourceResolver
import com.example.util.ResolvedPlaybackSource
import com.example.util.VideoSourceType
import com.example.util.VideoSourceUtils
import kotlinx.coroutines.launch

private val AGE_RATINGS = listOf("G", "PG", "PG-13", "TV-G", "TV-PG", "TV-14", "TV-MA", "R")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCatalogEditorDialog(
    initial: CatalogItem?,
    initialMediaType: String,
    initialIsComingSoon: Boolean = false,
    availableGenres: List<Genre>,
    loadedSeriesSeasons: List<SeasonDraft>,
    isLoadingSeriesSeasons: Boolean,
    isSaving: Boolean,
    mediaUploadStates: Map<String, MediaUploadState> = emptyMap(),
    onUploadMedia: ((fieldKey: String, uri: Uri, category: MediaAssetCategory, contentFolderHint: String, onUploaded: (String) -> Unit) -> Unit)? = null,
    onClearUploadState: ((String?) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSaveMovie: (CatalogItem) -> Unit,
    onSaveSeries: (
        series: CatalogItem,
        seasons: List<SeasonDraft>,
        removedSeasonIds: Set<String>,
        removedEpisodeRefs: List<Pair<String, String>>
    ) -> Unit
) {
    val initialId = initial?.id
    val seed = remember(initialId, initialMediaType, initialIsComingSoon) {
        initial ?: CatalogItem(
            mediaType = initialMediaType,
            releaseYear = 2026,
            runtimeMinutes = if (initialMediaType == "series") 45 else 110,
            ageRating = "PG-13",
            director = "Notflix Studios",
            genres = listOf("Drama"),
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            isStaffPick = false,
            isAwardWinning = false,
            isComingSoon = initialIsComingSoon
        )
    }

    var mediaType by remember(initialId, initialMediaType) { mutableStateOf(seed.mediaType) }
    var title by remember(initialId) { mutableStateOf(seed.title) }
    var synopsis by remember(initialId) { mutableStateOf(seed.synopsis) }
    var releaseYear by remember(initialId) { mutableStateOf(seed.releaseYear.toString()) }
    var runtime by remember(initialId) { mutableStateOf(seed.runtimeMinutes.toString()) }
    var ageRating by remember(initialId) { mutableStateOf(seed.ageRating) }
    var director by remember(initialId) { mutableStateOf(seed.director) }
    var castText by remember(initialId) { mutableStateOf(seed.cast.joinToString(", ")) }

    val knownGenreNames = remember(availableGenres) {
        availableGenres.map { it.name }.ifEmpty {
            listOf("Action", "Sci-Fi", "Drama", "Thriller", "Comedy", "Animation", "Documentary")
        }
    }
    val initialGenres = remember(initialId) { seed.genres.ifEmpty { listOf("Drama") } }
    var selectedGenres by remember(initialId) {
        mutableStateOf(initialGenres.filter { g -> knownGenreNames.any { it.equals(g, ignoreCase = true) } }.toSet())
    }
    var customGenresText by remember(initialId) {
        mutableStateOf(
            initialGenres.filter { g -> knownGenreNames.none { it.equals(g, ignoreCase = true) } }
                .joinToString(", ")
        )
    }

    var posterUrl by remember(initialId) { mutableStateOf(seed.posterUrl.orEmpty()) }
    var backdropUrl by remember(initialId) { mutableStateOf(seed.backdropUrl.orEmpty()) }
    var trailerUrl by remember(initialId) { mutableStateOf(seed.trailerUrl.orEmpty()) }

    val initialSourceType = remember(initialId) {
        if (initial == null) {
            VideoSourceType.AUTO
        } else if (seed.videoSourceType.isNotBlank()) {
            VideoSourceType.fromKey(seed.videoSourceType)
        } else {
            VideoSourceUtils.detectSourceTypeFromUrl(seed.preferredStreamUrl, VideoSourceType.AUTO)
        }
    }
    var videoSourceType by remember(initialId) { mutableStateOf(initialSourceType) }
    var videoUrl by remember(initialId) { mutableStateOf(seed.preferredStreamUrl) }
    var providerName by remember(initialId) { mutableStateOf(seed.providerName.orEmpty()) }

    var isFeatured by remember(initialId) { mutableStateOf(seed.isFeatured) }
    var isVisible by remember(initialId) { mutableStateOf(seed.isVisible) }
    var isKidsFriendly by remember(initialId) { mutableStateOf(seed.isKidsFriendly) }
    var isStaffPick by remember(initialId) { mutableStateOf(seed.isStaffPick) }
    var isAwardWinning by remember(initialId) { mutableStateOf(seed.isAwardWinning) }
    var isComingSoon by remember(initialId, initialIsComingSoon) { mutableStateOf(seed.isComingSoon) }
    var expectedReleaseDate by remember(initialId) { mutableStateOf(seed.expectedReleaseDate.orEmpty()) }
    var leavingSoonAt by remember(initialId) { mutableStateOf(seed.leavingSoonAt.orEmpty()) }
    var tagsText by remember(initialId) { mutableStateOf(seed.tags.joinToString(", ")) }
    var keywordsText by remember(initialId) { mutableStateOf(seed.keywords.joinToString(", ")) }

    val seasonDrafts = remember(initialId) { mutableStateListOf<SeasonDraft>() }
    val removedSeasonIds = remember(initialId) { mutableSetOf<String>() }
    val removedEpisodeRefs = remember(initialId) { mutableListOf<Pair<String, String>>() }
    var initializedFromLoad by remember(initialId) { mutableStateOf(false) }

    LaunchedEffect(loadedSeriesSeasons, isLoadingSeriesSeasons, mediaType) {
        if (mediaType == "series" && !isLoadingSeriesSeasons && !initializedFromLoad) {
            seasonDrafts.clear()
            if (loadedSeriesSeasons.isNotEmpty()) {
                seasonDrafts.addAll(loadedSeriesSeasons)
            } else {
                seasonDrafts.add(
                    SeasonDraft(
                        season = Season(
                            id = "season-1",
                            seasonNumber = 1,
                            title = "Season 1",
                            year = releaseYear.toIntOrNull() ?: 2026,
                            isVisible = true
                        ),
                        episodes = emptyList()
                    )
                )
            }
            initializedFromLoad = true
        }
    }

    var editingEpisodeTarget by remember { mutableStateOf<Pair<Int, Episode?>?>(null) } // seasonIndex to existingEpisode?
    var editingSeasonIndex by remember { mutableStateOf<Int?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    val posterValidation = remember(posterUrl) { VideoSourceUtils.validateOptionalUrl(posterUrl, "Poster URL") }
    val backdropValidation = remember(backdropUrl) { VideoSourceUtils.validateOptionalUrl(backdropUrl, "Backdrop URL") }
    val trailerValidation = remember(trailerUrl) { VideoSourceUtils.validateTrailerUrl(trailerUrl) }
    val videoSourceValidation = remember(videoUrl, videoSourceType, mediaType, isComingSoon) {
        VideoSourceUtils.validateVideoSource(
            url = videoUrl,
            sourceType = videoSourceType,
            required = mediaType == "movie" && !isComingSoon
        )
    }

    fun submitForm() {
        val trimmedTitle = title.trim()
        val trimmedSynopsis = synopsis.trim()
        val parsedYear = releaseYear.trim().toIntOrNull()
        val parsedRuntime = runtime.trim().toIntOrNull()
        val combinedGenres = (selectedGenres.toList() +
            customGenresText.split(",").map { it.trim() }.filter { it.isNotBlank() })
            .distinctBy { it.lowercase() }

        when {
            trimmedTitle.isBlank() -> {
                formError = "Title is required."
                return
            }
            trimmedSynopsis.isBlank() -> {
                formError = "Description is required."
                return
            }
            parsedYear == null || parsedYear !in 1900..2100 -> {
                formError = "Enter a valid release year between 1900 and 2100."
                return
            }
            parsedRuntime == null || parsedRuntime !in 1..1000 -> {
                formError = "Enter a valid runtime in minutes (1–1000)."
                return
            }
            combinedGenres.isEmpty() -> {
                formError = "Select or enter at least one genre."
                return
            }
            ageRating.isBlank() -> {
                formError = "Age rating is required."
                return
            }
            !posterValidation.isValid -> {
                formError = posterValidation.errorOrNull
                return
            }
            !backdropValidation.isValid -> {
                formError = backdropValidation.errorOrNull
                return
            }
            !trailerValidation.isValid -> {
                formError = "Trailer URL: ${trailerValidation.errorOrNull}"
                return
            }
            !videoSourceValidation.isValid -> {
                formError = videoSourceValidation.errorOrNull
                return
            }
            mediaType == "series" && !isComingSoon -> {
                if (seasonDrafts.isEmpty()) {
                    formError = "Add at least one season to this TV show."
                    return
                }
                val emptySeason = seasonDrafts.firstOrNull { it.episodes.isEmpty() }
                if (emptySeason != null) {
                    formError = "Season ${emptySeason.season.seasonNumber} needs at least one episode with a video source."
                    return
                }
                for (draft in seasonDrafts) {
                    for (ep in draft.episodes) {
                        val epType = VideoSourceType.fromKey(ep.videoSourceType)
                        val epCheck = VideoSourceUtils.validateVideoSource(ep.preferredStreamUrl, epType, required = true)
                        if (!epCheck.isValid) {
                            formError = "S${draft.season.seasonNumber} E${ep.episodeNumber} (${ep.title}): ${epCheck.errorOrNull}"
                            return
                        }
                    }
                }
            }
        }

        formError = null
        val cleanedUrl = videoUrl.trim()
        val effectiveKind = VideoSourceUtils.detectClassification(cleanedUrl, providerName).detectedKind
        val hlsValue = if ((videoSourceType == VideoSourceType.HLS || effectiveKind == DetectedSourceKind.HLS_STREAM) && cleanedUrl.isNotBlank()) {
            cleanedUrl
        } else null
        val cleanedProvider = providerName.trim().takeIf { it.isNotBlank() }
        val parsedTags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
        val parsedKeywords = keywordsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
        val itemToSave = (initial ?: CatalogItem()).copy(
            title = trimmedTitle,
            synopsis = trimmedSynopsis,
            mediaType = mediaType,
            genres = combinedGenres,
            releaseYear = parsedYear!!,
            runtimeMinutes = parsedRuntime!!,
            ageRating = ageRating.trim(),
            director = director.trim().ifBlank { "Notflix Studios" },
            cast = castText.split(",").map { it.trim() }.filter { it.isNotBlank() },
            posterUrl = posterUrl.trim().takeIf { it.isNotBlank() },
            backdropUrl = backdropUrl.trim().takeIf { it.isNotBlank() },
            trailerUrl = trailerUrl.trim().takeIf { it.isNotBlank() },
            videoUrl = cleanedUrl,
            videoSourceType = videoSourceType.key,
            providerName = cleanedProvider,
            hlsUrl = hlsValue,
            isFeatured = if (isComingSoon) false else isFeatured,
            isVisible = isVisible,
            isKidsFriendly = isKidsFriendly,
            tags = parsedTags,
            keywords = parsedKeywords,
            isStaffPick = isStaffPick,
            isAwardWinning = isAwardWinning,
            isComingSoon = isComingSoon,
            expectedReleaseDate = if (isComingSoon) expectedReleaseDate.trim().takeIf { it.isNotBlank() } else null,
            leavingSoonAt = leavingSoonAt.trim().takeIf { it.isNotBlank() }
        )

        if (mediaType == "series") {
            val seasonsToPersist = if (isComingSoon) {
                seasonDrafts.filter { it.episodes.isNotEmpty() }
            } else {
                seasonDrafts.toList()
            }
            onSaveSeries(itemToSave, seasonsToPersist, removedSeasonIds.toSet(), removedEpisodeRefs.toList())
        } else {
            onSaveMovie(itemToSave)
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = VeloraBackground,
            topBar = {
                Surface(
                    color = VeloraSurface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (!isSaving) onDismiss() },
                                modifier = Modifier.testTag("editor_close_button")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close editor", tint = VeloraOnBackground)
                            }
                            Column {
                                Text(
                                    text = when {
                                        initial == null && mediaType == "series" -> "Add TV Show"
                                        initial == null -> "Add Movie"
                                        mediaType == "series" -> "Edit TV Show"
                                        else -> "Edit Movie"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = VeloraOnBackground
                                )
                                Text(
                                    text = "Saved directly to Notflix Firestore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeloraOnSurfaceMuted
                                )
                            }
                        }

                        Button(
                            onClick = ::submitForm,
                            enabled = !isSaving && !isLoadingSeriesSeasons,
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                            modifier = Modifier.testTag("editor_save_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Content Type Toggle
                item {
                    EditorSectionCard(title = "Content Format") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = mediaType == "movie",
                                onClick = {
                                    mediaType = "movie"
                                    formError = null
                                },
                                enabled = initial == null,
                                leadingIcon = { Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                label = { Text("Movie") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VeloraCrimson,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_type_movie")
                            )
                            FilterChip(
                                selected = mediaType == "series",
                                onClick = {
                                    mediaType = "series"
                                    if (seasonDrafts.isEmpty()) {
                                        seasonDrafts.add(
                                            SeasonDraft(
                                                season = Season(
                                                    id = "season-1",
                                                    seasonNumber = 1,
                                                    title = "Season 1",
                                                    year = releaseYear.toIntOrNull() ?: 2026,
                                                    isVisible = true
                                                ),
                                                episodes = emptyList()
                                            )
                                        )
                                    }
                                    formError = null
                                },
                                enabled = initial == null,
                                leadingIcon = { Icon(Icons.Filled.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                label = { Text("TV Show") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VeloraCrimson,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_type_series")
                            )
                        }
                    }
                }

                // Core Metadata
                item {
                    EditorSectionCard(title = "Title & Details") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title*") },
                            placeholder = { Text(if (mediaType == "series") "e.g. Cyber Horizon" else "e.g. Tears of Steel") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input")
                        )
                        OutlinedTextField(
                            value = synopsis,
                            onValueChange = { synopsis = it },
                            label = { Text("Description*") },
                            placeholder = { Text("Enter full movie or series synopsis...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_description_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = releaseYear,
                                onValueChange = { releaseYear = it.filter { ch -> ch.isDigit() }.take(4) },
                                label = { Text("Release Year*") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_year_input")
                            )
                            OutlinedTextField(
                                value = runtime,
                                onValueChange = { runtime = it.filter { ch -> ch.isDigit() }.take(4) },
                                label = { Text(if (mediaType == "series") "Avg Ep Mins*" else "Runtime (min)*") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_runtime_input")
                            )
                        }

                        Text("Age Rating*", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AGE_RATINGS.forEach { ratingOption ->
                                FilterChip(
                                    selected = ageRating.equals(ratingOption, ignoreCase = true),
                                    onClick = { ageRating = ratingOption },
                                    label = { Text(ratingOption) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VeloraCrimson,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value = ageRating,
                            onValueChange = { ageRating = it.take(20) },
                            label = { Text("Age Rating (Custom or Selected)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_age_rating_input")
                        )

                        Text("Genres*", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            knownGenreNames.forEach { genreName ->
                                val isSelected = selectedGenres.any { it.equals(genreName, ignoreCase = true) }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedGenres = if (isSelected) {
                                            selectedGenres.filterNot { it.equals(genreName, ignoreCase = true) }.toSet()
                                        } else {
                                            selectedGenres + genreName
                                        }
                                    },
                                    label = { Text(genreName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VeloraCrimson,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value = customGenresText,
                            onValueChange = { customGenresText = it },
                            label = { Text("Additional Genres (comma-separated, optional)") },
                            placeholder = { Text("e.g. Cyberpunk, Mystery") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_genres_input")
                        )

                        OutlinedTextField(
                            value = director,
                            onValueChange = { director = it },
                            label = { Text(if (mediaType == "series") "Creator / Showrunner" else "Director") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = castText,
                            onValueChange = { castText = it },
                            label = { Text("Cast (comma-separated)") },
                            placeholder = { Text("Actor One, Actor Two") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            label = { Text("Discovery Tags (comma-separated)") },
                            placeholder = { Text("e.g. Cyberpunk, Binge-Worthy, Superhero, Rom-Com") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_tags_input")
                        )
                        OutlinedTextField(
                            value = keywordsText,
                            onValueChange = { keywordsText = it },
                            label = { Text("Recommendation Keywords (comma-separated)") },
                            placeholder = { Text("e.g. heist, time loop, space, haunted") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_keywords_input")
                        )
                    }
                }

                // Artwork & Trailer URLs + Optional Photo Picker Upload
                item {
                    val folderHint = title.ifBlank { initial?.id ?: "catalog-item" }
                    val posterPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null && onUploadMedia != null) {
                            onUploadMedia("poster", uri, MediaAssetCategory.POSTER, folderHint) { url ->
                                posterUrl = url
                            }
                        }
                    }
                    val backdropPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null && onUploadMedia != null) {
                            onUploadMedia("backdrop", uri, MediaAssetCategory.BACKDROP, folderHint) { url ->
                                backdropUrl = url
                            }
                        }
                    }
                    val trailerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null && onUploadMedia != null) {
                            onUploadMedia("trailer", uri, MediaAssetCategory.TRAILER, folderHint) { url ->
                                trailerUrl = url
                            }
                        }
                    }

                    EditorSectionCard(title = "Artwork & Trailer") {
                        OutlinedTextField(
                            value = posterUrl,
                            onValueChange = { posterUrl = it },
                            label = { Text("Poster URL (https://...)") },
                            placeholder = { Text("https://example.com/poster.jpg") },
                            isError = !posterValidation.isValid,
                            supportingText = {
                                if (!posterValidation.isValid) {
                                    Text(posterValidation.errorOrNull!!, color = VeloraError)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_poster_url_input")
                        )
                        if (onUploadMedia != null) {
                            MediaUploadControlRow(
                                label = "Upload Poster Image",
                                state = mediaUploadStates["poster"],
                                onPickClick = {
                                    onClearUploadState?.invoke("poster")
                                    posterPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                        }
                        if (posterUrl.isNotBlank() && posterValidation.isValid) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = posterUrl.trim(),
                                    contentDescription = "Poster preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VeloraSurfaceBright)
                                )
                                Text("Poster URL preview", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                            }
                        }

                        OutlinedTextField(
                            value = backdropUrl,
                            onValueChange = { backdropUrl = it },
                            label = { Text("Backdrop URL (https://...)") },
                            placeholder = { Text("https://example.com/backdrop.jpg") },
                            isError = !backdropValidation.isValid,
                            supportingText = {
                                if (!backdropValidation.isValid) {
                                    Text(backdropValidation.errorOrNull!!, color = VeloraError)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_backdrop_url_input")
                        )
                        if (onUploadMedia != null) {
                            MediaUploadControlRow(
                                label = "Upload Backdrop Image",
                                state = mediaUploadStates["backdrop"],
                                onPickClick = {
                                    onClearUploadState?.invoke("backdrop")
                                    backdropPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                        }
                        if (backdropUrl.isNotBlank() && backdropValidation.isValid) {
                            AsyncImage(
                                model = backdropUrl.trim(),
                                contentDescription = "Backdrop preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VeloraSurfaceBright)
                            )
                        }

                        OutlinedTextField(
                            value = trailerUrl,
                            onValueChange = { trailerUrl = it },
                            label = { Text("Trailer URL (Auto-detect, YouTube, Vimeo, MP4, or HLS)") },
                            placeholder = { Text("https://www.youtube.com/watch?v=... or https://.../trailer.mp4") },
                            isError = !trailerValidation.isValid,
                            supportingText = {
                                if (!trailerValidation.isValid) {
                                    Text(trailerValidation.errorOrNull!!, color = VeloraError)
                                } else if (trailerUrl.isNotBlank()) {
                                    val detected = VideoSourceUtils.detectClassification(trailerUrl)
                                    Text("Detected trailer: ${detected.detectedKind.label} (${detected.providerName ?: "Auto"})", color = VeloraMatchGreen)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_trailer_url_input")
                        )
                        if (onUploadMedia != null) {
                            MediaUploadControlRow(
                                label = "Upload Trailer Video",
                                state = mediaUploadStates["trailer"],
                                onPickClick = {
                                    onClearUploadState?.invoke("trailer")
                                    trailerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                            )
                        }
                    }
                }

                // Multi-Host Video Source Section
                item {
                    val folderHint = title.ifBlank { initial?.id ?: "catalog-item" }
                    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null && onUploadMedia != null) {
                            onUploadMedia("video", uri, MediaAssetCategory.VIDEO, folderHint) { url ->
                                videoUrl = url
                                videoSourceType = VideoSourceType.MP4
                                if (providerName.isBlank()) providerName = "Firebase Storage"
                            }
                        }
                    }

                    EditorSectionCard(
                        title = when {
                            isComingSoon -> "Video Source (Optional for Coming Soon)"
                            mediaType == "series" -> "Series Preview / Fallback Video Source (Optional)"
                            else -> "Video / Webpage Source*"
                        }
                    ) {
                        if (isComingSoon) {
                            Text(
                                text = "Coming Soon titles are non-playable until release. You can leave the video URL blank and attach only a Trailer URL above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraGold
                            )
                        } else if (mediaType == "series") {
                            Text(
                                text = "Each TV episode below has its own video source. Optionally provide a series-level fallback URL here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )
                        } else {
                            Text(
                                text = "Paste a direct MP4/WebM, HLS (.m3u8), DASH (.mpd), Google Drive share link, official embed URL (YouTube, Vimeo, Dailymotion, Twitch, Loom, Archive.org), or CDN redirect link. Use 'Test Playback' below before publishing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )
                        }

                        VideoSourceSelectorRow(
                            selectedType = videoSourceType,
                            onSelectType = { videoSourceType = it },
                            tagPrefix = "editor_video_type"
                        )

                        OutlinedTextField(
                            value = videoUrl,
                            onValueChange = { videoUrl = it },
                            label = { Text("Video / Webpage URL${if (mediaType == "movie" && !isComingSoon) "*" else ""}") },
                            placeholder = { Text(placeholderForSourceType(videoSourceType)) },
                            isError = videoUrl.isNotBlank() && !videoSourceValidation.isValid,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_video_url_input")
                        )

                        val detectedPreview = remember(videoUrl) {
                            if (videoUrl.isNotBlank()) VideoSourceUtils.detectClassification(videoUrl) else null
                        }
                        OutlinedTextField(
                            value = providerName,
                            onValueChange = { providerName = it.take(80) },
                            label = { Text("Provider Name (Optional)") },
                            placeholder = { Text(detectedPreview?.providerName ?: "e.g. YouTube, Vimeo, Dailymotion, CDN") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_provider_name_input")
                        )

                        if (onUploadMedia != null) {
                            MediaUploadControlRow(
                                label = "Upload MP4 Video to Firebase Storage (Optional)",
                                state = mediaUploadStates["video"],
                                onPickClick = {
                                    onClearUploadState?.invoke("video")
                                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                            )
                        }

                        SourceValidationBadge(
                            url = videoUrl,
                            sourceType = videoSourceType,
                            customProviderName = providerName,
                            required = mediaType == "movie" && !isComingSoon
                        )
                    }
                }

                // TV Show Seasons & Episodes Builder
                if (mediaType == "series") {
                    item {
                        EditorSectionCard(title = "Seasons & Episodes*") {
                            if (isLoadingSeriesSeasons) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VeloraCrimson, strokeWidth = 2.dp)
                                    Text("Loading seasons and episodes from Firestore...", color = VeloraOnSurfaceMuted)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${seasonDrafts.size} Season(s) • ${seasonDrafts.sumOf { it.episodes.size }} Episode(s)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeloraOnSurfaceMuted
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val nextNum = (seasonDrafts.maxOfOrNull { it.season.seasonNumber } ?: 0) + 1
                                            seasonDrafts.add(
                                                SeasonDraft(
                                                    season = Season(
                                                        id = "season-$nextNum",
                                                        seasonNumber = nextNum,
                                                        title = "Season $nextNum",
                                                        year = releaseYear.toIntOrNull() ?: 2026,
                                                        isVisible = true
                                                    ),
                                                    episodes = emptyList()
                                                )
                                            )
                                        },
                                        border = BorderStroke(1.dp, VeloraCrimson),
                                        modifier = Modifier.testTag("editor_add_season_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = VeloraCrimson, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Season", color = VeloraCrimson)
                                    }
                                }

                                seasonDrafts.forEachIndexed { sIndex, draft ->
                                    Surface(
                                        color = VeloraSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, VeloraOutline)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Season ${draft.season.seasonNumber}: ${draft.season.title}",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = VeloraOnBackground
                                                    )
                                                    Text(
                                                        text = "${draft.season.year} • ${draft.episodes.size} episode(s) • ${if (draft.season.isVisible) "Visible" else "Hidden"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = VeloraOnSurfaceMuted
                                                    )
                                                }
                                                Row {
                                                    IconButton(onClick = { editingSeasonIndex = sIndex }) {
                                                        Icon(Icons.Filled.Edit, contentDescription = "Edit season", tint = VeloraOnBackground)
                                                    }
                                                    if (seasonDrafts.size > 1) {
                                                        IconButton(
                                                            onClick = {
                                                                val removed = seasonDrafts.removeAt(sIndex)
                                                                if (removed.season.id.isNotBlank()) {
                                                                    removedSeasonIds.add(removed.season.id)
                                                                }
                                                            }
                                                        ) {
                                                            Icon(Icons.Filled.Delete, contentDescription = "Delete season", tint = VeloraError)
                                                        }
                                                    }
                                                }
                                            }

                                            if (draft.episodes.isEmpty()) {
                                                Text(
                                                    text = "No episodes yet. Tap Add Episode below to attach a YouTube, MP4, or HLS video source.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = VeloraGold
                                                )
                                            } else {
                                                draft.episodes.sortedBy { it.episodeNumber }.forEach { ep ->
                                                    val epSourceType = VideoSourceType.fromKey(ep.videoSourceType)
                                                    Surface(
                                                        color = VeloraSurfaceElevated,
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, VeloraOutline)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(10.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "E${ep.episodeNumber}. ${ep.title}",
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        color = VeloraOnBackground,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f, fill = false)
                                                                    )
                                                                    Surface(
                                                                        color = VeloraCrimson.copy(alpha = 0.16f),
                                                                        shape = RoundedCornerShape(4.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = epSourceType.label,
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = VeloraCoral,
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Text(
                                                                    text = "${ep.runtimeMinutes}m • ${ep.preferredStreamUrl}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = VeloraOnSurfaceMuted,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            Row {
                                                                IconButton(onClick = { editingEpisodeTarget = sIndex to ep }) {
                                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit episode", tint = VeloraOnBackground, modifier = Modifier.size(20.dp))
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        val updatedEps = draft.episodes.filterNot { it === ep }
                                                                        seasonDrafts[sIndex] = draft.copy(episodes = updatedEps)
                                                                        if (draft.season.id.isNotBlank() && ep.id.isNotBlank()) {
                                                                            removedEpisodeRefs.add(draft.season.id to ep.id)
                                                                        }
                                                                    }
                                                                ) {
                                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete episode", tint = VeloraError, modifier = Modifier.size(20.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = { editingEpisodeTarget = sIndex to null },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = VeloraSurfaceBright,
                                                    contentColor = VeloraOnBackground
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("editor_add_episode_s${draft.season.seasonNumber}")
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Add Episode to Season ${draft.season.seasonNumber}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Publishing Flags
                item {
                    EditorSectionCard(title = "Visibility & Placement") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Featured Hero Banner", color = VeloraOnBackground, style = MaterialTheme.typography.bodyLarge)
                                Text("Showcase this title on the Home screen spotlight", color = VeloraOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                        }
                        HorizontalDivider(color = VeloraOutline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Visible to Users", color = VeloraOnBackground, style = MaterialTheme.typography.bodyLarge)
                                Text("Published titles appear across Home, Explore & Search", color = VeloraOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = isVisible, onCheckedChange = { isVisible = it })
                        }
                        HorizontalDivider(color = VeloraOutline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Kids Profile Friendly", color = VeloraOnBackground, style = MaterialTheme.typography.bodyLarge)
                                Text("Allow playback when a Kids profile is active", color = VeloraOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = isKidsFriendly, onCheckedChange = { isKidsFriendly = it })
                        }
                        HorizontalDivider(color = VeloraOutline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Staff Pick", color = VeloraOnBackground, style = MaterialTheme.typography.bodyLarge)
                                Text("Feature in the Staff Picks discovery row", color = VeloraOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = isStaffPick, onCheckedChange = { isStaffPick = it })
                        }
                        HorizontalDivider(color = VeloraOutline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Award-Winning Title", color = VeloraOnBackground, style = MaterialTheme.typography.bodyLarge)
                                Text("Highlight in the Award-Winning Movies shelf", color = VeloraOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = isAwardWinning, onCheckedChange = { isAwardWinning = it })
                        }
                        HorizontalDivider(color = VeloraOutline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Coming Soon (Upcoming Release)", color = VeloraGold, style = MaterialTheme.typography.bodyLarge)
                                Text("Shows release info & trailer only; playback remains locked until release", color = VeloraOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = isComingSoon,
                                onCheckedChange = { isComingSoon = it },
                                modifier = Modifier.testTag("editor_coming_soon_switch")
                            )
                        }
                        if (isComingSoon) {
                            OutlinedTextField(
                                value = expectedReleaseDate,
                                onValueChange = { expectedReleaseDate = it.take(60) },
                                label = { Text("Expected Release Date") },
                                placeholder = { Text("e.g. Jan 22, 2027") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_expected_release_input")
                            )
                        }
                        OutlinedTextField(
                            value = leavingSoonAt,
                            onValueChange = { leavingSoonAt = it.take(60) },
                            label = { Text("Leaving Soon Note (Optional)") },
                            placeholder = { Text("e.g. Leaves Nov 30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (formError != null) {
                    item {
                        Surface(
                            color = VeloraError.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VeloraError.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = VeloraError)
                                Text(formError!!, color = VeloraError, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = ::submitForm,
                        enabled = !isSaving && !isLoadingSeriesSeasons,
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("editor_bottom_save_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Saving to Firestore...")
                        } else {
                            Text(
                                text = if (mediaType == "series") "Save TV Show & Episodes" else "Save Movie",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog: Edit Season metadata
    editingSeasonIndex?.let { sIdx ->
        val draft = seasonDrafts.getOrNull(sIdx)
        if (draft != null) {
            var sNum by remember(sIdx) { mutableStateOf(draft.season.seasonNumber.toString()) }
            var sTitle by remember(sIdx) { mutableStateOf(draft.season.title) }
            var sYear by remember(sIdx) { mutableStateOf(draft.season.year.toString()) }
            var sVisible by remember(sIdx) { mutableStateOf(draft.season.isVisible) }

            AlertDialog(
                onDismissRequest = { editingSeasonIndex = null },
                containerColor = VeloraSurfaceElevated,
                title = { Text("Edit Season", color = VeloraOnBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sNum,
                                onValueChange = { sNum = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Season #") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sYear,
                                onValueChange = { sYear = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("Year") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = sTitle,
                            onValueChange = { sTitle = it },
                            label = { Text("Season Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Visible to Users", color = VeloraOnBackground)
                            Switch(checked = sVisible, onCheckedChange = { sVisible = it })
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newNum = (sNum.toIntOrNull() ?: draft.season.seasonNumber).coerceIn(1, 100)
                            val updatedSeason = draft.season.copy(
                                id = draft.season.id.ifBlank { "season-$newNum" },
                                seasonNumber = newNum,
                                title = sTitle.trim().ifBlank { "Season $newNum" },
                                year = (sYear.toIntOrNull() ?: draft.season.year).coerceIn(1900, 2100),
                                isVisible = sVisible
                            )
                            seasonDrafts[sIdx] = draft.copy(
                                season = updatedSeason,
                                episodes = draft.episodes.map { it.copy(seasonNumber = newNum) }
                            )
                            editingSeasonIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                    ) { Text("Save Season") }
                },
                dismissButton = {
                    TextButton(onClick = { editingSeasonIndex = null }) {
                        Text("Cancel", color = VeloraOnSurfaceMuted)
                    }
                }
            )
        }
    }

    // Sub-dialog: Add / Edit Episode
    editingEpisodeTarget?.let { (sIdx, existingEp) ->
        val draft = seasonDrafts.getOrNull(sIdx)
        if (draft != null) {
            val defaultNextNum = (draft.episodes.maxOfOrNull { it.episodeNumber } ?: 0) + 1
            EpisodeEditorDialog(
                seasonNumber = draft.season.seasonNumber,
                initial = existingEp,
                defaultEpisodeNumber = defaultNextNum,
                fallbackThumbKey = initial?.backdropKey ?: "img_hero_nebula",
                mediaUploadState = mediaUploadStates["episode_${draft.season.seasonNumber}"],
                onUploadEpisodeVideo = onUploadMedia?.let { uploader ->
                    { uri, onUploaded ->
                        uploader(
                            "episode_${draft.season.seasonNumber}",
                            uri,
                            MediaAssetCategory.EPISODE,
                            title.ifBlank { initial?.id ?: "series" },
                            onUploaded
                        )
                    }
                },
                onDismiss = { editingEpisodeTarget = null },
                onConfirm = { savedEp ->
                    val currentEps = draft.episodes.toMutableList()
                    if (existingEp != null) {
                        val idx = currentEps.indexOfFirst { it === existingEp || (it.id.isNotBlank() && it.id == existingEp.id) }
                        if (idx >= 0) currentEps[idx] = savedEp else currentEps.add(savedEp)
                    } else {
                        currentEps.add(savedEp)
                    }
                    seasonDrafts[sIdx] = draft.copy(episodes = currentEps.sortedBy { it.episodeNumber })
                    editingEpisodeTarget = null
                }
            )
        }
    }
}

@Composable
private fun MediaUploadControlRow(
    label: String,
    state: MediaUploadState?,
    onPickClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = onPickClick,
            enabled = state !is MediaUploadState.Uploading,
            border = BorderStroke(1.dp, VeloraOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (state is MediaUploadState.Uploading) state.label else label,
                color = VeloraOnBackground
            )
        }
        when (state) {
            is MediaUploadState.Uploading -> {
                LinearProgressIndicator(
                    progress = { state.progressFraction },
                    color = VeloraCrimson,
                    trackColor = VeloraOutline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is MediaUploadState.Error -> {
                Text(state.message, color = VeloraError, style = MaterialTheme.typography.bodySmall)
            }
            is MediaUploadState.Success -> {
                Text("Uploaded: ${state.fileName}", color = VeloraMatchGreen, style = MaterialTheme.typography.bodySmall)
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoSourceSelectorRow(
    selectedType: VideoSourceType,
    onSelectType: (VideoSourceType) -> Unit,
    tagPrefix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Source Type*", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VideoSourceType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onSelectType(type) },
                    label = { Text(type.label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VeloraCrimson,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("${tagPrefix}_${type.key}")
                )
            }
        }
    }
}

@Composable
fun SourceValidationBadge(
    url: String,
    sourceType: VideoSourceType,
    customProviderName: String? = null,
    required: Boolean
) {
    val trimmed = url.trim()
    if (trimmed.isBlank()) {
        if (required) {
            Text(
                text = "Enter an HTTPS video or webpage URL (${sourceType.label}).",
                style = MaterialTheme.typography.bodySmall,
                color = VeloraOnSurfaceMuted
            )
        }
        return
    }
    val validation = VideoSourceUtils.validateVideoSource(trimmed, sourceType, required = required)
    if (!validation.isValid) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = VeloraError, modifier = Modifier.size(16.dp))
            Text(validation.errorOrNull!!, style = MaterialTheme.typography.bodySmall, color = VeloraError)
        }
        return
    }

    val scope = rememberCoroutineScope()
    var isTesting by remember(trimmed, sourceType, customProviderName) { mutableStateOf(false) }
    var testedResult by remember(trimmed, sourceType, customProviderName) {
        mutableStateOf<ResolvedPlaybackSource?>(null)
    }

    val staticResolved = remember(trimmed, sourceType, customProviderName) {
        VideoSourceUtils.resolveForPlayback(trimmed, sourceType.key, customProviderName)
    }
    val displaySource = testedResult ?: staticResolved

    val isPassed = displaySource.probeVerified &&
        displaySource.validationError == null &&
        (displaySource.isDirectlyPlayable || displaySource.hasOfficialEmbed)
    val isFailed = displaySource.validationError != null ||
        displaySource.detectedKind == DetectedSourceKind.UNSUPPORTED

    val badgeColor = when {
        isFailed -> VeloraError
        displaySource.detectedKind == DetectedSourceKind.ORDINARY_WEBPAGE -> VeloraGold
        else -> VeloraMatchGreen
    }
    val badgeIcon = when {
        isFailed -> Icons.Filled.ErrorOutline
        displaySource.detectedKind == DetectedSourceKind.ORDINARY_WEBPAGE -> Icons.Filled.Info
        else -> Icons.Filled.CheckCircle
    }

    val testStatusLabel = when {
        isTesting -> "Testing playback compatibility..."
        testedResult == null -> "Not tested yet"
        isPassed -> "Passed"
        isFailed -> "Failed"
        else -> "Webpage fallback only"
    }

    Surface(
        color = VeloraSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                Text(
                    text = "${displaySource.providerName ?: "Auto"} • ${displaySource.detectedKind.label}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }

            // Structured compatibility readout for Admin before publishing
            Text(
                text = buildString {
                    append("Direct Playable: ${if (displaySource.isDirectlyPlayable) "Yes" else "No"}")
                    append("  •  Official Embed: ${if (displaySource.hasOfficialEmbed) "Yes" else "No"}")
                    append("\nAuth Required: ${if (displaySource.requiresAuthentication) "Yes" else "No"}")
                    append("  •  Compatibility Test: $testStatusLabel")
                    if (displaySource.probeVerified && displaySource.isDirectlyPlayable) {
                        append("\nSeeking / Byte-Ranges: ${if (displaySource.supportsByteRanges) "Supported" else "Not advertised"}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = VeloraOnSurfaceMuted
            )

            val summaryLine = displaySource.validationError
                ?: displaySource.probeSummary
                ?: displaySource.infoMessage
            if (!summaryLine.isNullOrBlank()) {
                Text(
                    text = summaryLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFailed) VeloraError else badgeColor
                )
            }

            OutlinedButton(
                onClick = {
                    if (!isTesting) {
                        isTesting = true
                        scope.launch {
                            testedResult = PlaybackSourceResolver.resolve(
                                rawUrl = trimmed,
                                declaredType = sourceType.key,
                                providerName = customProviderName
                            )
                            isTesting = false
                        }
                    }
                },
                enabled = !isTesting,
                border = BorderStroke(1.dp, VeloraOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("admin_test_playback_button")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = VeloraCrimson
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Testing Playback...", color = VeloraOnBackground, style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(
                        text = if (testedResult == null) "Test Playback Compatibility" else "Re-test Playback",
                        color = VeloraOnBackground,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeEditorDialog(
    seasonNumber: Int,
    initial: Episode?,
    defaultEpisodeNumber: Int,
    fallbackThumbKey: String,
    mediaUploadState: MediaUploadState? = null,
    onUploadEpisodeVideo: ((Uri, (String) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (Episode) -> Unit
) {
    var epNum by remember { mutableStateOf((initial?.episodeNumber ?: defaultEpisodeNumber).toString()) }
    var epTitle by remember { mutableStateOf(initial?.title ?: "") }
    var epSynopsis by remember { mutableStateOf(initial?.synopsis ?: "") }
    var epRuntime by remember { mutableStateOf((initial?.runtimeMinutes ?: 42).toString()) }
    var epThumbUrl by remember { mutableStateOf(initial?.thumbnailUrl ?: "") }
    var epSourceType by remember {
        mutableStateOf(
            if (initial == null) VideoSourceType.AUTO
            else if (initial.videoSourceType.isNotBlank()) {
                VideoSourceType.fromKey(initial.videoSourceType)
            } else {
                VideoSourceUtils.detectSourceTypeFromUrl(initial.preferredStreamUrl, VideoSourceType.AUTO)
            }
        )
    }
    var epVideoUrl by remember { mutableStateOf(initial?.preferredStreamUrl ?: "") }
    var epProviderName by remember { mutableStateOf(initial?.providerName ?: "") }
    var epVisible by remember { mutableStateOf(initial?.isVisible ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val episodePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && onUploadEpisodeVideo != null) {
            onUploadEpisodeVideo(uri) { uploadedUrl ->
                epVideoUrl = uploadedUrl
                epSourceType = VideoSourceType.MP4
                if (epProviderName.isBlank()) epProviderName = "Firebase Storage"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = {
            Text(
                text = if (initial == null) "Add Episode • Season $seasonNumber" else "Edit Episode • Season $seasonNumber",
                color = VeloraOnBackground
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = epNum,
                        onValueChange = { epNum = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Ep #*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = epRuntime,
                        onValueChange = { epRuntime = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Mins*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = epTitle,
                    onValueChange = { epTitle = it },
                    label = { Text("Episode Title*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ep_editor_title_input")
                )
                OutlinedTextField(
                    value = epSynopsis,
                    onValueChange = { epSynopsis = it },
                    label = { Text("Episode Description*") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ep_editor_description_input")
                )
                OutlinedTextField(
                    value = epThumbUrl,
                    onValueChange = { epThumbUrl = it },
                    label = { Text("Thumbnail URL (Optional)") },
                    placeholder = { Text("https://example.com/thumb.jpg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                VideoSourceSelectorRow(
                    selectedType = epSourceType,
                    onSelectType = { epSourceType = it },
                    tagPrefix = "ep_editor_type"
                )

                OutlinedTextField(
                    value = epVideoUrl,
                    onValueChange = { epVideoUrl = it },
                    label = { Text("Episode Video / Webpage URL*") },
                    placeholder = { Text(placeholderForSourceType(epSourceType)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ep_editor_video_url_input")
                )

                OutlinedTextField(
                    value = epProviderName,
                    onValueChange = { epProviderName = it.take(80) },
                    label = { Text("Provider Name (Optional)") },
                    placeholder = { Text("e.g. YouTube, Vimeo, CDN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onUploadEpisodeVideo != null) {
                    MediaUploadControlRow(
                        label = "Upload Episode Video (MP4)",
                        state = mediaUploadState,
                        onPickClick = {
                            episodePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    )
                }

                SourceValidationBadge(
                    url = epVideoUrl,
                    sourceType = epSourceType,
                    customProviderName = epProviderName,
                    required = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visible to Users", color = VeloraOnBackground)
                    Switch(checked = epVisible, onCheckedChange = { epVisible = it })
                }

                errorMsg?.let { Text(it, color = VeloraError, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedNum = epNum.toIntOrNull()
                    val parsedMins = epRuntime.toIntOrNull()
                    val thumbCheck = VideoSourceUtils.validateOptionalUrl(epThumbUrl, "Thumbnail URL")
                    val videoCheck = VideoSourceUtils.validateVideoSource(epVideoUrl, epSourceType, required = true)

                    when {
                        parsedNum == null || parsedNum < 1 -> errorMsg = "Enter a valid episode number."
                        epTitle.isBlank() -> errorMsg = "Episode title is required."
                        parsedMins == null || parsedMins < 1 -> errorMsg = "Enter episode runtime in minutes."
                        !thumbCheck.isValid -> errorMsg = thumbCheck.errorOrNull
                        !videoCheck.isValid -> errorMsg = videoCheck.errorOrNull
                        else -> {
                            val trimmedUrl = epVideoUrl.trim()
                            val kind = VideoSourceUtils.detectClassification(trimmedUrl, epProviderName).detectedKind
                            onConfirm(
                                (initial ?: Episode()).copy(
                                    id = initial?.id?.ifBlank { "s${seasonNumber}e$parsedNum" } ?: "s${seasonNumber}e$parsedNum",
                                    seasonNumber = seasonNumber,
                                    episodeNumber = parsedNum,
                                    title = epTitle.trim(),
                                    synopsis = epSynopsis.trim().ifBlank { "Episode $parsedNum" },
                                    runtimeMinutes = parsedMins,
                                    thumbnailKey = initial?.thumbnailKey ?: fallbackThumbKey,
                                    thumbnailUrl = epThumbUrl.trim().takeIf { it.isNotBlank() },
                                    videoUrl = trimmedUrl,
                                    videoSourceType = epSourceType.key,
                                    providerName = epProviderName.trim().takeIf { it.isNotBlank() },
                                    hlsUrl = if (epSourceType == VideoSourceType.HLS || kind == DetectedSourceKind.HLS_STREAM) trimmedUrl else null,
                                    isVisible = epVisible
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                modifier = Modifier.testTag("ep_editor_save_button")
            ) { Text("Save Episode") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VeloraOnSurfaceMuted) }
        }
    )
}

@Composable
private fun EditorSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = VeloraSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, VeloraOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VeloraGold
            )
            content()
        }
    }
}

fun placeholderForSourceType(type: VideoSourceType): String {
    return when (type) {
        VideoSourceType.AUTO -> "https://youtu.be/..., Drive link, .mp4, .m3u8, or .mpd"
        VideoSourceType.EMBED -> "https://vimeo.com/..., https://www.youtube.com/watch?v=..., or embed URL"
        VideoSourceType.MP4 -> "https://example.com/videos/feature.mp4"
        VideoSourceType.HLS -> "https://example.com/streams/master.m3u8"
        VideoSourceType.DASH -> "https://example.com/streams/manifest.mpd"
    }
}
