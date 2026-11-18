package com.example.util

import java.net.URI
import java.util.Locale

enum class VideoSourceType(val key: String, val label: String) {
    AUTO("auto", "Auto-detect"),
    MP4("mp4", "MP4"),
    HLS("hls", "HLS"),
    DASH("dash", "DASH"),
    EMBED("embed", "Embedded Player");

    companion object {
        fun fromKey(key: String?): VideoSourceType {
            return when (key?.trim()?.lowercase(Locale.US)) {
                "auto", "auto_detect", "autodetect" -> AUTO
                "embed", "embedded", "youtube", "yt", "youtube_embed", "vimeo", "dailymotion" -> EMBED
                "hls", "m3u8" -> HLS
                "dash", "mpd" -> DASH
                else -> MP4
            }
        }
    }
}

enum class DetectedSourceKind(val label: String) {
    DIRECT_MP4("Direct Video"),
    HLS_STREAM("HLS"),
    DASH_STREAM("DASH"),
    SUPPORTED_EMBED("Official Embedded Player"),
    ORDINARY_WEBPAGE("External Webpage"),
    UNSUPPORTED("Unsupported Source")
}

enum class PlaybackFailureCategory(val title: String) {
    INVALID_URL("Invalid Video URL"),
    NOT_DIRECT_VIDEO("Not a Direct Video Stream"),
    PROVIDER_EMBED_DISABLED("Embedded Playback Disabled"),
    DRIVE_NOT_PUBLIC("Google Drive Access Required"),
    DRIVE_PLAYBACK_UNAVAILABLE("Google Drive Playback Unavailable"),
    AUTH_REQUIRED("Provider Sign-In Required"),
    VIDEO_REMOVED_OR_NOT_FOUND("Video Unavailable or Removed"),
    NETWORK_FAILED("Network Connection Failed"),
    UNSUPPORTED_FORMAT("Unsupported Media Format")
}

sealed interface SourceValidationResult {
    data object Valid : SourceValidationResult
    data class Invalid(val message: String) : SourceValidationResult

    val isValid: Boolean get() = this is Valid
    val errorOrNull: String? get() = (this as? Invalid)?.message
}

data class ResolvedPlaybackSource(
    val url: String,
    val originalUrl: String,
    val sourceType: VideoSourceType,
    val detectedKind: DetectedSourceKind,
    val providerName: String?,
    val youtubeVideoId: String? = null,
    val embedUrl: String? = null,
    val googleDriveFileId: String? = null,
    val mimeType: String? = null,
    val isDirectlyPlayable: Boolean = detectedKind == DetectedSourceKind.DIRECT_MP4 ||
        detectedKind == DetectedSourceKind.HLS_STREAM ||
        detectedKind == DetectedSourceKind.DASH_STREAM,
    val hasOfficialEmbed: Boolean = detectedKind == DetectedSourceKind.SUPPORTED_EMBED &&
        (!embedUrl.isNullOrBlank() || !youtubeVideoId.isNullOrBlank()),
    val requiresAuthentication: Boolean = false,
    val supportsByteRanges: Boolean = false,
    val openExternallyLabel: String = "Open Website",
    val failureCategory: PlaybackFailureCategory? = null,
    val probeVerified: Boolean = false,
    val probeSummary: String? = null,
    val infoMessage: String? = null,
    val validationError: String? = null
)

object VideoSourceUtils {

    private const val APP_ORIGIN_ID = "com.aistudio.velora.kxmpzq"
    private val YOUTUBE_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")
    private val VIMEO_ID_REGEX = Regex("^[0-9]{4,14}$")
    private val DAILYMOTION_ID_REGEX = Regex("^[a-zA-Z0-9]{5,14}$")
    private val STREAMABLE_CODE_REGEX = Regex("^[a-zA-Z0-9]{4,12}$")
    private val LOOM_ID_REGEX = Regex("^[a-fA-F0-9]{16,40}$")
    private val WISTIA_ID_REGEX = Regex("^[a-zA-Z0-9]{6,20}$")
    private val DRIVE_ID_REGEX = Regex("^[a-zA-Z0-9_-]{10,}$")

    private val DIRECT_VIDEO_EXTENSIONS = listOf(".mp4", ".m4v", ".webm", ".mov", ".mkv", ".3gp", ".ts")

    /**
     * Normalizes to values permitted by existing Firestore security rules:
     * ['auto', 'youtube', 'embed', 'mp4', 'hls'].
     * DASH sources are stored as 'auto' so rules remain compatible while playback auto-detects DASH (.mpd).
     */
    fun normalizeFirestoreSourceType(raw: String?): String {
        return when (raw?.trim()?.lowercase(Locale.US)) {
            "auto", "auto_detect", "autodetect", "dash", "mpd" -> "auto"
            "youtube", "yt", "youtube_embed" -> "youtube"
            "embed", "embedded" -> "embed"
            "hls", "m3u8" -> "hls"
            else -> "mp4"
        }
    }

    fun isYouTubeUrl(url: String): Boolean {
        val host = parseNormalizedHost(url) ?: return false
        return host == "youtube.com" ||
            host == "m.youtube.com" ||
            host == "music.youtube.com" ||
            host == "youtu.be" ||
            host == "youtube-nocookie.com"
    }

    fun isGoogleDriveUrl(url: String): Boolean {
        val host = parseNormalizedHost(url) ?: return false
        return host == "drive.google.com" ||
            host == "docs.google.com" ||
            host == "drive.usercontent.google.com"
    }

    fun isFlystreamUrl(url: String): Boolean {
        val host = parseNormalizedHost(url) ?: return false
        return host == "flystream.net" || host.endsWith(".flystream.net")
    }

    fun isFirebaseStorageUrl(url: String): Boolean {
        val host = parseNormalizedHost(url) ?: return false
        return host == "firebasestorage.googleapis.com" ||
            host.endsWith(".firebasestorage.app") ||
            host == "storage.googleapis.com"
    }

    fun extractYouTubeVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        if (YOUTUBE_ID_REGEX.matches(trimmed)) return trimmed

        val uri = parseUriSafely(trimmed) ?: return null
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        val pathSegments = uri.path
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val candidate = when {
            host == "youtu.be" -> pathSegments.firstOrNull()
            host == "youtube.com" || host == "m.youtube.com" || host == "music.youtube.com" || host == "youtube-nocookie.com" -> {
                val queryId = parseQueryParam(uri.rawQuery, "v")
                when {
                    !queryId.isNullOrBlank() -> queryId
                    pathSegments.size >= 2 && pathSegments[0] in listOf("embed", "shorts", "live", "v") -> pathSegments[1]
                    else -> null
                }
            }
            else -> null
        }
        return candidate?.takeIf { YOUTUBE_ID_REGEX.matches(it) }
    }

    fun extractGoogleDriveFileId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        val uri = parseUriSafely(trimmed) ?: return null
        if (!isGoogleDriveUrl(trimmed)) return null

        val path = uri.path.orEmpty()
        // Matches /file/d/{id}/..., /file/u/0/d/{id}/..., /a/domain/file/d/{id}/...
        val pathMatch = Regex("/file/(?:u/\\d+/)?d/([a-zA-Z0-9_-]+)").find(path)
        if (pathMatch != null) {
            return pathMatch.groupValues[1].takeIf { DRIVE_ID_REGEX.matches(it) }
        }
        // Matches /open?id=..., /uc?id=...&export=download, /download?id=...
        val queryId = parseQueryParam(uri.rawQuery, "id")
        if (!queryId.isNullOrBlank() && DRIVE_ID_REGEX.matches(queryId)) {
            return queryId
        }
        return null
    }

    fun buildGoogleDriveDirectDownloadUrl(fileId: String): String =
        "https://drive.usercontent.google.com/download?id=$fileId&export=download"

    fun buildGoogleDrivePreviewUrl(fileId: String): String =
        "https://drive.google.com/file/d/$fileId/preview"

    fun buildGoogleDriveShareUrl(fileId: String): String =
        "https://drive.google.com/file/d/$fileId/view"

    private fun extractVimeoVideoId(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        if (host != "vimeo.com" && host != "player.vimeo.com") return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        return segments.lastOrNull { VIMEO_ID_REGEX.matches(it) }
    }

    private fun extractDailymotionVideoId(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        val rawCandidate = when (host) {
            "dai.ly" -> segments.firstOrNull()
            "dailymotion.com", "geo.dailymotion.com" -> {
                val videoIdx = segments.indexOf("video")
                if (videoIdx >= 0 && videoIdx + 1 < segments.size) {
                    segments[videoIdx + 1].substringBefore('_')
                } else null
            }
            else -> null
        }
        return rawCandidate?.takeIf { DAILYMOTION_ID_REGEX.matches(it) }
    }

    private fun extractTwitchEmbedUrl(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        return when (host) {
            "twitch.tv", "m.twitch.tv" -> {
                when {
                    segments.size >= 2 && segments[0] == "videos" ->
                        "https://player.twitch.tv/?video=${segments[1]}&parent=$APP_ORIGIN_ID&autoplay=true"
                    segments.size >= 3 && segments[1] == "clip" ->
                        "https://clips.twitch.tv/embed?clip=${segments[2]}&parent=$APP_ORIGIN_ID&autoplay=true"
                    else -> null
                }
            }
            "clips.twitch.tv" -> segments.firstOrNull()?.let { slug ->
                "https://clips.twitch.tv/embed?clip=$slug&parent=$APP_ORIGIN_ID&autoplay=true"
            }
            "player.twitch.tv" -> uri.toString()
            else -> null
        }
    }

    private fun extractStreamableEmbedUrl(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        if (host != "streamable.com") return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        val code = when {
            segments.size == 1 -> segments[0]
            segments.size >= 2 && segments[0] == "e" -> segments[1]
            else -> null
        }?.takeIf { STREAMABLE_CODE_REGEX.matches(it) } ?: return null
        return "https://streamable.com/e/$code?autoplay=1"
    }

    private fun extractWistiaEmbedUrl(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        return when {
            host == "fast.wistia.net" && segments.contains("iframe") -> uri.toString()
            host.endsWith("wistia.com") && segments.size >= 2 && segments[0] == "medias" -> {
                val id = segments[1].takeIf { WISTIA_ID_REGEX.matches(it) } ?: return null
                "https://fast.wistia.net/embed/iframe/$id?autoPlay=true"
            }
            else -> null
        }
    }

    private fun extractLoomEmbedUrl(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        if (host != "loom.com") return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        val id = if (segments.size >= 2 && segments[0] in listOf("share", "embed")) {
            segments[1].takeIf { LOOM_ID_REGEX.matches(it) }
        } else null
        return id?.let { "https://www.loom.com/embed/$it?autoplay=1" }
    }

    private fun extractArchiveOrgEmbedUrl(uri: URI): String? {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        if (host != "archive.org") return null
        val segments = uri.path?.split('/')?.filter { it.isNotBlank() }.orEmpty()
        val id = if (segments.size >= 2 && segments[0] in listOf("details", "embed")) {
            segments[1]
        } else null
        return id?.let { "https://archive.org/embed/$it?autoplay=1" }
    }

    private fun isLikelyExplicitEmbedUrl(uri: URI): Boolean {
        val host = uri.host?.lowercase(Locale.US)?.removePrefix("www.") ?: return false
        val path = uri.path?.lowercase(Locale.US).orEmpty()
        return host.startsWith("player.") ||
            host.startsWith("embed.") ||
            (host == "rumble.com" && path.contains("/embed/")) ||
            path.contains("/embed/") ||
            path.startsWith("/embed")
    }

    private fun isDashUrl(url: String, uri: URI): Boolean {
        val cleanPath = uri.path?.lowercase(Locale.US).orEmpty()
        val lowerNoQuery = url.substringBefore('?').lowercase(Locale.US)
        return cleanPath.endsWith(".mpd") || lowerNoQuery.endsWith(".mpd") || url.contains(".mpd?", ignoreCase = true)
    }

    private fun isHlsUrl(url: String, uri: URI): Boolean {
        val cleanPath = uri.path?.lowercase(Locale.US).orEmpty()
        return cleanPath.endsWith(".m3u8") || url.contains(".m3u8", ignoreCase = true)
    }

    private fun isDirectMp4OrStorageFile(url: String, uri: URI): Boolean {
        val cleanPath = uri.path?.lowercase(Locale.US).orEmpty()
        val lowerUrlNoQuery = url.substringBefore('?').lowercase(Locale.US)
        if (DIRECT_VIDEO_EXTENSIONS.any { cleanPath.endsWith(it) || lowerUrlNoQuery.endsWith(it) }) {
            return true
        }
        if (isFirebaseStorageUrl(url) &&
            !cleanPath.endsWith(".m3u8") &&
            !cleanPath.endsWith(".mpd") &&
            !cleanPath.endsWith(".html") &&
            !cleanPath.endsWith(".htm")
        ) {
            return true
        }
        return false
    }

    private fun guessDirectVideoMimeType(url: String): String? {
        val lower = url.substringBefore('?').lowercase(Locale.US)
        return when {
            lower.endsWith(".mp4") || lower.endsWith(".m4v") -> "video/mp4"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mov") -> "video/quicktime"
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".3gp") -> "video/3gpp"
            else -> null
        }
    }

    internal fun friendlyHostName(host: String): String {
        val clean = host.removePrefix("www.").removePrefix("m.").removePrefix("player.").removePrefix("embed.")
        if (clean == "flystream.net" || clean.endsWith(".flystream.net")) return "Flystream"
        if (clean == "drive.google.com" || clean == "drive.usercontent.google.com" || clean == "docs.google.com") return "Google Drive"
        val base = clean.substringBefore('.')
        return base.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            .ifBlank { clean }
    }

    fun detectSourceTypeFromUrl(url: String, fallback: VideoSourceType = VideoSourceType.AUTO): VideoSourceType {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return fallback
        val detected = detectClassification(trimmed)
        return when (detected.detectedKind) {
            DetectedSourceKind.HLS_STREAM -> VideoSourceType.HLS
            DetectedSourceKind.DASH_STREAM -> VideoSourceType.DASH
            DetectedSourceKind.DIRECT_MP4 -> VideoSourceType.MP4
            DetectedSourceKind.SUPPORTED_EMBED -> VideoSourceType.EMBED
            DetectedSourceKind.ORDINARY_WEBPAGE, DetectedSourceKind.UNSUPPORTED -> VideoSourceType.AUTO
        }
    }

    fun detectClassification(
        rawUrl: String,
        customProviderName: String? = null
    ): ResolvedPlaybackSource {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return ResolvedPlaybackSource(
                url = "",
                originalUrl = "",
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                providerName = customProviderName?.takeIf { it.isNotBlank() },
                failureCategory = PlaybackFailureCategory.INVALID_URL,
                validationError = "No playable video or webpage URL is configured for this title."
            )
        }

        val uri = parseUriSafely(trimmed)
            ?: return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                providerName = customProviderName?.takeIf { it.isNotBlank() },
                failureCategory = PlaybackFailureCategory.INVALID_URL,
                validationError = "Invalid URL format. Enter a complete https:// URL."
            )

        val scheme = uri.scheme?.lowercase(Locale.US)
        if (scheme == "http") {
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                providerName = customProviderName?.takeIf { it.isNotBlank() } ?: uri.host?.let(::friendlyHostName),
                failureCategory = PlaybackFailureCategory.INVALID_URL,
                validationError = "Insecure http:// sources are blocked on Android. Use an https:// URL or open it in an external browser."
            )
        }
        if (scheme != "https" || uri.host.isNullOrBlank()) {
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                providerName = customProviderName?.takeIf { it.isNotBlank() },
                failureCategory = PlaybackFailureCategory.INVALID_URL,
                validationError = "URL must begin with https:// and include a valid host domain."
            )
        }

        val host = uri.host.lowercase(Locale.US).removePrefix("www.")
        val explicitProvider = customProviderName?.trim()?.takeIf { it.isNotBlank() }

        // 1. HLS manifest (.m3u8)
        if (isHlsUrl(trimmed, uri)) {
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.HLS,
                detectedKind = DetectedSourceKind.HLS_STREAM,
                providerName = explicitProvider ?: "HLS Stream",
                mimeType = "application/vnd.apple.mpegurl",
                isDirectlyPlayable = true,
                hasOfficialEmbed = false,
                infoMessage = "Adaptive HLS stream (.m3u8) — plays natively via Media3 ExoPlayer."
            )
        }

        // 2. MPEG-DASH manifest (.mpd)
        if (isDashUrl(trimmed, uri)) {
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.DASH,
                detectedKind = DetectedSourceKind.DASH_STREAM,
                providerName = explicitProvider ?: "MPEG-DASH Stream",
                mimeType = "application/dash+xml",
                isDirectlyPlayable = true,
                hasOfficialEmbed = false,
                infoMessage = "Adaptive MPEG-DASH stream (.mpd) — plays natively via Media3 ExoPlayer."
            )
        }

        // 3. Direct video file / Firebase Storage object
        if (isDirectMp4OrStorageFile(trimmed, uri)) {
            val provider = explicitProvider ?: if (isFirebaseStorageUrl(trimmed)) "Firebase Storage" else "Direct Video"
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.MP4,
                detectedKind = DetectedSourceKind.DIRECT_MP4,
                providerName = provider,
                mimeType = guessDirectVideoMimeType(trimmed),
                isDirectlyPlayable = true,
                hasOfficialEmbed = false,
                infoMessage = "Direct video stream — plays natively via Media3 ExoPlayer."
            )
        }

        // 4. YouTube official IFrame Player API
        if (isYouTubeUrl(trimmed)) {
            val ytId = extractYouTubeVideoId(trimmed)
            if (ytId != null) {
                return ResolvedPlaybackSource(
                    url = trimmed,
                    originalUrl = trimmed,
                    sourceType = VideoSourceType.EMBED,
                    detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                    providerName = explicitProvider ?: "YouTube",
                    youtubeVideoId = ytId,
                    embedUrl = "https://www.youtube.com/embed/$ytId",
                    isDirectlyPlayable = false,
                    hasOfficialEmbed = true,
                    openExternallyLabel = "Open on YouTube",
                    infoMessage = "YouTube Video ID '$ytId' — plays via official YouTube IFrame Player."
                )
            }
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                providerName = explicitProvider ?: "YouTube",
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                openExternallyLabel = "Open on YouTube",
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                infoMessage = "This YouTube link points to a channel, playlist, or page without a specific video ID."
            )
        }

        // 5. Vimeo official embedded player
        extractVimeoVideoId(uri)?.let { vimeoId ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Vimeo",
                embedUrl = "https://player.vimeo.com/video/$vimeoId?autoplay=1&dnt=1",
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                openExternallyLabel = "Open on Vimeo",
                infoMessage = "Vimeo Video '$vimeoId' — plays via official Vimeo embedded player."
            )
        }

        // 6. Dailymotion official embedded player
        extractDailymotionVideoId(uri)?.let { dmId ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Dailymotion",
                embedUrl = "https://www.dailymotion.com/embed/video/$dmId?autoplay=1",
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                openExternallyLabel = "Open on Dailymotion",
                infoMessage = "Dailymotion Video '$dmId' — plays via official Dailymotion embedded player."
            )
        }

        // 7. Twitch official embedded player
        extractTwitchEmbedUrl(uri)?.let { twitchEmbed ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Twitch",
                embedUrl = twitchEmbed,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                openExternallyLabel = "Open on Twitch",
                infoMessage = "Twitch media — plays via official Twitch embedded player."
            )
        }

        // 8. Streamable, Wistia, Loom, Internet Archive
        extractStreamableEmbedUrl(uri)?.let { streamableEmbed ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Streamable",
                embedUrl = streamableEmbed,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                infoMessage = "Streamable video — plays via official Streamable embedded player."
            )
        }
        extractWistiaEmbedUrl(uri)?.let { wistiaEmbed ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Wistia",
                embedUrl = wistiaEmbed,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                infoMessage = "Wistia video — plays via official Wistia embedded player."
            )
        }
        extractLoomEmbedUrl(uri)?.let { loomEmbed ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Loom",
                embedUrl = loomEmbed,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                infoMessage = "Loom video — plays via official Loom embedded player."
            )
        }
        extractArchiveOrgEmbedUrl(uri)?.let { archiveEmbed ->
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: "Internet Archive",
                embedUrl = archiveEmbed,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                infoMessage = "Internet Archive media — plays via official Archive.org embedded player."
            )
        }

        // 9. Google Drive sharing/download URLs
        if (isGoogleDriveUrl(trimmed)) {
            val fileId = extractGoogleDriveFileId(trimmed)
            if (fileId != null) {
                return ResolvedPlaybackSource(
                    url = trimmed,
                    originalUrl = buildGoogleDriveShareUrl(fileId),
                    sourceType = VideoSourceType.EMBED,
                    detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                    providerName = explicitProvider ?: "Google Drive",
                    embedUrl = buildGoogleDrivePreviewUrl(fileId),
                    googleDriveFileId = fileId,
                    isDirectlyPlayable = false,
                    hasOfficialEmbed = true,
                    openExternallyLabel = "Open in Google Drive",
                    infoMessage = "Google Drive file (ID: $fileId). At playback or Test Playback, Notflix verifies public access and streams via native ExoPlayer when Drive serves a direct range-capable video, or falls back to Google Drive's official preview player."
                )
            }
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                providerName = explicitProvider ?: "Google Drive",
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                openExternallyLabel = "Open in Google Drive",
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                infoMessage = "Google Drive folder or page without a specific video file ID. Open it directly in Google Drive."
            )
        }

        // 10. Flystream (known third-party web portal with no official embed API)
        if (isFlystreamUrl(trimmed)) {
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                providerName = explicitProvider ?: "Flystream",
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                openExternallyLabel = "Open on Flystream",
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                infoMessage = "Flystream does not provide a direct MP4/HLS stream or an official third-party embed API, and uses browser verification. Tap 'Open on Flystream' to watch in your browser."
            )
        }

        // 11. Generic explicit embed / player URLs
        if (isLikelyExplicitEmbedUrl(uri)) {
            return ResolvedPlaybackSource(
                url = trimmed,
                originalUrl = trimmed,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = explicitProvider ?: friendlyHostName(host),
                embedUrl = trimmed,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                infoMessage = "Embedded player endpoint from ${explicitProvider ?: friendlyHostName(host)}."
            )
        }

        // 12. Unknown / extensionless URL (will be HTTP-probed at playback or Test Playback before assuming webpage!)
        val provider = explicitProvider ?: friendlyHostName(host)
        return ResolvedPlaybackSource(
            url = trimmed,
            originalUrl = trimmed,
            sourceType = VideoSourceType.AUTO,
            detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
            providerName = provider,
            isDirectlyPlayable = false,
            hasOfficialEmbed = false,
            failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
            infoMessage = "'$host' does not have a direct video extension or recognized official embed URL. Run 'Test Playback' to check whether it redirects to a direct video stream, or open it via 'Open Website'."
        )
    }

    fun validateVideoSource(
        url: String,
        sourceType: VideoSourceType,
        required: Boolean = true
    ): SourceValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return if (required) {
                SourceValidationResult.Invalid("Video or webpage URL is required.")
            } else {
                SourceValidationResult.Valid
            }
        }

        val detected = detectClassification(trimmed)
        if (detected.detectedKind == DetectedSourceKind.UNSUPPORTED) {
            return SourceValidationResult.Invalid(
                detected.validationError ?: "Unsupported video source URL."
            )
        }

        return when (sourceType) {
            VideoSourceType.AUTO -> SourceValidationResult.Valid

            VideoSourceType.MP4 -> {
                when (detected.detectedKind) {
                    DetectedSourceKind.HLS_STREAM -> SourceValidationResult.Invalid(
                        "This looks like an HLS playlist (.m3u8). Switch Source Type to HLS or Auto-detect."
                    )
                    DetectedSourceKind.DASH_STREAM -> SourceValidationResult.Invalid(
                        "This looks like an MPEG-DASH manifest (.mpd). Switch Source Type to DASH or Auto-detect."
                    )
                    DetectedSourceKind.SUPPORTED_EMBED -> SourceValidationResult.Invalid(
                        "This is a ${detected.providerName ?: "video host"} webpage/embed link, not a direct MP4 file. Switch Source Type to Auto-detect or Embedded Player."
                    )
                    DetectedSourceKind.ORDINARY_WEBPAGE -> SourceValidationResult.Invalid(
                        "This looks like an ordinary webpage rather than a direct MP4 video stream. Switch Source Type to Auto-detect (to probe headers/redirects), Embedded Player, or upload an MP4 to Firebase Storage."
                    )
                    DetectedSourceKind.DIRECT_MP4 -> SourceValidationResult.Valid
                    DetectedSourceKind.UNSUPPORTED -> SourceValidationResult.Invalid(detected.validationError ?: "Invalid MP4 URL.")
                }
            }

            VideoSourceType.HLS -> {
                when (detected.detectedKind) {
                    DetectedSourceKind.HLS_STREAM -> SourceValidationResult.Valid
                    DetectedSourceKind.DIRECT_MP4 -> SourceValidationResult.Invalid(
                        "This is a direct MP4/video file. Switch Source Type to MP4 or Auto-detect."
                    )
                    DetectedSourceKind.DASH_STREAM -> SourceValidationResult.Invalid(
                        "This is an MPEG-DASH (.mpd) manifest. Switch Source Type to DASH or Auto-detect."
                    )
                    else -> SourceValidationResult.Invalid(
                        "HLS streams must reference an .m3u8 manifest URL. Switch Source Type to Auto-detect for extensionless streams or hosted webpages."
                    )
                }
            }

            VideoSourceType.DASH -> {
                when (detected.detectedKind) {
                    DetectedSourceKind.DASH_STREAM -> SourceValidationResult.Valid
                    DetectedSourceKind.HLS_STREAM -> SourceValidationResult.Invalid(
                        "This is an HLS (.m3u8) manifest. Switch Source Type to HLS or Auto-detect."
                    )
                    DetectedSourceKind.DIRECT_MP4 -> SourceValidationResult.Invalid(
                        "This is a direct MP4/video file. Switch Source Type to MP4 or Auto-detect."
                    )
                    else -> SourceValidationResult.Invalid(
                        "DASH streams must reference an .mpd manifest URL. Switch Source Type to Auto-detect for extensionless streams."
                    )
                }
            }

            VideoSourceType.EMBED -> {
                when (detected.detectedKind) {
                    DetectedSourceKind.DIRECT_MP4 -> SourceValidationResult.Invalid(
                        "This is a direct MP4 video file. Switch Source Type to MP4 or Auto-detect for ExoPlayer playback."
                    )
                    DetectedSourceKind.HLS_STREAM -> SourceValidationResult.Invalid(
                        "This is an HLS (.m3u8) stream. Switch Source Type to HLS or Auto-detect for ExoPlayer playback."
                    )
                    DetectedSourceKind.DASH_STREAM -> SourceValidationResult.Invalid(
                        "This is an MPEG-DASH (.mpd) stream. Switch Source Type to DASH or Auto-detect for ExoPlayer playback."
                    )
                    DetectedSourceKind.SUPPORTED_EMBED,
                    DetectedSourceKind.ORDINARY_WEBPAGE -> SourceValidationResult.Valid
                    DetectedSourceKind.UNSUPPORTED -> SourceValidationResult.Invalid(detected.validationError ?: "Invalid embed URL.")
                }
            }
        }
    }

    fun validateOptionalUrl(url: String, fieldName: String): SourceValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return SourceValidationResult.Valid
        val uri = parseUriSafely(trimmed)
            ?: return SourceValidationResult.Invalid("$fieldName is not a valid URL.")
        val scheme = uri.scheme?.lowercase(Locale.US)
        if (scheme == "http") {
            return SourceValidationResult.Invalid(
                "$fieldName uses http:// which may fail to load. Use https://."
            )
        }
        if (scheme != "https" || uri.host.isNullOrBlank()) {
            return SourceValidationResult.Invalid("$fieldName must be a valid https:// URL.")
        }
        return SourceValidationResult.Valid
    }

    fun validateTrailerUrl(url: String): SourceValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return SourceValidationResult.Valid
        return validateVideoSource(trimmed, VideoSourceType.AUTO, required = false)
    }

    fun resolveForPlayback(
        rawUrl: String,
        declaredType: String?,
        providerName: String? = null
    ): ResolvedPlaybackSource {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return ResolvedPlaybackSource(
                url = "",
                originalUrl = "",
                sourceType = VideoSourceType.fromKey(declaredType),
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                providerName = providerName?.takeIf { it.isNotBlank() },
                failureCategory = PlaybackFailureCategory.INVALID_URL,
                validationError = "No playable video or webpage URL is configured for this title."
            )
        }

        val detected = detectClassification(trimmed, providerName)
        if (detected.detectedKind == DetectedSourceKind.UNSUPPORTED) {
            return detected
        }

        val declared = declaredType?.let { VideoSourceType.fromKey(it) } ?: VideoSourceType.AUTO

        // Honor explicit Embedded Player selection for compatible hosting providers if not a direct MP4/HLS/DASH stream
        if (declared == VideoSourceType.EMBED &&
            detected.detectedKind == DetectedSourceKind.ORDINARY_WEBPAGE
        ) {
            return detected.copy(
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                embedUrl = trimmed,
                hasOfficialEmbed = true,
                failureCategory = null,
                infoMessage = "Loading embedded player from ${detected.providerName ?: "provider"}. If the provider blocks embedding, you can open the website directly."
            )
        }

        // Never treat an ordinary webpage as a direct MP4 even if legacy metadata stored "mp4"
        if (declared == VideoSourceType.MP4 &&
            detected.detectedKind == DetectedSourceKind.ORDINARY_WEBPAGE
        ) {
            return detected.copy(
                sourceType = VideoSourceType.AUTO,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                infoMessage = "This URL points to a webpage rather than a direct MP4 file. Open it on the provider's website or update the source URL in the Admin Dashboard."
            )
        }

        return detected
    }

    internal fun parseNormalizedHost(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        return parseUriSafely(trimmed)?.host?.lowercase(Locale.US)?.removePrefix("www.")
    }

    internal fun parseUriSafely(raw: String): URI? = try {
        URI(raw.trim())
    } catch (_: Exception) {
        null
    }

    internal fun parseQueryParam(rawQuery: String?, key: String): String? {
        if (rawQuery.isNullOrBlank()) return null
        return rawQuery.split('&')
            .asSequence()
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) null
                else pair.substring(0, idx) to pair.substring(idx + 1)
            }
            .firstOrNull { it.first == key }
            ?.second
            ?.takeIf { it.isNotBlank() }
    }
}
