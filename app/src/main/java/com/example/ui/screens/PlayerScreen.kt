package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.data.model.Episode
import com.example.ui.components.HostedEmbedWebPlayer
import com.example.ui.components.YouTubeEmbedPlayer
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated
import com.example.util.DetectedSourceKind
import com.example.util.PlaybackFailureCategory
import com.example.util.PlaybackSourceResolver
import com.example.util.ResolvedPlaybackSource
import com.example.util.VideoSourceType
import com.example.util.VideoSourceUtils
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    contentId: String,
    title: String,
    subtitleLabel: String?,
    streamUrl: String,
    streamSourceType: String? = null,
    streamProviderName: String? = null,
    episodeId: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    initialPositionMs: Long,
    defaultQuality: String,
    defaultSubtitlesEnabled: Boolean,
    autoplayNext: Boolean,
    nextEpisode: Episode?,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onPlayNextEpisode: (Episode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val staticResolved = remember(streamUrl, streamSourceType, streamProviderName) {
        VideoSourceUtils.resolveForPlayback(streamUrl, streamSourceType, streamProviderName)
    }
    var resolveRetryToken by remember(streamUrl, streamSourceType, streamProviderName) { mutableIntStateOf(0) }
    var isResolvingSource by remember(streamUrl, streamSourceType, streamProviderName, resolveRetryToken) {
        mutableStateOf(staticResolved.validationError == null && staticResolved.detectedKind != DetectedSourceKind.UNSUPPORTED)
    }
    var resolvedSource by remember(streamUrl, streamSourceType, streamProviderName, resolveRetryToken) {
        mutableStateOf<ResolvedPlaybackSource>(staticResolved)
    }
    var forceEmbedFallback by remember(streamUrl, streamSourceType, streamProviderName, resolveRetryToken) {
        mutableStateOf(false)
    }

    // Centralized async multi-source resolution (probes headers, redirects, Google Drive, DASH/HLS MIME, YouTube oEmbed)
    LaunchedEffect(streamUrl, streamSourceType, streamProviderName, resolveRetryToken) {
        if (staticResolved.validationError != null || staticResolved.detectedKind == DetectedSourceKind.UNSUPPORTED) {
            resolvedSource = staticResolved
            isResolvingSource = false
            return@LaunchedEffect
        }
        isResolvingSource = true
        val probed = PlaybackSourceResolver.resolve(
            rawUrl = streamUrl,
            declaredType = streamSourceType,
            providerName = streamProviderName
        )
        resolvedSource = probed
        isResolvingSource = false
    }

    var isFullscreen by remember { mutableStateOf(false) }
    var nextEpisodeCountdown by remember(streamUrl, episodeId) { mutableIntStateOf(-1) }

    // Fullscreen & orientation management
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Next Episode Countdown
    LaunchedEffect(nextEpisodeCountdown) {
        if (nextEpisodeCountdown > 0) {
            delay(1000L)
            nextEpisodeCountdown--
        } else if (nextEpisodeCountdown == 0 && nextEpisode != null) {
            nextEpisodeCountdown = -1
            onPlayNextEpisode(nextEpisode)
        }
    }

    // Checking / Resolving video source headers & redirects
    if (isResolvingSource) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .testTag("player_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = VeloraCrimson)
                Text(
                    text = "Preparing ${staticResolved.providerName ?: "video"} source...",
                    style = MaterialTheme.typography.titleMedium,
                    color = VeloraOnBackground
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloraOnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    val activeSource = resolvedSource

    // Source validation failure, network probe failure, Drive access restriction, or unsupported URL UI
    if (activeSource.validationError != null || activeSource.detectedKind == DetectedSourceKind.UNSUPPORTED) {
        val canOpenOriginal = activeSource.originalUrl.startsWith("https://") ||
            activeSource.originalUrl.startsWith("http://")
        val canRetryProbe = activeSource.failureCategory != PlaybackFailureCategory.INVALID_URL
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PlaybackErrorCard(
                title = activeSource.failureCategory?.title ?: "Playback Unavailable",
                message = activeSource.validationError ?: "Unsupported video source.",
                sourceBadge = activeSource.providerName ?: activeSource.sourceType.label,
                canRetry = canRetryProbe,
                onRetry = { resolveRetryToken++ },
                onOpenWebsite = if (canOpenOriginal) {
                    { openExternalWebsite(context, activeSource.originalUrl) }
                } else null,
                openWebsiteLabel = activeSource.openExternallyLabel,
                onBack = onBack
            )
        }
        return
    }

    // Ordinary webpage (e.g., Flystream or HTML portal without direct video stream or official embed API)
    if (activeSource.detectedKind == DetectedSourceKind.ORDINARY_WEBPAGE) {
        OrdinaryWebpageViewingCard(
            title = title,
            subtitleLabel = subtitleLabel,
            providerName = activeSource.providerName ?: "External Website",
            explanation = activeSource.infoMessage
                ?: "This source points to an ordinary webpage rather than a direct video stream or supported embedded player.",
            targetUrl = activeSource.originalUrl,
            openWebsiteLabel = activeSource.openExternallyLabel,
            nextEpisode = nextEpisode,
            onRetry = { resolveRetryToken++ },
            onOpenWebsite = { openExternalWebsite(context, activeSource.originalUrl) },
            onPlayNextEpisode = onPlayNextEpisode,
            onBack = onBack
        )
        return
    }

    // YouTube official IFrame Player API
    if (activeSource.youtubeVideoId != null) {
        YouTubePlayerHost(
            title = title,
            subtitleLabel = subtitleLabel,
            videoId = activeSource.youtubeVideoId,
            originalUrl = activeSource.originalUrl,
            initialPositionMs = initialPositionMs,
            autoplayNext = autoplayNext,
            isFullscreen = isFullscreen,
            nextEpisode = nextEpisode,
            nextEpisodeCountdown = nextEpisodeCountdown,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onSaveProgress = onSaveProgress,
            onTriggerNextCountdown = { nextEpisodeCountdown = 5 },
            onCancelNextCountdown = { nextEpisodeCountdown = -1 },
            onOpenWebsite = { openExternalWebsite(context, activeSource.originalUrl) },
            onPlayNextEpisode = onPlayNextEpisode,
            onBack = onBack
        )
        return
    }

    // Compatible external host official embedded player (Vimeo, Dailymotion, Twitch, Google Drive /preview fallback, etc.)
    if ((forceEmbedFallback || activeSource.detectedKind == DetectedSourceKind.SUPPORTED_EMBED) &&
        !activeSource.embedUrl.isNullOrBlank()
    ) {
        HostedEmbedPlayerHost(
            title = title,
            subtitleLabel = subtitleLabel,
            providerName = activeSource.providerName ?: "Embedded Player",
            embedUrl = activeSource.embedUrl,
            originalUrl = activeSource.originalUrl,
            openWebsiteLabel = activeSource.openExternallyLabel,
            isFullscreen = isFullscreen,
            nextEpisode = nextEpisode,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onOpenWebsite = { openExternalWebsite(context, activeSource.originalUrl) },
            onPlayNextEpisode = onPlayNextEpisode,
            onBack = onBack
        )
        return
    }

    ExoDirectVideoPlayerHost(
        title = title,
        subtitleLabel = subtitleLabel,
        streamUrl = activeSource.url,
        originalUrl = activeSource.originalUrl,
        sourceType = activeSource.sourceType,
        resolvedMimeType = activeSource.mimeType,
        providerName = activeSource.providerName,
        openWebsiteLabel = activeSource.openExternallyLabel,
        fallbackEmbedUrl = activeSource.embedUrl,
        onSwitchToFallbackEmbed = if (!activeSource.embedUrl.isNullOrBlank()) {
            { forceEmbedFallback = true }
        } else null,
        initialPositionMs = initialPositionMs,
        defaultQuality = defaultQuality,
        defaultSubtitlesEnabled = defaultSubtitlesEnabled,
        autoplayNext = autoplayNext,
        isFullscreen = isFullscreen,
        nextEpisode = nextEpisode,
        nextEpisodeCountdown = nextEpisodeCountdown,
        onToggleFullscreen = { isFullscreen = !isFullscreen },
        onSaveProgress = onSaveProgress,
        onTriggerNextCountdown = { nextEpisodeCountdown = 5 },
        onCancelNextCountdown = { nextEpisodeCountdown = -1 },
        onOpenWebsite = { openExternalWebsite(context, activeSource.originalUrl) },
        onPlayNextEpisode = onPlayNextEpisode,
        onBack = onBack
    )
}

@Composable
private fun YouTubePlayerHost(
    title: String,
    subtitleLabel: String?,
    videoId: String,
    originalUrl: String,
    initialPositionMs: Long,
    autoplayNext: Boolean,
    isFullscreen: Boolean,
    nextEpisode: Episode?,
    nextEpisodeCountdown: Int,
    onToggleFullscreen: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onTriggerNextCountdown: () -> Unit,
    onCancelNextCountdown: () -> Unit,
    onOpenWebsite: () -> Unit,
    onPlayNextEpisode: (Episode) -> Unit,
    onBack: () -> Unit
) {
    var reloadToken by remember { mutableIntStateOf(0) }
    var isLoading by remember(videoId, reloadToken) { mutableStateOf(true) }
    var ytError by remember(videoId, reloadToken) { mutableStateOf<String?>(null) }
    var latestPosMs by remember(videoId) { mutableLongStateOf(initialPositionMs) }
    var latestDurMs by remember(videoId) { mutableLongStateOf(0L) }
    var lastSavedTickMs by remember(videoId) { mutableLongStateOf(0L) }

    DisposableEffect(videoId) {
        onDispose {
            if (latestDurMs > 0L) {
                onSaveProgress(latestPosMs, latestDurMs)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        YouTubeEmbedPlayer(
            videoId = videoId,
            initialPositionMs = initialPositionMs,
            reloadToken = reloadToken,
            onReady = { dur ->
                isLoading = false
                if (dur > 0L) latestDurMs = dur
            },
            onProgress = { pos, dur, _ ->
                latestPosMs = pos
                if (dur > 0L) latestDurMs = dur
                if (dur > 0L && kotlin.math.abs(pos - lastSavedTickMs) >= 5000L) {
                    lastSavedTickMs = pos
                    onSaveProgress(pos, dur)
                }
            },
            onEnded = { dur ->
                if (dur > 0L) {
                    latestDurMs = dur
                    onSaveProgress(dur, dur)
                }
                if (autoplayNext && nextEpisode != null) {
                    onTriggerNextCountdown()
                }
            },
            onError = { message ->
                isLoading = false
                ytError = message
            }
        )

        // Top non-obstructive bar above the YouTube embedded iframe
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = {
                        if (latestDurMs > 0L) onSaveProgress(latestPosMs, latestDurMs)
                        onBack()
                    },
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    if (!subtitleLabel.isNullOrBlank()) {
                        Text(subtitleLabel, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = VeloraCrimson.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, VeloraCrimson.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "YouTube",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (nextEpisode != null) {
                    TextButton(
                        onClick = {
                            if (latestDurMs > 0L) onSaveProgress(latestPosMs, latestDurMs)
                            onPlayNextEpisode(nextEpisode)
                        },
                        modifier = Modifier.testTag("next_episode_button")
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Next Ep", color = Color.White)
                    }
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        if (isLoading && ytError == null) {
            CircularProgressIndicator(
                color = VeloraCrimson,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (ytError != null) {
            Surface(
                color = VeloraSurfaceElevated.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VeloraOutline),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("youtube_error_banner")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = VeloraError,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "YouTube Embedded Playback Notice",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { ytError = null }) {
                            Text("Dismiss", color = VeloraOnSurfaceMuted)
                        }
                    }
                    Text(
                        text = ytError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                ytError = null
                                isLoading = true
                                reloadToken++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                        if (originalUrl.startsWith("https://")) {
                            OutlinedButton(onClick = onOpenWebsite) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Open on YouTube", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (nextEpisodeCountdown >= 0 && nextEpisode != null) {
            NextEpisodeCountdownCard(
                nextEpisode = nextEpisode,
                countdownSeconds = nextEpisodeCountdown,
                onCancel = onCancelNextCountdown,
                onPlayNow = { onPlayNextEpisode(nextEpisode) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            )
        }
    }
}

@Composable
private fun HostedEmbedPlayerHost(
    title: String,
    subtitleLabel: String?,
    providerName: String,
    embedUrl: String,
    originalUrl: String,
    openWebsiteLabel: String = "Open Website",
    isFullscreen: Boolean,
    nextEpisode: Episode?,
    onToggleFullscreen: () -> Unit,
    onOpenWebsite: () -> Unit,
    onPlayNextEpisode: (Episode) -> Unit,
    onBack: () -> Unit
) {
    var reloadToken by remember { mutableIntStateOf(0) }
    var isLoading by remember(embedUrl, reloadToken) { mutableStateOf(true) }
    var embedError by remember(embedUrl, reloadToken) { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HostedEmbedWebPlayer(
            embedUrl = embedUrl,
            providerName = providerName,
            reloadToken = reloadToken,
            onLoaded = { isLoading = false },
            onError = { message ->
                isLoading = false
                embedError = message
            }
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitleLabel.isNullOrBlank()) {
                        Text(
                            text = subtitleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = VeloraCrimson.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, VeloraCrimson.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = providerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (originalUrl.startsWith("https://")) {
                    IconButton(
                        onClick = onOpenWebsite,
                        modifier = Modifier.testTag("embed_open_website_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = openWebsiteLabel,
                            tint = Color.White
                        )
                    }
                }
                if (nextEpisode != null) {
                    TextButton(
                        onClick = { onPlayNextEpisode(nextEpisode) },
                        modifier = Modifier.testTag("next_episode_button")
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Next Ep", color = Color.White)
                    }
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Toggle fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        if (isLoading && embedError == null) {
            CircularProgressIndicator(
                color = VeloraCrimson,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (embedError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                PlaybackErrorCard(
                    title = "$providerName Playback Unavailable",
                    message = embedError!!,
                    sourceBadge = providerName,
                    canRetry = true,
                    onRetry = {
                        embedError = null
                        isLoading = true
                        reloadToken++
                    },
                    onOpenWebsite = if (originalUrl.startsWith("https://")) onOpenWebsite else null,
                    openWebsiteLabel = openWebsiteLabel,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun OrdinaryWebpageViewingCard(
    title: String,
    subtitleLabel: String?,
    providerName: String,
    explanation: String,
    targetUrl: String,
    openWebsiteLabel: String = "Open Website",
    nextEpisode: Episode?,
    onRetry: () -> Unit,
    onOpenWebsite: () -> Unit,
    onPlayNextEpisode: (Episode) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!subtitleLabel.isNullOrBlank()) {
                        Text(subtitleLabel, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (nextEpisode != null) {
                TextButton(onClick = { onPlayNextEpisode(nextEpisode) }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Next Ep", color = Color.White)
                }
            }
        }

        Surface(
            color = VeloraSurfaceElevated,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, VeloraOutline),
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = VeloraGold,
                    modifier = Modifier.size(46.dp)
                )
                Surface(
                    color = VeloraGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$providerName • Browser Playback",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeloraGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Watch on $providerName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VeloraOnBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeloraOnSurfaceMuted,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = targetUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloraOnSurfaceMuted.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onOpenWebsite,
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_website_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(openWebsiteLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        border = BorderStroke(1.dp, VeloraOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("webpage_back_button")
                    ) {
                        Text("Go Back", color = VeloraOnBackground)
                    }
                    OutlinedButton(
                        onClick = onRetry,
                        border = BorderStroke(1.dp, VeloraOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("webpage_retry_button")
                    ) {
                        Text("Retry", color = VeloraOnBackground)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoDirectVideoPlayerHost(
    title: String,
    subtitleLabel: String?,
    streamUrl: String,
    originalUrl: String,
    sourceType: VideoSourceType,
    resolvedMimeType: String?,
    providerName: String?,
    openWebsiteLabel: String,
    fallbackEmbedUrl: String?,
    onSwitchToFallbackEmbed: (() -> Unit)?,
    initialPositionMs: Long,
    defaultQuality: String,
    defaultSubtitlesEnabled: Boolean,
    autoplayNext: Boolean,
    isFullscreen: Boolean,
    nextEpisode: Episode?,
    nextEpisodeCountdown: Int,
    onToggleFullscreen: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onTriggerNextCountdown: () -> Unit,
    onCancelNextCountdown: () -> Unit,
    onOpenWebsite: () -> Unit,
    onPlayNextEpisode: (Episode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember(streamUrl) { mutableStateOf<String?>(null) }
    var currentPosMs by remember { mutableLongStateOf(initialPositionMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var volume by remember { mutableFloatStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf(defaultQuality) }
    var subtitlesEnabled by remember { mutableStateOf(defaultSubtitlesEnabled) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var selectedAudioLang by remember { mutableStateOf("English (Original)") }

    val exoPlayer = remember(streamUrl, sourceType, resolvedMimeType) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setUserAgent("NotflixAndroid/9.0 (Linux; Android)")

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
            .apply {
                val mediaItemBuilder = MediaItem.Builder().setUri(streamUrl)
                when {
                    sourceType == VideoSourceType.HLS ||
                        streamUrl.contains(".m3u8", ignoreCase = true) ||
                        resolvedMimeType.equals(MimeTypes.APPLICATION_M3U8, ignoreCase = true) ||
                        resolvedMimeType.equals("application/x-mpegurl", ignoreCase = true) ->
                        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                    sourceType == VideoSourceType.DASH ||
                        streamUrl.contains(".mpd", ignoreCase = true) ||
                        resolvedMimeType.equals(MimeTypes.APPLICATION_MPD, ignoreCase = true) ->
                        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                    !resolvedMimeType.isNullOrBlank() && resolvedMimeType.startsWith("video/") ->
                        mediaItemBuilder.setMimeType(resolvedMimeType)
                }
                setMediaItem(mediaItemBuilder.build())
                applyQualityConstraint(this, selectedQuality, subtitlesEnabled)
                prepare()
                if (initialPositionMs > 2000L) {
                    seekTo(initialPositionMs)
                }
                playWhenReady = true
            }
    }

    // Track listener + periodic position sync
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
                if (playbackState == Player.STATE_ENDED) {
                    onSaveProgress(exoPlayer.duration.coerceAtLeast(0L), exoPlayer.duration.coerceAtLeast(0L))
                    if (autoplayNext && nextEpisode != null) {
                        onTriggerNextCountdown()
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = formatPlaybackException(error, sourceType)
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onSaveProgress(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.duration.coerceAtLeast(0L))
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll current position & periodically persist progress every 5s
    LaunchedEffect(exoPlayer) {
        var ticks = 0
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0L) durationMs = dur
            ticks++
            if (ticks % 5 == 0 && durationMs > 0L && exoPlayer.isPlaying) {
                onSaveProgress(currentPosMs, durationMs)
            }
            delay(1000L)
        }
    }

    // Auto-hide controls while playing
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4500L)
            controlsVisible = false
        }
    }

    val showSkipIntro = currentPosMs in 5_000L..45_000L && durationMs > 60_000L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Spacebar -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            true
                        }
                        Key.DirectionLeft -> {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                            true
                        }
                        Key.DirectionRight -> {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L)
                            true
                        }
                        Key.M -> {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else volume
                            true
                        }
                        Key.F -> {
                            onToggleFullscreen()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view -> view.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && playerError == null) {
            CircularProgressIndicator(
                color = VeloraCrimson,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                PlaybackErrorCard(
                    title = "Playback Unavailable",
                    message = playerError!!,
                    sourceBadge = providerName ?: sourceType.label,
                    canRetry = true,
                    onRetry = {
                        playerError = null
                        isBuffering = true
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
                    onFallbackEmbed = if (!fallbackEmbedUrl.isNullOrBlank() && onSwitchToFallbackEmbed != null) {
                        onSwitchToFallbackEmbed
                    } else null,
                    fallbackEmbedLabel = "Try ${providerName ?: "Provider"} Preview Player",
                    onOpenWebsite = if (originalUrl.startsWith("https://")) onOpenWebsite else null,
                    openWebsiteLabel = openWebsiteLabel,
                    onBack = onBack
                )
            }
        }

        // Subtitle Banner Preview when captions enabled
        if (subtitlesEnabled && isPlaying && playerError == null) {
            val captionLine = when {
                currentPosMs < 15_000L -> "[$title — Audio: $selectedAudioLang]"
                currentPosMs < 35_000L -> "Captions active • ${sourceType.label} stream ready"
                else -> null
            }
            if (captionLine != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (controlsVisible) 138.dp else 36.dp)
                ) {
                    Text(
                        text = captionLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Skip Intro Floating Button
        AnimatedVisibility(
            visible = showSkipIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = if (controlsVisible) 144.dp else 40.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable {
                        exoPlayer.seekTo(46_000L)
                        controlsVisible = true
                    }
                    .testTag("skip_intro_button")
            ) {
                Text(
                    text = "Skip Intro",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        if (nextEpisodeCountdown >= 0 && nextEpisode != null) {
            NextEpisodeCountdownCard(
                nextEpisode = nextEpisode,
                countdownSeconds = nextEpisodeCountdown,
                onCancel = onCancelNextCountdown,
                onPlayNow = { onPlayNextEpisode(nextEpisode) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }

        // Custom Cinema HUD Controls
        AnimatedVisibility(
            visible = controlsVisible && playerError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
                                onBack()
                            },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column {
                            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            if (!subtitleLabel.isNullOrBlank()) {
                                Text(subtitleLabel, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else volume
                        }) {
                            Icon(
                                imageVector = if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle mute",
                                tint = Color.White
                            )
                        }
                        Box(Modifier.width(84.dp)) {
                            Slider(
                                value = if (isMuted) 0f else volume,
                                onValueChange = {
                                    volume = it
                                    isMuted = it == 0f
                                    exoPlayer.volume = it
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = VeloraCrimson
                                )
                            )
                        }
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Toggle fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VeloraCrimson)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Bottom Scrub Bar & Track/Speed/Quality Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(formatTimeMs(currentPosMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Slider(
                            value = if (durationMs > 0L) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { fraction ->
                                if (durationMs > 0L) {
                                    val seekPos = (fraction * durationMs).toLong()
                                    currentPosMs = seekPos
                                    exoPlayer.seekTo(seekPos)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = VeloraCrimson,
                                activeTrackColor = VeloraCrimson,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Text(formatTimeMs(durationMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box {
                                TextButton(onClick = { showSpeedMenu = true }) {
                                    Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${playbackSpeed}x", color = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false },
                                    containerColor = VeloraSurfaceElevated
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                        DropdownMenuItem(
                                            text = { Text("${spd}x", color = VeloraOnBackground) },
                                            onClick = {
                                                playbackSpeed = spd
                                                exoPlayer.setPlaybackSpeed(spd)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box {
                                TextButton(onClick = { showQualityMenu = true }) {
                                    Icon(Icons.Filled.HighQuality, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(selectedQuality, color = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                    containerColor = VeloraSurfaceElevated
                                ) {
                                    listOf("Auto", "1080p", "720p", "480p").forEach { q ->
                                        DropdownMenuItem(
                                            text = { Text(q, color = VeloraOnBackground) },
                                            onClick = {
                                                selectedQuality = q
                                                applyQualityConstraint(exoPlayer, q, subtitlesEnabled)
                                                showQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = { showTracksDialog = true }) {
                                Icon(
                                    imageVector = if (subtitlesEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionOff,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Audio & Subs", color = Color.White)
                            }
                        }

                        if (nextEpisode != null) {
                            TextButton(
                                onClick = {
                                    onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
                                    onPlayNextEpisode(nextEpisode)
                                },
                                modifier = Modifier.testTag("next_episode_button")
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Next Ep", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTracksDialog) {
        AlertDialog(
            onDismissRequest = { showTracksDialog = false },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Audio & Subtitles", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audio Track", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                    listOf("English (Original)", "Spanish (Dubbed)", "French (Dubbed)").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedAudioLang = lang
                                    val code = when {
                                        lang.startsWith("Spanish") -> "es"
                                        lang.startsWith("French") -> "fr"
                                        else -> "en"
                                    }
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setPreferredAudioLanguage(code)
                                        .build()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang, color = VeloraOnBackground)
                            if (selectedAudioLang == lang) {
                                Text("Active", color = VeloraCrimson, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Subtitles / Captions", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                subtitlesEnabled = true
                                applyQualityConstraint(exoPlayer, selectedQuality, true)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (subtitlesEnabled) VeloraCrimson else VeloraOutline
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("English CC") }
                        Button(
                            onClick = {
                                subtitlesEnabled = false
                                applyQualityConstraint(exoPlayer, selectedQuality, false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!subtitlesEnabled) VeloraCrimson else VeloraOutline
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Off") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTracksDialog = false }) {
                    Text("Done", color = VeloraCrimson)
                }
            }
        )
    }
}

@Composable
private fun PlaybackErrorCard(
    title: String,
    message: String,
    sourceBadge: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onFallbackEmbed: (() -> Unit)? = null,
    fallbackEmbedLabel: String = "Try Embedded Preview",
    onOpenWebsite: (() -> Unit)? = null,
    openWebsiteLabel: String = "Open Website",
    onBack: () -> Unit
) {
    Surface(
        color = VeloraSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, VeloraOutline),
        modifier = Modifier.widthIn(max = 480.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = VeloraError,
                modifier = Modifier.size(44.dp)
            )
            Surface(
                color = VeloraCrimson.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = sourceBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = VeloraCrimson,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = VeloraOnBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = VeloraOnSurfaceMuted,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("player_error_back_button"),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Text("Go Back", color = VeloraOnBackground)
                }
                if (canRetry) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("player_error_retry_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
            if (onFallbackEmbed != null) {
                OutlinedButton(
                    onClick = onFallbackEmbed,
                    border = BorderStroke(1.dp, VeloraCrimson.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("player_error_fallback_embed_button")
                ) {
                    Text(fallbackEmbedLabel, color = VeloraOnBackground, fontWeight = FontWeight.SemiBold)
                }
            }
            if (onOpenWebsite != null) {
                OutlinedButton(
                    onClick = onOpenWebsite,
                    border = BorderStroke(1.dp, VeloraGold.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("player_error_open_website_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(openWebsiteLabel, color = VeloraGold)
                }
            }
        }
    }
}

private fun openExternalWebsite(context: Context, url: String) {
    val clean = url.trim()
    if (clean.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
    }
}

@Composable
private fun NextEpisodeCountdownCard(
    nextEpisode: Episode,
    countdownSeconds: Int,
    onCancel: () -> Unit,
    onPlayNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VeloraSurfaceElevated.copy(alpha = 0.95f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, VeloraOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Next Episode in ${countdownSeconds}s",
                style = MaterialTheme.typography.labelLarge,
                color = VeloraCrimson
            )
            Text(
                text = "E${nextEpisode.episodeNumber}: ${nextEpisode.title}",
                style = MaterialTheme.typography.titleMedium,
                color = VeloraOnBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = VeloraOnSurfaceMuted)
                }
                Button(
                    onClick = onPlayNow,
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) {
                    Text("Play Now")
                }
            }
        }
    }
}

private fun formatPlaybackException(error: PlaybackException, sourceType: VideoSourceType): String {
    return when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Network connection failed while loading this ${sourceType.label} stream. Check your internet connection and tap Retry."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
            val raw = error.localizedMessage.orEmpty()
            when {
                raw.contains("401") || raw.contains("403") ->
                    "Provider requires authentication or denied access (HTTP 401/403). Verify the link is publicly accessible."
                raw.contains("404") || raw.contains("410") ->
                    "Video source has been removed or expired (HTTP 404/410)."
                else ->
                    "The video server returned an HTTP error (${raw.ifBlank { "access denied or unavailable" }}). Verify the ${sourceType.label} URL is publicly accessible."
            }
        }
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
            "Cleartext http:// streams are blocked. Update the video source URL to https://."
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
            if (sourceType == VideoSourceType.DASH) {
                "This MPEG-DASH manifest (.mpd) is invalid or uses an unsupported format."
            } else {
                "This HLS playlist (.m3u8) is invalid or uses an unsupported manifest format."
            }
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
            "Source is not a playable direct video stream or uses an unsupported container format."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED ->
            "Unsupported video codec on this device (${error.errorCodeName}). Re-encode as standard H.264/AAC MP4 or HLS."
        else -> error.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Unable to play this ${sourceType.label} source (${error.errorCodeName})."
    }
}

private fun applyQualityConstraint(player: ExoPlayer, quality: String, subtitlesEnabled: Boolean) {
    val builder: TrackSelectionParameters.Builder = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
    when (quality) {
        "1080p" -> builder.setMaxVideoSize(1920, 1080)
        "720p" -> builder.setMaxVideoSize(1280, 720)
        "480p" -> builder.setMaxVideoSize(854, 480)
        else -> builder.clearVideoSizeConstraints()
    }
    player.trackSelectionParameters = builder.build()
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
