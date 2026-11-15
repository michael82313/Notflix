package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.Episode
import com.example.data.model.PlaybackProgress
import com.example.data.model.Season
import com.example.ui.components.CatalogImage
import com.example.ui.components.PosterCard
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
    item: CatalogItem,
    inWatchlist: Boolean,
    rating: ContentRating?,
    progressList: List<PlaybackProgress>,
    similarTitles: List<CatalogItem>,
    seasonsFlow: Flow<List<Season>>,
    episodesFlowProvider: (seasonId: String, seasonNumber: Int) -> Flow<List<Episode>>,
    onBack: () -> Unit,
    onPlayMovieOrDefault: () -> Unit,
    onPlayTrailer: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onToggleWatchlist: () -> Unit,
    onLikeDislike: (Int) -> Unit,
    onStarRate: (Int) -> Unit,
    onOpenDetails: (CatalogItem) -> Unit
) {
    val seasons by seasonsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedSeasonId by remember(seasons) { mutableStateOf(seasons.firstOrNull()?.id ?: "") }
    val activeSeason = seasons.firstOrNull { it.id == selectedSeasonId } ?: seasons.firstOrNull()
    var showSeasonMenu by remember { mutableStateOf(false) }

    val episodesFlow = remember(activeSeason?.id, activeSeason?.seasonNumber) {
        if (activeSeason != null) episodesFlowProvider(activeSeason.id, activeSeason.seasonNumber) else null
    }
    val episodes = if (episodesFlow != null) {
        episodesFlow.collectAsStateWithLifecycle(initialValue = emptyList()).value
    } else emptyList()

    val itemProgress = remember(progressList, item.id) {
        progressList.firstOrNull { it.contentId == item.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraBackground),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Backdrop Header + Poster + Title
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                CatalogImage(
                    imageUrl = item.backdropUrl,
                    fallbackKey = item.backdropKey,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to VeloraBackground.copy(alpha = 0.55f),
                                0.5f to VeloraBackground.copy(alpha = 0.35f),
                                1f to VeloraBackground
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    CatalogImage(
                        imageUrl = item.posterUrl,
                        fallbackKey = item.posterKey,
                        contentDescription = "${item.title} poster",
                        modifier = Modifier
                            .width(96.dp)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = VeloraOnBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(item.trendingScore / 10).coerceIn(84, 99)}% Match",
                                style = MaterialTheme.typography.labelLarge,
                                color = VeloraMatchGreen
                            )
                            Text("${item.releaseYear}", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                            Surface(color = VeloraSurfaceBright, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = item.ageRating,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeloraOnBackground,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = if (item.isSeries) "${seasons.size.coerceAtLeast(1)} Seasons" else "${item.runtimeMinutes}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )
                        }
                    }
                }
            }
        }

        // Primary CTAs + Synopsis + Cast + Ratings
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = {
                        if (item.isSeries && episodes.isNotEmpty()) {
                            val resumeEp = itemProgress?.episodeId?.let { eid -> episodes.firstOrNull { it.id == eid } }
                            onPlayEpisode(resumeEp ?: episodes.first())
                        } else {
                            onPlayMovieOrDefault()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("details_play_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (itemProgress != null && !itemProgress.completed) "Resume Playback" else "Play Now",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleWatchlist,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("details_my_list_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = VeloraSurfaceElevated,
                            contentColor = VeloraOnBackground
                        )
                    ) {
                        Icon(if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (inWatchlist) "In My List" else "My List")
                    }

                    if (!item.trailerUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = onPlayTrailer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("details_trailer_button"),
                            border = BorderStroke(1.dp, VeloraOutline)
                        ) {
                            Icon(Icons.Filled.Theaters, contentDescription = null, tint = VeloraCoral)
                            Spacer(Modifier.width(6.dp))
                            Text("Trailer", color = VeloraOnBackground)
                        }
                    }
                }

                Text(
                    text = item.synopsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VeloraOnBackground.copy(alpha = 0.9f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Director: ${item.director}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeloraOnSurfaceMuted
                    )
                    if (item.cast.isNotEmpty()) {
                        Text(
                            text = "Cast: ${item.cast.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.genres.forEach { genre ->
                        AssistChip(
                            onClick = {},
                            label = { Text(genre) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = VeloraSurfaceElevated,
                                labelColor = VeloraOnBackground
                            ),
                            border = BorderStroke(1.dp, VeloraOutline)
                        )
                    }
                }

                // Like / Dislike & Star Rating Bar
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val liked = rating?.liked ?: 0
                            IconButton(
                                onClick = { onLikeDislike(if (liked == 1) 0 else 1) },
                                modifier = Modifier.testTag("details_like_button")
                            ) {
                                Icon(
                                    imageVector = if (liked == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (liked == 1) VeloraMatchGreen else VeloraOnSurfaceMuted
                                )
                            }
                            IconButton(
                                onClick = { onLikeDislike(if (liked == -1) 0 else -1) },
                                modifier = Modifier.testTag("details_dislike_button")
                            ) {
                                Icon(
                                    imageVector = if (liked == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                    contentDescription = "Dislike",
                                    tint = if (liked == -1) VeloraCrimson else VeloraOnSurfaceMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val stars = rating?.stars ?: 0
                            (1..5).forEach { idx ->
                                IconButton(
                                    onClick = { onStarRate(idx) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (idx <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "Rate $idx stars",
                                        tint = if (idx <= stars) VeloraGold else VeloraOnSurfaceMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Episodes Section for TV Series
        if (item.isSeries && seasons.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.titleLarge, color = VeloraOnBackground)
                        Box {
                            Surface(
                                color = VeloraSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, VeloraOutline),
                                modifier = Modifier.clickable { showSeasonMenu = true }
                            ) {
                                Text(
                                    text = activeSeason?.title ?: "Season 1",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VeloraOnBackground,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showSeasonMenu,
                                onDismissRequest = { showSeasonMenu = false },
                                containerColor = VeloraSurfaceElevated
                            ) {
                                seasons.forEach { season ->
                                    DropdownMenuItem(
                                        text = { Text(season.title, color = VeloraOnBackground) },
                                        onClick = {
                                            selectedSeasonId = season.id
                                            showSeasonMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(episodes, key = { it.id }) { ep ->
                val epProgress = progressList.firstOrNull { it.contentId == item.id && it.episodeId == ep.id }
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VeloraOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onPlayEpisode(ep) }
                        .testTag("episode_card_${ep.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(116.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                CatalogImage(
                                    imageUrl = ep.thumbnailUrl,
                                    fallbackKey = ep.thumbnailKey,
                                    contentDescription = ep.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play episode ${ep.episodeNumber}",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "${ep.episodeNumber}. ${ep.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VeloraOnBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ep.runtimeMinutes}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeloraOnSurfaceMuted
                                )
                                Text(
                                    text = ep.synopsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloraOnSurfaceMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (epProgress != null && epProgress.progressFraction > 0f) {
                            LinearProgressIndicator(
                                progress = { epProgress.progressFraction },
                                color = VeloraCrimson,
                                trackColor = VeloraSurfaceBright,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                            )
                        }
                    }
                }
            }
        }

        // More Like This
        if (similarTitles.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "More Like This",
                        style = MaterialTheme.typography.titleLarge,
                        color = VeloraOnBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(similarTitles, key = { "similar_${it.id}" }) { sim ->
                            PosterCard(
                                item = sim,
                                onClick = { onOpenDetails(sim) },
                                onQuickPreview = { onOpenDetails(sim) }
                            )
                        }
                    }
                }
            }
        }
    }
}
