package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Locale

data class HttpProbeResult(
    val statusCode: Int,
    val finalUrl: String,
    val contentType: String?,
    val acceptRanges: String?,
    val contentRange: String?,
    val contentLength: Long?,
    val contentDisposition: String?,
    val xFrameOptions: String?,
    val contentSecurityPolicy: String?,
    val bodySnippet: String?,
    val magicBytes: ByteArray = ByteArray(0),
    val redirectCount: Int = 0,
    val networkError: String? = null
) {
    val isSuccess: Boolean get() = networkError == null && statusCode in 200..299
    val normalizedMimeType: String?
        get() = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
    val supportsRangeRequests: Boolean
        get() = statusCode == 206 ||
            acceptRanges?.equals("bytes", ignoreCase = true) == true ||
            !contentRange.isNullOrBlank()
}

interface HttpProbeClient {
    suspend fun probe(
        url: String,
        useRangeHeader: Boolean = true,
        readSnippetBytes: Int = 4096
    ): HttpProbeResult
}

/**
 * Lightweight HTTP header & partial-range probe client.
 * - Never downloads entire webpages or large video files (reads at most 4 KB).
 * - Safely follows up to 5 HTTP redirects (enforcing https:// on final stream).
 * - Uses bounded connect/read timeouts so playback never hangs.
 */
object UrlConnectionHttpProbeClient : HttpProbeClient {
    private const val CONNECT_TIMEOUT_MS = 7_500
    private const val READ_TIMEOUT_MS = 7_500
    private const val MAX_REDIRECTS = 5
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    override suspend fun probe(
        url: String,
        useRangeHeader: Boolean,
        readSnippetBytes: Int
    ): HttpProbeResult = withContext(Dispatchers.IO) {
        var currentUrl = url.trim()
        var redirects = 0
        var lastConn: HttpURLConnection? = null
        try {
            while (redirects <= MAX_REDIRECTS) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "*/*")
                    if (useRangeHeader) {
                        setRequestProperty("Range", "bytes=0-2047")
                    }
                }
                lastConn = conn
                val code = conn.responseCode

                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")?.trim()
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        return@withContext HttpProbeResult(
                            statusCode = code,
                            finalUrl = currentUrl,
                            contentType = null,
                            acceptRanges = null,
                            contentRange = null,
                            contentLength = null,
                            contentDisposition = null,
                            xFrameOptions = null,
                            contentSecurityPolicy = null,
                            bodySnippet = null,
                            redirectCount = redirects,
                            networkError = "Server returned redirect ($code) without a target URL."
                        )
                    }
                    currentUrl = runCatching { URI(currentUrl).resolve(location).toString() }
                        .getOrElse { location }
                    redirects++
                    continue
                }

                val contentType = conn.getHeaderField("Content-Type")
                val acceptRanges = conn.getHeaderField("Accept-Ranges")
                val contentRange = conn.getHeaderField("Content-Range")
                val contentLength = conn.getHeaderField("Content-Length")?.toLongOrNull()
                val contentDisposition = conn.getHeaderField("Content-Disposition")
                val xFrameOptions = conn.getHeaderField("X-Frame-Options")
                val csp = conn.getHeaderField("Content-Security-Policy")

                val rawBytes = readBoundedBytes(
                    if (code in 200..399) conn.inputStream else conn.errorStream,
                    maxBytes = readSnippetBytes.coerceIn(0, 8192)
                )
                conn.disconnect()

                val mimeLower = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.US).orEmpty()
                val shouldDecodeText = mimeLower.startsWith("text/") ||
                    mimeLower.contains("json") ||
                    mimeLower.contains("xml") ||
                    mimeLower.contains("mpegurl") ||
                    mimeLower.isBlank()

                val snippet = if (shouldDecodeText && rawBytes.isNotEmpty()) {
                    String(rawBytes, Charsets.UTF_8)
                } else {
                    null
                }

                return@withContext HttpProbeResult(
                    statusCode = code,
                    finalUrl = currentUrl,
                    contentType = contentType,
                    acceptRanges = acceptRanges,
                    contentRange = contentRange,
                    contentLength = contentLength,
                    contentDisposition = contentDisposition,
                    xFrameOptions = xFrameOptions,
                    contentSecurityPolicy = csp,
                    bodySnippet = snippet,
                    magicBytes = rawBytes.copyOf(minOf(rawBytes.size, 64)),
                    redirectCount = redirects
                )
            }

            HttpProbeResult(
                statusCode = 0,
                finalUrl = currentUrl,
                contentType = null,
                acceptRanges = null,
                contentRange = null,
                contentLength = null,
                contentDisposition = null,
                xFrameOptions = null,
                contentSecurityPolicy = null,
                bodySnippet = null,
                redirectCount = redirects,
                networkError = "Too many redirects (more than $MAX_REDIRECTS hops) while resolving video URL."
            )
        } catch (e: SocketTimeoutException) {
            HttpProbeResult(
                statusCode = 0,
                finalUrl = currentUrl,
                contentType = null,
                acceptRanges = null,
                contentRange = null,
                contentLength = null,
                contentDisposition = null,
                xFrameOptions = null,
                contentSecurityPolicy = null,
                bodySnippet = null,
                redirectCount = redirects,
                networkError = "Connection timed out while checking the video source."
            )
        } catch (e: UnknownHostException) {
            HttpProbeResult(
                statusCode = 0,
                finalUrl = currentUrl,
                contentType = null,
                acceptRanges = null,
                contentRange = null,
                contentLength = null,
                contentDisposition = null,
                xFrameOptions = null,
                contentSecurityPolicy = null,
                bodySnippet = null,
                redirectCount = redirects,
                networkError = "Could not resolve host domain. Check your internet connection or verify the URL."
            )
        } catch (e: IOException) {
            HttpProbeResult(
                statusCode = 0,
                finalUrl = currentUrl,
                contentType = null,
                acceptRanges = null,
                contentRange = null,
                contentLength = null,
                contentDisposition = null,
                xFrameOptions = null,
                contentSecurityPolicy = null,
                bodySnippet = null,
                redirectCount = redirects,
                networkError = "Network connection failed: ${e.message ?: "I/O error"}."
            )
        } catch (e: Exception) {
            HttpProbeResult(
                statusCode = 0,
                finalUrl = currentUrl,
                contentType = null,
                acceptRanges = null,
                contentRange = null,
                contentLength = null,
                contentDisposition = null,
                xFrameOptions = null,
                contentSecurityPolicy = null,
                bodySnippet = null,
                redirectCount = redirects,
                networkError = "Unable to inspect video URL: ${e.message ?: "unexpected error"}."
            )
        } finally {
            runCatching { lastConn?.disconnect() }
        }
    }

    private fun readBoundedBytes(stream: java.io.InputStream?, maxBytes: Int): ByteArray {
        if (stream == null || maxBytes <= 0) return ByteArray(0)
        return runCatching {
            stream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(1024)
                var total = 0
                while (total < maxBytes) {
                    val toRead = minOf(buf.size, maxBytes - total)
                    val read = input.read(buf, 0, toRead)
                    if (read <= 0) break
                    out.write(buf, 0, read)
                    total += read
                }
                out.toByteArray()
            }
        }.getOrDefault(ByteArray(0))
    }
}

/**
 * Centralized multi-source playback resolver that combines fast static URL classification
 * with lightweight HTTP header / content-type / redirect probing, Google Drive media resolution,
 * YouTube oEmbed embeddability verification, and framing-policy checks for webpages.
 */
object PlaybackSourceResolver {

    suspend fun resolve(
        rawUrl: String,
        declaredType: String? = "auto",
        providerName: String? = null,
        httpClient: HttpProbeClient = UrlConnectionHttpProbeClient
    ): ResolvedPlaybackSource {
        val staticResolved = VideoSourceUtils.resolveForPlayback(rawUrl, declaredType, providerName)
        if (staticResolved.detectedKind == DetectedSourceKind.UNSUPPORTED || staticResolved.validationError != null) {
            return staticResolved.copy(probeVerified = true)
        }

        // 1. YouTube: preserve official IFrame Player, verify embeddability via official oEmbed
        if (staticResolved.youtubeVideoId != null) {
            return resolveYouTube(staticResolved, httpClient)
        }

        // 2. Google Drive: dedicated Drive handler (checks direct range stream -> /preview fallback -> accurate error)
        if (staticResolved.googleDriveFileId != null || VideoSourceUtils.isGoogleDriveUrl(staticResolved.url)) {
            return resolveGoogleDrive(staticResolved, httpClient)
        }

        // 3. Direct MP4 / HLS / DASH: probe headers & redirects to verify live reachability and real MIME type
        if (staticResolved.detectedKind == DetectedSourceKind.DIRECT_MP4 ||
            staticResolved.detectedKind == DetectedSourceKind.HLS_STREAM ||
            staticResolved.detectedKind == DetectedSourceKind.DASH_STREAM
        ) {
            return resolveDirectOrPlaylist(staticResolved, httpClient)
        }

        // 4. Known Official Embed Providers (Vimeo, Dailymotion, Twitch, Streamable, Wistia, Loom, Archive.org, explicit /embed/)
        if (staticResolved.detectedKind == DetectedSourceKind.SUPPORTED_EMBED && !staticResolved.embedUrl.isNullOrBlank()) {
            return resolveOfficialEmbed(staticResolved, declaredType, httpClient)
        }

        // 5. Ordinary / Unknown Webpage (including Flystream & extensionless URLs that might redirect to real streams!)
        return resolveWebpageOrUnknown(staticResolved, declaredType, httpClient)
    }

    private suspend fun resolveYouTube(
        base: ResolvedPlaybackSource,
        httpClient: HttpProbeClient
    ): ResolvedPlaybackSource {
        val videoId = base.youtubeVideoId ?: return base
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val encoded = runCatching { URLEncoder.encode(watchUrl, "UTF-8") }.getOrElse { watchUrl }
        val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"

        val probe = httpClient.probe(oembedUrl, useRangeHeader = false, readSnippetBytes = 2048)
        if (probe.networkError != null) {
            // Do not block YouTube playback if oEmbed probe timed out; let official IFrame Player try
            return base.copy(
                probeVerified = false,
                probeSummary = "Official YouTube IFrame Player ready (metadata check skipped: ${probe.networkError})"
            )
        }

        return when (probe.statusCode) {
            200 -> base.copy(
                probeVerified = true,
                hasOfficialEmbed = true,
                isDirectlyPlayable = false,
                requiresAuthentication = false,
                failureCategory = null,
                probeSummary = "Verified via YouTube oEmbed — embedding is enabled.",
                infoMessage = "YouTube Video '$videoId' — official embedding enabled."
            )
            401, 403 -> base.copy(
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                requiresAuthentication = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.PROVIDER_EMBED_DISABLED,
                validationError = "This YouTube video has embedding disabled by its owner (HTTP ${probe.statusCode}). Open it on YouTube instead.",
                probeSummary = "YouTube oEmbed returned HTTP ${probe.statusCode}: embedding disabled by video owner."
            )
            400, 404 -> base.copy(
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.VIDEO_REMOVED_OR_NOT_FOUND,
                validationError = "This YouTube video was not found, is private, or has been removed (HTTP ${probe.statusCode}).",
                probeSummary = "YouTube oEmbed returned HTTP ${probe.statusCode}: video not found or private."
            )
            else -> base.copy(
                probeVerified = false,
                probeSummary = "YouTube returned HTTP ${probe.statusCode}; attempting official YouTube IFrame Player."
            )
        }
    }

    private suspend fun resolveGoogleDrive(
        base: ResolvedPlaybackSource,
        httpClient: HttpProbeClient
    ): ResolvedPlaybackSource {
        val fileId = base.googleDriveFileId ?: VideoSourceUtils.extractGoogleDriveFileId(base.originalUrl)
        if (fileId.isNullOrBlank()) {
            return base.copy(
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                infoMessage = "This Google Drive link does not point to an individual video file. Open it in Google Drive."
            )
        }

        val shareUrl = VideoSourceUtils.buildGoogleDriveShareUrl(fileId)
        val previewUrl = VideoSourceUtils.buildGoogleDrivePreviewUrl(fileId)
        val downloadUrl = VideoSourceUtils.buildGoogleDriveDirectDownloadUrl(fileId)

        val dlProbe = httpClient.probe(downloadUrl, useRangeHeader = true, readSnippetBytes = 4096)

        if (dlProbe.networkError != null) {
            return base.copy(
                url = base.originalUrl,
                originalUrl = shareUrl,
                embedUrl = previewUrl,
                googleDriveFileId = fileId,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = false,
                failureCategory = PlaybackFailureCategory.NETWORK_FAILED,
                validationError = "Network connection failed while checking Google Drive: ${dlProbe.networkError}",
                probeSummary = dlProbe.networkError
            )
        }

        // Case 1: Google Drive redirected to Google Sign-In or returned 401/403
        val finalHost = VideoSourceUtils.parseNormalizedHost(dlProbe.finalUrl).orEmpty()
        val lowerSnippet = dlProbe.bodySnippet?.lowercase(Locale.US).orEmpty()

        if (finalHost.contains("accounts.google.com") ||
            dlProbe.statusCode == 401 ||
            dlProbe.statusCode == 403 ||
            lowerSnippet.contains("you need access") ||
            lowerSnippet.contains("request access") ||
            (lowerSnippet.contains("accounts.google.com/servicelogin"))
        ) {
            val isQuota = lowerSnippet.contains("too many users have viewed or downloaded") ||
                lowerSnippet.contains("quota exceeded") ||
                dlProbe.statusCode == 429
            if (isQuota) {
                return base.copy(
                    url = shareUrl,
                    originalUrl = shareUrl,
                    embedUrl = null,
                    googleDriveFileId = fileId,
                    detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                    isDirectlyPlayable = false,
                    hasOfficialEmbed = false,
                    requiresAuthentication = false,
                    openExternallyLabel = "Open in Google Drive",
                    probeVerified = true,
                    failureCategory = PlaybackFailureCategory.DRIVE_PLAYBACK_UNAVAILABLE,
                    validationError = "Google Drive playback is temporarily unavailable for this file because too many users have viewed or downloaded it recently (quota limit).",
                    probeSummary = "Google Drive quota limit reached."
                )
            }
            return base.copy(
                url = shareUrl,
                originalUrl = shareUrl,
                embedUrl = null,
                googleDriveFileId = fileId,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                requiresAuthentication = true,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.DRIVE_NOT_PUBLIC,
                validationError = "Google Drive file is not publicly accessible. The owner must share it as 'Anyone with the link' or you can open it in Google Drive while signed in.",
                probeSummary = "Google Drive returned HTTP ${dlProbe.statusCode} / access restricted."
            )
        }

        // Case 2: Removed / Not Found (404 / 410)
        if (dlProbe.statusCode == 404 || dlProbe.statusCode == 410) {
            return base.copy(
                url = shareUrl,
                originalUrl = shareUrl,
                embedUrl = null,
                googleDriveFileId = fileId,
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.VIDEO_REMOVED_OR_NOT_FOUND,
                validationError = "Google Drive file was not found or has been removed (HTTP ${dlProbe.statusCode}).",
                probeSummary = "Google Drive file not found (HTTP ${dlProbe.statusCode})."
            )
        }

        // Case 3: Direct playable video response from Google Drive!
        val mime = dlProbe.normalizedMimeType
        val isDirectVideo = dlProbe.isSuccess && isPlayableDirectVideo(mime, dlProbe.contentDisposition, dlProbe.magicBytes)
        if (isDirectVideo) {
            val resolvedMime = when {
                mime != null && mime.startsWith("video/") -> mime
                else -> inferVideoMimeFromFilenameOrMagic(dlProbe.contentDisposition, dlProbe.magicBytes) ?: "video/mp4"
            }
            val supportsSeeking = dlProbe.supportsRangeRequests
            val seekNote = if (supportsSeeking) {
                "Byte-range seeking supported (HTTP ${dlProbe.statusCode})."
            } else {
                "Direct stream available (server did not advertise byte-range seeking)."
            }
            return ResolvedPlaybackSource(
                url = dlProbe.finalUrl,
                originalUrl = shareUrl,
                sourceType = VideoSourceType.MP4,
                detectedKind = DetectedSourceKind.DIRECT_MP4,
                providerName = base.providerName ?: "Google Drive",
                embedUrl = previewUrl, // Retained strictly as clean fallback if Drive drops mid-stream
                googleDriveFileId = fileId,
                mimeType = resolvedMime,
                isDirectlyPlayable = true,
                hasOfficialEmbed = true,
                requiresAuthentication = false,
                supportsByteRanges = supportsSeeking,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                probeSummary = "Direct Google Drive video ($resolvedMime) verified. $seekNote",
                infoMessage = "Google Drive direct video ($resolvedMime) — plays natively via Media3 ExoPlayer. $seekNote"
            )
        }

        // Case 4: Non-video binary file (e.g., PDF, image, ZIP)
        if (dlProbe.isSuccess && mime != null && !mime.startsWith("text/html") && !mime.contains("xhtml")) {
            return base.copy(
                url = shareUrl,
                originalUrl = shareUrl,
                embedUrl = null,
                googleDriveFileId = fileId,
                mimeType = mime,
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.UNSUPPORTED_FORMAT,
                validationError = "This Google Drive file is not a video (detected '$mime'). Share a video file or open it in Google Drive.",
                probeSummary = "Google Drive returned non-video Content-Type '$mime'."
            )
        }

        // Case 5: HTML response (e.g., Google Drive virus-scan / large-file confirmation page or quota notice)
        if (lowerSnippet.contains("too many users have viewed or downloaded") ||
            lowerSnippet.contains("quota exceeded")
        ) {
            return base.copy(
                url = shareUrl,
                originalUrl = shareUrl,
                embedUrl = null,
                googleDriveFileId = fileId,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.DRIVE_PLAYBACK_UNAVAILABLE,
                validationError = "Google Drive playback quota was exceeded for this file. Try again later or open it in Google Drive.",
                probeSummary = "Google Drive returned quota-exceeded HTML page."
            )
        }

        val isVirusScanOrConfirmPage = lowerSnippet.contains("can't scan this file for viruses") ||
            lowerSnippet.contains("cannot scan this file for viruses") ||
            lowerSnippet.contains("download anyway") ||
            lowerSnippet.contains("confirm=") ||
            lowerSnippet.contains("uc-download-link")

        // Respect Google Drive's confirmation wall (never bypass tokens); check if official /preview embed is accessible
        val previewProbe = httpClient.probe(previewUrl, useRangeHeader = false, readSnippetBytes = 4096)
        val previewHost = VideoSourceUtils.parseNormalizedHost(previewProbe.finalUrl).orEmpty()
        val previewSnippet = previewProbe.bodySnippet?.lowercase(Locale.US).orEmpty()

        val previewBlocked = previewProbe.networkError != null ||
            previewProbe.statusCode in listOf(401, 403, 404, 410) ||
            previewHost.contains("accounts.google.com") ||
            previewSnippet.contains("you need access") ||
            previewSnippet.contains("request access") ||
            isFramingDisallowed(previewProbe)

        if (!previewBlocked && previewProbe.isSuccess) {
            val reasonPrefix = if (isVirusScanOrConfirmPage) {
                "Google Drive requires a download confirmation page for this larger file, so direct ExoPlayer streaming is unavailable. "
            } else {
                "Google Drive did not serve a direct video stream for this file. "
            }
            return ResolvedPlaybackSource(
                url = shareUrl,
                originalUrl = shareUrl,
                sourceType = VideoSourceType.EMBED,
                detectedKind = DetectedSourceKind.SUPPORTED_EMBED,
                providerName = base.providerName ?: "Google Drive",
                embedUrl = previewUrl,
                googleDriveFileId = fileId,
                isDirectlyPlayable = false,
                hasOfficialEmbed = true,
                requiresAuthentication = false,
                supportsByteRanges = false,
                openExternallyLabel = "Open in Google Drive",
                probeVerified = true,
                probeSummary = "${reasonPrefix}Using official Google Drive preview player.",
                infoMessage = "${reasonPrefix}Playing via Google Drive's official preview embed (you can also tap 'Open in Google Drive')."
            )
        }

        val needsAuth = previewHost.contains("accounts.google.com") ||
            previewProbe.statusCode in listOf(401, 403) ||
            previewSnippet.contains("you need access")
        return base.copy(
            url = shareUrl,
            originalUrl = shareUrl,
            embedUrl = null,
            googleDriveFileId = fileId,
            detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
            isDirectlyPlayable = false,
            hasOfficialEmbed = false,
            requiresAuthentication = needsAuth,
            openExternallyLabel = "Open in Google Drive",
            probeVerified = true,
            failureCategory = if (needsAuth) PlaybackFailureCategory.DRIVE_NOT_PUBLIC else PlaybackFailureCategory.DRIVE_PLAYBACK_UNAVAILABLE,
            validationError = if (needsAuth) {
                "Google Drive file is not publicly accessible. Share the file as 'Anyone with the link' or open it in Google Drive."
            } else {
                "Google Drive playback is unavailable for this file (HTTP ${previewProbe.statusCode}). Open it in Google Drive or host an MP4/HLS stream."
            },
            probeSummary = "Google Drive preview check failed (HTTP ${previewProbe.statusCode})."
        )
    }

    private suspend fun resolveDirectOrPlaylist(
        base: ResolvedPlaybackSource,
        httpClient: HttpProbeClient
    ): ResolvedPlaybackSource {
        val probe = httpClient.probe(base.url, useRangeHeader = true, readSnippetBytes = 4096)
        if (probe.networkError != null) {
            return base.copy(
                probeVerified = false,
                failureCategory = PlaybackFailureCategory.NETWORK_FAILED,
                validationError = probe.networkError,
                probeSummary = probe.networkError
            )
        }

        if (probe.statusCode == 401 || probe.statusCode == 403) {
            return base.copy(
                isDirectlyPlayable = false,
                requiresAuthentication = true,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.AUTH_REQUIRED,
                validationError = "Provider requires authentication or denied access to this stream (HTTP ${probe.statusCode}).",
                probeSummary = "HTTP ${probe.statusCode}: authentication or authorization required."
            )
        }

        if (probe.statusCode == 404 || probe.statusCode == 410) {
            return base.copy(
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.VIDEO_REMOVED_OR_NOT_FOUND,
                validationError = "Video source was not found or has expired (HTTP ${probe.statusCode}).",
                probeSummary = "HTTP ${probe.statusCode}: video source not found or removed."
            )
        }

        if (probe.statusCode >= 400) {
            return base.copy(
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.NETWORK_FAILED,
                validationError = "Video server returned HTTP ${probe.statusCode}. Verify the URL is active and publicly accessible.",
                probeSummary = "HTTP ${probe.statusCode} error from media server."
            )
        }

        // Enforce https on redirect target
        if (probe.finalUrl.startsWith("http://", ignoreCase = true)) {
            return base.copy(
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.INVALID_URL,
                validationError = "This URL redirected to an insecure http:// address, which Android blocks.",
                probeSummary = "Redirected to insecure http:// URL."
            )
        }

        // Detect if a URL with .mp4/.m3u8/.mpd actually returned an HTML error/login page!
        val mime = probe.normalizedMimeType
        if (mime != null && (mime.startsWith("text/html") || mime.contains("xhtml"))) {
            return base.copy(
                url = probe.finalUrl,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                validationError = "Source is not a direct video stream — the server returned an HTML webpage instead of media data.",
                probeSummary = "Server returned '$mime' instead of a direct video or streaming manifest."
            )
        }

        // Reclassify if headers reveal HLS / DASH / specific video MIME
        val classified = classifyStreamFromProbe(base, probe)
        return classified.copy(
            probeVerified = true,
            supportsByteRanges = probe.supportsRangeRequests,
            probeSummary = buildString {
                append("Verified ${classified.detectedKind.label} (HTTP ${probe.statusCode}")
                if (!classified.mimeType.isNullOrBlank()) append(", ${classified.mimeType}")
                if (probe.redirectCount > 0) append(", ${probe.redirectCount} redirect(s)")
                if (probe.supportsRangeRequests && classified.detectedKind == DetectedSourceKind.DIRECT_MP4) {
                    append(", seeking supported")
                }
                append(").")
            }
        )
    }

    private suspend fun resolveOfficialEmbed(
        base: ResolvedPlaybackSource,
        declaredType: String?,
        httpClient: HttpProbeClient
    ): ResolvedPlaybackSource {
        val target = base.embedUrl ?: base.url
        val probe = httpClient.probe(target, useRangeHeader = false, readSnippetBytes = 4096)

        if (probe.networkError != null) {
            return base.copy(
                probeVerified = false,
                probeSummary = "Official embed endpoint (${base.providerName}); live probe skipped (${probe.networkError})."
            )
        }

        if (probe.statusCode == 404 || probe.statusCode == 410) {
            return base.copy(
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.VIDEO_REMOVED_OR_NOT_FOUND,
                validationError = "${base.providerName ?: "Provider"} reported this video was not found or removed (HTTP ${probe.statusCode}).",
                probeSummary = "Embed endpoint returned HTTP ${probe.statusCode}."
            )
        }

        if (probe.statusCode == 401 || probe.statusCode == 403) {
            return base.copy(
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                requiresAuthentication = true,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.PROVIDER_EMBED_DISABLED,
                validationError = "${base.providerName ?: "Provider"} rejected embedded playback (HTTP ${probe.statusCode}). The video may be private or prohibit embedding.",
                probeSummary = "Embed endpoint returned HTTP ${probe.statusCode}."
            )
        }

        // If an admin explicitly set EMBED on a site that actually forbids framing via X-Frame-Options/CSP:
        if (isFramingDisallowed(probe)) {
            val provider = base.providerName ?: "This provider"
            return base.copy(
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.PROVIDER_EMBED_DISABLED,
                validationError = "$provider does not allow embedded playback (blocked by X-Frame-Options / Content-Security-Policy). Tap '${base.openExternallyLabel}' instead.",
                probeSummary = "Framing blocked by response headers (${probe.xFrameOptions ?: "CSP frame-ancestors"})."
            )
        }

        // Check if the embed URL actually resolved to a direct video/HLS/DASH stream!
        val upgraded = maybeUpgradeFromMediaProbe(base, probe)
        if (upgraded != null) return upgraded

        val botNotice = detectAntiBotChallenge(probe.bodySnippet)
        if (botNotice != null && VideoSourceType.fromKey(declaredType) == VideoSourceType.EMBED) {
            return base.copy(
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                hasOfficialEmbed = false,
                isDirectlyPlayable = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.PROVIDER_EMBED_DISABLED,
                validationError = "${base.providerName ?: "This site"} requires browser verification ($botNotice) and cannot be embedded reliably. Tap '${base.openExternallyLabel}' to open it in your browser.",
                probeSummary = "Detected browser anti-bot challenge: $botNotice."
            )
        }

        return base.copy(
            probeVerified = true,
            hasOfficialEmbed = true,
            probeSummary = "Verified ${base.providerName ?: "official"} embedded player (HTTP ${probe.statusCode})."
        )
    }

    private suspend fun resolveWebpageOrUnknown(
        base: ResolvedPlaybackSource,
        declaredType: String?,
        httpClient: HttpProbeClient
    ): ResolvedPlaybackSource {
        val probe = httpClient.probe(base.url, useRangeHeader = true, readSnippetBytes = 4096)

        if (probe.networkError != null) {
            return base.copy(
                probeVerified = false,
                failureCategory = PlaybackFailureCategory.NETWORK_FAILED,
                validationError = probe.networkError,
                probeSummary = probe.networkError
            )
        }

        // Check if a redirect landed on a known provider (e.g., short link -> YouTube, Vimeo, Google Drive, or .mp4/.m3u8/.mpd!)
        if (probe.finalUrl != base.url && probe.finalUrl.startsWith("https://", ignoreCase = true)) {
            val redirectedStatic = VideoSourceUtils.detectClassification(probe.finalUrl, base.providerName)
            if (redirectedStatic.youtubeVideoId != null) {
                return resolveYouTube(redirectedStatic.copy(originalUrl = base.originalUrl), httpClient)
            }
            if (redirectedStatic.googleDriveFileId != null) {
                return resolveGoogleDrive(redirectedStatic.copy(originalUrl = base.originalUrl), httpClient)
            }
        }

        if (probe.statusCode == 401 || probe.statusCode == 403) {
            return base.copy(
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                requiresAuthentication = true,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.AUTH_REQUIRED,
                validationError = "${base.providerName ?: "This provider"} requires authentication or blocked access (HTTP ${probe.statusCode}).",
                probeSummary = "HTTP ${probe.statusCode}: authentication or authorization required."
            )
        }

        if (probe.statusCode == 404 || probe.statusCode == 410) {
            return base.copy(
                detectedKind = DetectedSourceKind.UNSUPPORTED,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.VIDEO_REMOVED_OR_NOT_FOUND,
                validationError = "Video source was not found or has been removed (HTTP ${probe.statusCode}).",
                probeSummary = "HTTP ${probe.statusCode}: URL not found or expired."
            )
        }

        // CRITICAL FIX: Extensionless direct streams & streaming playlists!
        // If HTTP response headers reveal a real video, HLS playlist, or DASH manifest, upgrade to native ExoPlayer!
        val upgraded = maybeUpgradeFromMediaProbe(base, probe)
        if (upgraded != null) {
            return upgraded
        }

        // Still an HTML webpage — give an accurate, provider-aware diagnosis
        val host = VideoSourceUtils.parseNormalizedHost(probe.finalUrl)
            ?: VideoSourceUtils.parseNormalizedHost(base.url).orEmpty()
        val provider = base.providerName ?: VideoSourceUtils.friendlyHostName(host)
        val botChallenge = detectAntiBotChallenge(probe.bodySnippet)
        val framingBlocked = isFramingDisallowed(probe)

        if (VideoSourceUtils.isFlystreamUrl(probe.finalUrl) || VideoSourceUtils.isFlystreamUrl(base.url)) {
            val detail = buildString {
                append("Flystream is a third-party streaming website without an official public embed API or direct MP4/HLS stream")
                if (botChallenge != null) {
                    append(", and protects its pages with browser verification ($botChallenge)")
                }
                append(". Notflix does not scrape or bypass provider protections — tap 'Open on Flystream' to view it in your browser.")
            }
            return base.copy(
                url = probe.finalUrl,
                providerName = provider,
                detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
                isDirectlyPlayable = false,
                hasOfficialEmbed = false,
                requiresAuthentication = false,
                openExternallyLabel = "Open on Flystream",
                probeVerified = true,
                failureCategory = PlaybackFailureCategory.NOT_DIRECT_VIDEO,
                infoMessage = detail,
                probeSummary = if (botChallenge != null) {
                    "Flystream returned HTML with browser verification ($botChallenge); no direct stream or official embed API."
                } else {
                    "Flystream returned an HTML webpage without a direct stream or official embed endpoint."
                }
            )
        }

        val explanation = when {
            framingBlocked ->
                "$provider does not allow embedded playback (${probe.xFrameOptions ?: "CSP frame-ancestors"}) and this URL is not a direct MP4/HLS/DASH stream. Tap '${base.openExternallyLabel}' to open it in your browser."
            botChallenge != null ->
                "$provider returned a browser verification page ($botChallenge) rather than a direct video stream. Tap '${base.openExternallyLabel}' to open it in your browser."
            else ->
                "$provider returned an ordinary HTML webpage rather than a direct MP4/HLS/DASH video stream or recognized official embed player. Tap '${base.openExternallyLabel}' to open it directly."
        }

        return base.copy(
            url = probe.finalUrl,
            providerName = provider,
            detectedKind = DetectedSourceKind.ORDINARY_WEBPAGE,
            isDirectlyPlayable = false,
            hasOfficialEmbed = false,
            probeVerified = true,
            failureCategory = if (framingBlocked) {
                PlaybackFailureCategory.PROVIDER_EMBED_DISABLED
            } else {
                PlaybackFailureCategory.NOT_DIRECT_VIDEO
            },
            infoMessage = explanation,
            probeSummary = "HTML webpage (HTTP ${probe.statusCode}, ${probe.normalizedMimeType ?: "text/html"}) — not a direct stream or official embed."
        )
    }

    private fun maybeUpgradeFromMediaProbe(
        base: ResolvedPlaybackSource,
        probe: HttpProbeResult
    ): ResolvedPlaybackSource? {
        if (!probe.isSuccess) return null
        if (probe.finalUrl.startsWith("http://", ignoreCase = true)) return null

        val mime = probe.normalizedMimeType
        val snippetTrim = probe.bodySnippet?.trimStart().orEmpty()

        // HLS manifest by Content-Type or #EXTM3U header
        val isHls = mime == "application/vnd.apple.mpegurl" ||
            mime == "application/x-mpegurl" ||
            mime == "audio/mpegurl" ||
            snippetTrim.startsWith("#EXTM3U")
        if (isHls) {
            return ResolvedPlaybackSource(
                url = probe.finalUrl,
                originalUrl = base.originalUrl,
                sourceType = VideoSourceType.HLS,
                detectedKind = DetectedSourceKind.HLS_STREAM,
                providerName = base.providerName ?: "HLS Stream",
                mimeType = "application/vnd.apple.mpegurl",
                isDirectlyPlayable = true,
                hasOfficialEmbed = false,
                supportsByteRanges = probe.supportsRangeRequests,
                probeVerified = true,
                probeSummary = "Detected HLS playlist via HTTP response (HTTP ${probe.statusCode}, ${mime ?: "#EXTM3U"}).",
                infoMessage = "Adaptive HLS stream resolved automatically — plays natively via Media3 ExoPlayer."
            )
        }

        // MPEG-DASH manifest by Content-Type or <MPD XML root
        val isDash = mime == "application/dash+xml" ||
            snippetTrim.contains("<MPD", ignoreCase = false)
        if (isDash) {
            return ResolvedPlaybackSource(
                url = probe.finalUrl,
                originalUrl = base.originalUrl,
                sourceType = VideoSourceType.DASH,
                detectedKind = DetectedSourceKind.DASH_STREAM,
                providerName = base.providerName ?: "MPEG-DASH Stream",
                mimeType = "application/dash+xml",
                isDirectlyPlayable = true,
                hasOfficialEmbed = false,
                supportsByteRanges = probe.supportsRangeRequests,
                probeVerified = true,
                probeSummary = "Detected MPEG-DASH manifest via HTTP response (HTTP ${probe.statusCode}, ${mime ?: "MPD XML"}).",
                infoMessage = "Adaptive MPEG-DASH stream resolved automatically — plays natively via Media3 ExoPlayer."
            )
        }

        // Direct video binary stream by Content-Type or magic bytes (extensionless CDN/redirect MP4/WebM!)
        if (isPlayableDirectVideo(mime, probe.contentDisposition, probe.magicBytes)) {
            val resolvedMime = when {
                mime != null && mime.startsWith("video/") -> mime
                else -> inferVideoMimeFromFilenameOrMagic(probe.contentDisposition, probe.magicBytes) ?: "video/mp4"
            }
            return ResolvedPlaybackSource(
                url = probe.finalUrl,
                originalUrl = base.originalUrl,
                sourceType = VideoSourceType.MP4,
                detectedKind = DetectedSourceKind.DIRECT_MP4,
                providerName = base.providerName ?: "Direct Video",
                mimeType = resolvedMime,
                isDirectlyPlayable = true,
                hasOfficialEmbed = false,
                supportsByteRanges = probe.supportsRangeRequests,
                probeVerified = true,
                probeSummary = "Detected direct video stream via HTTP headers (HTTP ${probe.statusCode}, $resolvedMime${if (probe.supportsRangeRequests) ", seeking supported" else ""}).",
                infoMessage = "Direct video stream ($resolvedMime) resolved automatically — plays natively via Media3 ExoPlayer."
            )
        }

        return null
    }

    private fun classifyStreamFromProbe(
        base: ResolvedPlaybackSource,
        probe: HttpProbeResult
    ): ResolvedPlaybackSource {
        val upgraded = maybeUpgradeFromMediaProbe(base, probe)
        if (upgraded != null) return upgraded
        return base.copy(
            url = probe.finalUrl,
            mimeType = probe.normalizedMimeType ?: base.mimeType,
            isDirectlyPlayable = true
        )
    }

    private fun isPlayableDirectVideo(
        normalizedMime: String?,
        contentDisposition: String?,
        magicBytes: ByteArray
    ): Boolean {
        if (normalizedMime != null) {
            if (normalizedMime.startsWith("video/")) return true
            if (normalizedMime == "application/mp4") return true
            if (normalizedMime != "application/octet-stream" && normalizedMime != "binary/octet-stream") {
                return false
            }
        }
        return inferVideoMimeFromFilenameOrMagic(contentDisposition, magicBytes) != null
    }

    private fun inferVideoMimeFromFilenameOrMagic(
        contentDisposition: String?,
        magicBytes: ByteArray
    ): String? {
        val dispLower = contentDisposition?.lowercase(Locale.US).orEmpty()
        when {
            dispLower.contains(".mp4") || dispLower.contains(".m4v") -> return "video/mp4"
            dispLower.contains(".webm") -> return "video/webm"
            dispLower.contains(".mov") -> return "video/quicktime"
            dispLower.contains(".mkv") -> return "video/x-matroska"
        }
        // Check ISO BMFF / MP4 "ftyp" signature at offset 4..7
        if (magicBytes.size >= 8 &&
            magicBytes[4] == 'f'.code.toByte() &&
            magicBytes[5] == 't'.code.toByte() &&
            magicBytes[6] == 'y'.code.toByte() &&
            magicBytes[7] == 'p'.code.toByte()
        ) {
            return "video/mp4"
        }
        // Check EBML (WebM / MKV) header 0x1A 0x45 0xDF 0xA3
        if (magicBytes.size >= 4 &&
            magicBytes[0] == 0x1A.toByte() &&
            magicBytes[1] == 0x45.toByte() &&
            magicBytes[2] == 0xDF.toByte() &&
            magicBytes[3] == 0xA3.toByte()
        ) {
            return "video/webm"
        }
        return null
    }

    internal fun isFramingDisallowed(probe: HttpProbeResult): Boolean {
        val xfo = probe.xFrameOptions?.trim()?.uppercase(Locale.US).orEmpty()
        if (xfo.contains("DENY") || xfo.contains("SAMEORIGIN")) return true

        val csp = probe.contentSecurityPolicy?.lowercase(Locale.US).orEmpty()
        val faIdx = csp.indexOf("frame-ancestors")
        if (faIdx >= 0) {
            val directive = csp.substring(faIdx).substringBefore(';')
            if (directive.contains("'none'") || directive.contains("'self'")) {
                return true
            }
        }
        return false
    }

    internal fun detectAntiBotChallenge(snippet: String?): String? {
        if (snippet.isNullOrBlank()) return null
        val lower = snippet.lowercase(Locale.US)
        return when {
            lower.contains("making sure you're not a bot") || lower.contains("making sure you&#39;re not a bot") ->
                "anti-bot check"
            lower.contains("just a moment...") && lower.contains("cloudflare") ->
                "Cloudflare browser check"
            lower.contains("cf-browser-verification") || lower.contains("challenges.cloudflare.com") ->
                "Cloudflare challenge"
            else -> null
        }
    }
}
