package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.Episode
import com.example.ui.components.YouTubeEmbedPlayer
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated
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

    val resolvedSource = remember(streamUrl, streamSourceType) {
        VideoSourceUtils.resolveForPlayback(streamUrl, streamSourceType)
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

    // Source validation failure UI
    if (resolvedSource.validationError != null) {
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
                title = "Playback Unavailable",
                message = resolvedSource.validationError,
                sourceBadge = resolvedSource.sourceType.label,
                canRetry = false,
                onRetry = {},
                onBack = onBack
            )
        }
        return
    }

    if (resolvedSource.sourceType == VideoSourceType.YOUTUBE && resolvedSource.youtubeVideoId != null) {
        YouTubePlayerHost(
            title = title,
            subtitleLabel = subtitleLabel,
            videoId = resolvedSource.youtubeVideoId,
            initialPositionMs = initialPositionMs,
            autoplayNext = autoplayNext,
            isFullscreen = isFullscreen,
            nextEpisode = nextEpisode,
            nextEpisodeCountdown = nextEpisodeCountdown,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onSaveProgress = onSaveProgress,
            onTriggerNextCountdown = { nextEpisodeCountdown = 5 },
            onCancelNextCountdown = { nextEpisodeCountdown = -1 },
            onPlayNextEpisode = onPlayNextEpisode,
            onBack = onBack
        )
        return
    }

    ExoDirectVideoPlayerHost(
        title = title,
        subtitleLabel = subtitleLabel,
        streamUrl = resolvedSource.url,
        sourceType = resolvedSource.sourceType,
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
        onPlayNextEpisode = onPlayNextEpisode,
        onBack = onBack
    )
}

@Composable
private fun YouTubePlayerHost(
    title: String,
    subtitleLabel: String?,
    videoId: String,
    initialPositionMs: Long,
    autoplayNext: Boolean,
    isFullscreen: Boolean,
    nextEpisode: Episode?,
    nextEpisodeCountdown: Int,
    onToggleFullscreen: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onTriggerNextCountdown: () -> Unit,
    onCancelNextCountdown: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                PlaybackErrorCard(
                    title = "YouTube Playback Unavailable",
                    message = ytError!!,
                    sourceBadge = "YouTube Embed",
                    canRetry = true,
                    onRetry = {
                        ytError = null
                        isLoading = true
                        reloadToken++
                    },
                    onBack = onBack
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
                    .navigationBarsPadding()
                    .padding(20.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoDirectVideoPlayerHost(
    title: String,
    subtitleLabel: String?,
    streamUrl: String,
    sourceType: VideoSourceType,
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

    val exoPlayer = remember(streamUrl, sourceType) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItemBuilder = MediaItem.Builder().setUri(streamUrl)
            if (sourceType == VideoSourceType.HLS || streamUrl.contains(".m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
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
                    sourceBadge = sourceType.label,
                    canRetry = true,
                    onRetry = {
                        playerError = null
                        isBuffering = true
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
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
    onBack: () -> Unit
) {
    Surface(
        color = VeloraSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, VeloraOutline),
        modifier = Modifier.widthIn(max = 460.dp)
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
        }
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
            "Network connection failed while loading this ${sourceType.label} stream. Check your internet connection and retry."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "The video server returned an HTTP error (${error.localizedMessage ?: "403/404"}). Verify the ${sourceType.label} URL is publicly accessible."
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
            "Cleartext http:// streams are blocked. Update the video source URL to https://."
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
            "This HLS playlist (.m3u8) appears invalid or uses an unsupported manifest format."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
            "This direct video file could not be parsed. Verify it is an authorized MP4 or HLS stream."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
            "This device could not initialize a video decoder for this stream format."
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
