package com.example.util

import java.net.URI

enum class VideoSourceType(val key: String, val label: String) {
    YOUTUBE("youtube", "YouTube Embed"),
    MP4("mp4", "MP4"),
    HLS("hls", "HLS");

    companion object {
        fun fromKey(key: String?): VideoSourceType {
            return when (key?.trim()?.lowercase()) {
                "youtube", "yt", "youtube_embed" -> YOUTUBE
                "hls", "m3u8" -> HLS
                else -> MP4
            }
        }
    }
}

sealed interface SourceValidationResult {
    data object Valid : SourceValidationResult
    data class Invalid(val message: String) : SourceValidationResult

    val isValid: Boolean get() = this is Valid
    val errorOrNull: String? get() = (this as? Invalid)?.message
}

data class ResolvedPlaybackSource(
    val url: String,
    val sourceType: VideoSourceType,
    val youtubeVideoId: String?,
    val validationError: String?
)

object VideoSourceUtils {

    private val YOUTUBE_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")

    fun isYouTubeUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val host = parseUriSafely(trimmed)?.host?.lowercase()?.removePrefix("www.") ?: return false
        return host == "youtube.com" ||
            host == "m.youtube.com" ||
            host == "music.youtube.com" ||
            host == "youtu.be" ||
            host == "youtube-nocookie.com"
    }

    fun isGoogleDriveUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val host = parseUriSafely(trimmed)?.host?.lowercase()?.removePrefix("www.") ?: return false
        return host.contains("drive.google.com") || host.contains("docs.google.com")
    }

    fun extractGoogleDriveFileId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        val uri = parseUriSafely(trimmed) ?: return null
        val path = uri.path.orEmpty()
        val match = Regex("/file/d/([a-zA-Z0-9_-]+)").find(path)
        if (match != null) return match.groupValues[1]
        val queryId = parseQueryParam(uri.rawQuery, "id")
        if (!queryId.isNullOrBlank()) return queryId
        return null
    }

    fun convertGoogleDriveUrlToDirectStream(url: String): String {
        val fileId = extractGoogleDriveFileId(url) ?: return url
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    fun extractYouTubeVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        if (YOUTUBE_ID_REGEX.matches(trimmed)) return trimmed

        val uri = parseUriSafely(trimmed) ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
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

    fun detectSourceTypeFromUrl(url: String, fallback: VideoSourceType = VideoSourceType.MP4): VideoSourceType {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return fallback
        return when {
            isYouTubeUrl(trimmed) -> VideoSourceType.YOUTUBE
            isGoogleDriveUrl(trimmed) -> VideoSourceType.MP4
            trimmed.contains(".m3u8", ignoreCase = true) -> VideoSourceType.HLS
            else -> fallback
        }
    }

    fun validateVideoSource(
        url: String,
        sourceType: VideoSourceType,
        required: Boolean = true
    ): SourceValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return if (required) {
                SourceValidationResult.Invalid("Video source URL is required.")
            } else {
                SourceValidationResult.Valid
            }
        }

        val uri = parseUriSafely(trimmed)
            ?: return SourceValidationResult.Invalid("Invalid URL format. Enter a full https:// URL.")

        val scheme = uri.scheme?.lowercase()
        if (scheme == "http") {
            return SourceValidationResult.Invalid(
                "Insecure http:// streams are blocked on Android. Use an https:// URL instead."
            )
        }
        if (scheme != "https") {
            return SourceValidationResult.Invalid("Video URL must begin with https://.")
        }
        if (uri.host.isNullOrBlank()) {
            return SourceValidationResult.Invalid("Video URL is missing a valid domain host.")
        }

        if (isGoogleDriveUrl(trimmed)) {
            val fileId = extractGoogleDriveFileId(trimmed)
            if (fileId.isNullOrBlank()) {
                return SourceValidationResult.Invalid(
                    "Could not extract a valid file ID from this Google Drive link. Ensure permissions are set to 'Anyone with the link can view' or use Firebase Storage upload."
                )
            }
            return SourceValidationResult.Valid
        }

        return when (sourceType) {
            VideoSourceType.YOUTUBE -> {
                if (!isYouTubeUrl(trimmed)) {
                    SourceValidationResult.Invalid(
                        "Enter a valid YouTube URL (youtube.com/watch?v=..., youtu.be/..., or youtube.com/embed/...)."
                    )
                } else if (extractYouTubeVideoId(trimmed) == null) {
                    SourceValidationResult.Invalid(
                        "Could not extract an 11-character YouTube video ID from this URL."
                    )
                } else {
                    SourceValidationResult.Valid
                }
            }
            VideoSourceType.MP4 -> {
                when {
                    isYouTubeUrl(trimmed) -> SourceValidationResult.Invalid(
                        "This is a YouTube link. Change Video Source Type to YouTube Embed."
                    )
                    trimmed.contains(".m3u8", ignoreCase = true) -> SourceValidationResult.Invalid(
                        "This looks like an HLS playlist (.m3u8). Change Video Source Type to HLS."
                    )
                    else -> SourceValidationResult.Valid
                }
            }
            VideoSourceType.HLS -> {
                when {
                    isYouTubeUrl(trimmed) -> SourceValidationResult.Invalid(
                        "This is a YouTube link. Change Video Source Type to YouTube Embed."
                    )
                    trimmed.substringBefore('?').endsWith(".mp4", ignoreCase = true) ||
                        trimmed.substringBefore('?').endsWith(".webm", ignoreCase = true) ->
                        SourceValidationResult.Invalid(
                            "This is a direct MP4/WebM file. Change Video Source Type to MP4."
                        )
                    !trimmed.contains("m3u8", ignoreCase = true) -> SourceValidationResult.Invalid(
                        "HLS streams must reference an .m3u8 manifest URL."
                    )
                    else -> SourceValidationResult.Valid
                }
            }
        }
    }

    fun validateOptionalUrl(url: String, fieldName: String): SourceValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return SourceValidationResult.Valid
        val uri = parseUriSafely(trimmed)
            ?: return SourceValidationResult.Invalid("$fieldName is not a valid URL.")
        val scheme = uri.scheme?.lowercase()
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
        val detected = detectSourceTypeFromUrl(trimmed, VideoSourceType.MP4)
        return validateVideoSource(trimmed, detected, required = false)
    }

    fun resolveForPlayback(rawUrl: String, declaredType: String?): ResolvedPlaybackSource {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return ResolvedPlaybackSource(
                url = "",
                sourceType = VideoSourceType.fromKey(declaredType),
                youtubeVideoId = null,
                validationError = "No playable video source URL is configured for this title."
            )
        }
        val effectiveUrl = if (isGoogleDriveUrl(trimmed)) {
            convertGoogleDriveUrlToDirectStream(trimmed)
        } else {
            trimmed
        }

        val effectiveType = when {
            isYouTubeUrl(effectiveUrl) -> VideoSourceType.YOUTUBE
            declaredType != null -> {
                val parsed = VideoSourceType.fromKey(declaredType)
                if (parsed == VideoSourceType.MP4 && effectiveUrl.contains(".m3u8", ignoreCase = true)) {
                    VideoSourceType.HLS
                } else {
                    parsed
                }
            }
            effectiveUrl.contains(".m3u8", ignoreCase = true) -> VideoSourceType.HLS
            isGoogleDriveUrl(trimmed) -> VideoSourceType.MP4
            else -> VideoSourceType.MP4
        }
        val validation = validateVideoSource(effectiveUrl, effectiveType, required = true)
        val youtubeId = if (effectiveType == VideoSourceType.YOUTUBE) extractYouTubeVideoId(effectiveUrl) else null
        return ResolvedPlaybackSource(
            url = effectiveUrl,
            sourceType = effectiveType,
            youtubeVideoId = youtubeId,
            validationError = validation.errorOrNull
        )
    }

    private fun parseUriSafely(raw: String): URI? = try {
        URI(raw.trim())
    } catch (_: Exception) {
        null
    }

    private fun parseQueryParam(rawQuery: String?, key: String): String? {
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
