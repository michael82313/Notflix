package com.example.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSourceResolverTest {

    private class FakeHttpProbeClient(
        private val responder: (url: String, useRangeHeader: Boolean) -> HttpProbeResult
    ) : HttpProbeClient {
        val requestedUrls = mutableListOf<String>()

        override suspend fun probe(
            url: String,
            useRangeHeader: Boolean,
            readSnippetBytes: Int
        ): HttpProbeResult {
            requestedUrls += url
            return responder(url, useRangeHeader)
        }
    }

    private fun okProbe(
        finalUrl: String,
        statusCode: Int = 200,
        contentType: String? = "text/html; charset=utf-8",
        acceptRanges: String? = null,
        contentRange: String? = null,
        contentLength: Long? = null,
        contentDisposition: String? = null,
        xFrameOptions: String? = null,
        csp: String? = null,
        bodySnippet: String? = null,
        magicBytes: ByteArray = ByteArray(0),
        redirectCount: Int = 0
    ) = HttpProbeResult(
        statusCode = statusCode,
        finalUrl = finalUrl,
        contentType = contentType,
        acceptRanges = acceptRanges,
        contentRange = contentRange,
        contentLength = contentLength,
        contentDisposition = contentDisposition,
        xFrameOptions = xFrameOptions,
        contentSecurityPolicy = csp,
        bodySnippet = bodySnippet,
        magicBytes = magicBytes,
        redirectCount = redirectCount
    )

    // Case 1: Direct MP4 URL
    @Test
    fun resolve_case1_directMp4Url_resolvesToNativeExoPlayerWithByteRanges() = runBlocking {
        val url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 206,
                contentType = "video/mp4",
                acceptRanges = "bytes",
                contentRange = "bytes 0-2047/2499823"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals(DetectedSourceKind.DIRECT_MP4, resolved.detectedKind)
        assertEquals(VideoSourceType.MP4, resolved.sourceType)
        assertEquals("video/mp4", resolved.mimeType)
        assertTrue(resolved.isDirectlyPlayable)
        assertTrue(resolved.supportsByteRanges)
        assertTrue(resolved.probeVerified)
        assertNull(resolved.validationError)
        assertNull(resolved.failureCategory)
    }

    // Extensionless direct MP4 URL (must NOT be misclassified as External Webpage!)
    @Test
    fun resolve_extensionlessRedirectToMp4_upgradesToDirectNativePlayback() = runBlocking {
        val url = "https://cdn.example.com/stream/episode-42?token=abc"
        val mp4Magic = byteArrayOf(0x00, 0x00, 0x00, 0x18, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
        val client = FakeHttpProbeClient { _, _ ->
            okProbe(
                finalUrl = "https://edge-1.cdn.example.com/media/abc123",
                statusCode = 206,
                contentType = "video/mp4",
                acceptRanges = "bytes",
                contentRange = "bytes 0-2047/9000000",
                magicBytes = mp4Magic,
                redirectCount = 1
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals(DetectedSourceKind.DIRECT_MP4, resolved.detectedKind)
        assertEquals(VideoSourceType.MP4, resolved.sourceType)
        assertEquals("https://edge-1.cdn.example.com/media/abc123", resolved.url)
        assertEquals("video/mp4", resolved.mimeType)
        assertTrue(resolved.isDirectlyPlayable)
        assertTrue(resolved.supportsByteRanges)
        assertNull(resolved.validationError)
    }

    // Case 2: Direct HLS M3U8 URL
    @Test
    fun resolve_case2_directHlsM3u8Url_resolvesToNativeHlsExoPlayer() = runBlocking {
        val url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "application/vnd.apple.mpegurl",
                bodySnippet = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=1280000\nurl_0/index.m3u8"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals(DetectedSourceKind.HLS_STREAM, resolved.detectedKind)
        assertEquals(VideoSourceType.HLS, resolved.sourceType)
        assertEquals("application/vnd.apple.mpegurl", resolved.mimeType)
        assertTrue(resolved.isDirectlyPlayable)
        assertTrue(resolved.probeVerified)
        assertNull(resolved.validationError)
    }

    // Case 3: Supported DASH URL (.mpd)
    @Test
    fun resolve_case3_supportedDashMpdUrl_resolvesToNativeDashExoPlayerAndFirestoreCompatibleKey() = runBlocking {
        val url = "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "application/dash+xml",
                bodySnippet = "<?xml version=\"1.0\"?><MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\"></MPD>"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals(DetectedSourceKind.DASH_STREAM, resolved.detectedKind)
        assertEquals(VideoSourceType.DASH, resolved.sourceType)
        assertEquals("application/dash+xml", resolved.mimeType)
        assertTrue(resolved.isDirectlyPlayable)
        assertTrue(resolved.probeVerified)
        assertNull(resolved.validationError)

        // Ensure Firestore rules compatibility (rules allow ['auto', 'youtube', 'embed', 'mp4', 'hls'])
        assertEquals("auto", VideoSourceUtils.normalizeFirestoreSourceType("dash"))
        assertEquals("auto", VideoSourceUtils.normalizeFirestoreSourceType("mpd"))
    }

    // Case 4: YouTube video with embedding enabled
    @Test
    fun resolve_case4_youtubeEmbeddingEnabled_verifiesOfficialIFramePlayer() = runBlocking {
        val url = "https://www.youtube.com/watch?v=aqz-KE-bpKQ"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            assertTrue(reqUrl.startsWith("https://www.youtube.com/oembed"))
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "application/json",
                bodySnippet = """{"title":"Big Buck Bunny","provider_name":"YouTube"}"""
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, resolved.detectedKind)
        assertEquals("YouTube", resolved.providerName)
        assertEquals("aqz-KE-bpKQ", resolved.youtubeVideoId)
        assertTrue(resolved.hasOfficialEmbed)
        assertTrue(resolved.probeVerified)
        assertNull(resolved.validationError)
        assertNull(resolved.failureCategory)
    }

    // Case 5: YouTube video with embedding disabled
    @Test
    fun resolve_case5_youtubeEmbeddingDisabled_reportsEmbedDisabledClearly() = runBlocking {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 401,
                contentType = "text/plain",
                bodySnippet = "Unauthorized"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals("YouTube", resolved.providerName)
        assertEquals(DetectedSourceKind.ORDINARY_WEBPAGE, resolved.detectedKind)
        assertFalse(resolved.hasOfficialEmbed)
        assertEquals(PlaybackFailureCategory.PROVIDER_EMBED_DISABLED, resolved.failureCategory)
        assertNotNull(resolved.validationError)
        assertTrue(resolved.validationError!!.contains("embedding disabled", ignoreCase = true))
        assertEquals("Open on YouTube", resolved.openExternallyLabel)
    }

    // Case 6: Google Drive public video share link (direct 206 video/mp4 stream + preview fallback)
    @Test
    fun resolve_case6_googleDrivePublicVideo_resolvesToDirectNativeStreamWithPreviewFallback() = runBlocking {
        val shareUrl = "https://drive.google.com/file/d/1za1PGZTMjgrYp-aQQ8-Fgr-no4q02jYj/view?usp=sharing"
        val mp4Magic = byteArrayOf(0x00, 0x00, 0x00, 0x20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
        val client = FakeHttpProbeClient { reqUrl, useRange ->
            assertTrue(reqUrl.contains("drive.usercontent.google.com/download?id=1za1PGZTMjgrYp-aQQ8-Fgr-no4q02jYj"))
            assertTrue(useRange)
            okProbe(
                finalUrl = reqUrl,
                statusCode = 206,
                contentType = "video/mp4",
                acceptRanges = "bytes",
                contentRange = "bytes 0-2047/832168",
                contentDisposition = """attachment; filename="Soul.mp4"""",
                magicBytes = mp4Magic
            )
        }

        val resolved = PlaybackSourceResolver.resolve(shareUrl, "auto", httpClient = client)

        assertEquals("Google Drive", resolved.providerName)
        assertEquals(DetectedSourceKind.DIRECT_MP4, resolved.detectedKind)
        assertEquals("video/mp4", resolved.mimeType)
        assertTrue(resolved.isDirectlyPlayable)
        assertTrue(resolved.hasOfficialEmbed)
        assertTrue(resolved.supportsByteRanges)
        assertFalse(resolved.requiresAuthentication)
        assertEquals("https://drive.google.com/file/d/1za1PGZTMjgrYp-aQQ8-Fgr-no4q02jYj/preview", resolved.embedUrl)
        assertEquals("Open in Google Drive", resolved.openExternallyLabel)
        assertNull(resolved.validationError)
    }

    // Google Drive large file virus-scan confirmation page -> respects Google wall, uses official /preview embed
    @Test
    fun resolve_googleDriveLargeFileVirusScanWall_fallsBackToOfficialPreviewEmbed() = runBlocking {
        val shareUrl = "https://drive.google.com/file/u/0/d/1LargeVideoFileId123456/view?usp=sharing"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            if (reqUrl.contains("drive.usercontent.google.com")) {
                okProbe(
                    finalUrl = reqUrl,
                    statusCode = 200,
                    contentType = "text/html; charset=utf-8",
                    bodySnippet = "<html><body>Google Drive can't scan this file for viruses. Download anyway</body></html>"
                )
            } else {
                okProbe(
                    finalUrl = reqUrl,
                    statusCode = 200,
                    contentType = "text/html; charset=utf-8",
                    bodySnippet = "<html><head><title>Preview</title></head></html>"
                )
            }
        }

        val resolved = PlaybackSourceResolver.resolve(shareUrl, "auto", httpClient = client)

        assertEquals("Google Drive", resolved.providerName)
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, resolved.detectedKind)
        assertFalse(resolved.isDirectlyPlayable)
        assertTrue(resolved.hasOfficialEmbed)
        assertEquals("https://drive.google.com/file/d/1LargeVideoFileId123456/preview", resolved.embedUrl)
        assertEquals("Open in Google Drive", resolved.openExternallyLabel)
        assertNull(resolved.validationError)
    }

    // Case 7: Google Drive inaccessible / private video
    @Test
    fun resolve_case7_googleDrivePrivateOrInaccessible_reportsAuthRequiredAndOpenInDrive() = runBlocking {
        val shareUrl = "https://drive.google.com/file/d/1PrivateDriveFileId99999/view?usp=sharing"
        val client = FakeHttpProbeClient { _, _ ->
            okProbe(
                finalUrl = "https://accounts.google.com/v3/signin/identifier",
                statusCode = 200,
                contentType = "text/html; charset=utf-8",
                bodySnippet = "<html><body>Sign in to continue to Google Drive</body></html>"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(shareUrl, "auto", httpClient = client)

        assertEquals("Google Drive", resolved.providerName)
        assertFalse(resolved.isDirectlyPlayable)
        assertFalse(resolved.hasOfficialEmbed)
        assertTrue(resolved.requiresAuthentication)
        assertEquals(PlaybackFailureCategory.DRIVE_NOT_PUBLIC, resolved.failureCategory)
        assertEquals("Open in Google Drive", resolved.openExternallyLabel)
        assertNotNull(resolved.validationError)
        assertTrue(resolved.validationError!!.contains("Anyone with the link", ignoreCase = true))
    }

    // Case 7b: Google Drive quota exceeded & non-video file
    @Test
    fun resolve_googleDriveQuotaAndNonVideo_handledCleanly() = runBlocking {
        val quotaClient = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "text/html; charset=utf-8",
                bodySnippet = "Sorry, you can't view or download this file at this time. Too many users have viewed or downloaded this file recently."
            )
        }
        val quotaRes = PlaybackSourceResolver.resolve(
            "https://drive.google.com/open?id=1QuotaLimitedFileId123",
            "auto",
            httpClient = quotaClient
        )
        assertEquals(PlaybackFailureCategory.DRIVE_PLAYBACK_UNAVAILABLE, quotaRes.failureCategory)
        assertTrue(quotaRes.validationError!!.contains("quota", ignoreCase = true))

        val pdfClient = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "application/pdf"
            )
        }
        val pdfRes = PlaybackSourceResolver.resolve(
            "https://drive.google.com/uc?id=1PdfDocumentId123456&export=download",
            "auto",
            httpClient = pdfClient
        )
        assertEquals(PlaybackFailureCategory.UNSUPPORTED_FORMAT, pdfRes.failureCategory)
        assertTrue(pdfRes.validationError!!.contains("not a video", ignoreCase = true))
    }

    // Case 8: Ordinary webpage URL
    @Test
    fun resolve_case8_ordinaryWebpage_reportsNonEmbeddableWebpageCleanly() = runBlocking {
        val url = "https://example.com/movies/sample-article"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "text/html; charset=utf-8",
                xFrameOptions = "SAMEORIGIN",
                bodySnippet = "<html><head><title>Example Domain</title></head><body>Hello</body></html>"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals(DetectedSourceKind.ORDINARY_WEBPAGE, resolved.detectedKind)
        assertFalse(resolved.isDirectlyPlayable)
        assertFalse(resolved.hasOfficialEmbed)
        assertEquals(PlaybackFailureCategory.PROVIDER_EMBED_DISABLED, resolved.failureCategory)
        assertEquals("Open Website", resolved.openExternallyLabel)
        assertNotNull(resolved.infoMessage)
    }

    // Case 9: Flystream URL (anti-bot HTML page, no official embed API)
    @Test
    fun resolve_case9_flystreamUrl_identifiesProviderAndAntiBotWebpageWithoutScraping() = runBlocking {
        val url = "https://flystream.net/tv/119051?play=1"
        val client = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 200,
                contentType = "text/html; charset=utf-8",
                bodySnippet = "<!doctype html><html><head><title>Making sure you're not a bot!</title></head></html>"
            )
        }

        val resolved = PlaybackSourceResolver.resolve(url, "auto", httpClient = client)

        assertEquals("Flystream", resolved.providerName)
        assertEquals(DetectedSourceKind.ORDINARY_WEBPAGE, resolved.detectedKind)
        assertFalse(resolved.isDirectlyPlayable)
        assertFalse(resolved.hasOfficialEmbed)
        assertEquals("Open on Flystream", resolved.openExternallyLabel)
        assertEquals(PlaybackFailureCategory.NOT_DIRECT_VIDEO, resolved.failureCategory)
        assertTrue(resolved.infoMessage!!.contains("anti-bot", ignoreCase = true))
    }

    // Case 10: Invalid or expired video URL (404 / insecure http:// / malformed)
    @Test
    fun resolve_case10_invalidAndExpiredUrls_reportActionableErrors() = runBlocking {
        val notFoundClient = FakeHttpProbeClient { reqUrl, _ ->
            okProbe(
                finalUrl = reqUrl,
                statusCode = 404,
                contentType = "text/plain",
                bodySnippet = "Not Found"
            )
        }

        val expiredMp4 = PlaybackSourceResolver.resolve(
            "https://cdn.example.com/expired-video.mp4",
            "auto",
            httpClient = notFoundClient
        )
        assertFalse(expiredMp4.isDirectlyPlayable)
        assertEquals(PlaybackFailureCategory.VIDEO_REMOVED_OR_NOT_FOUND, expiredMp4.failureCategory)
        assertNotNull(expiredMp4.validationError)
        assertTrue(expiredMp4.validationError!!.contains("404"))

        val insecureHttp = PlaybackSourceResolver.resolve(
            "http://example.com/movie.mp4",
            "auto",
            httpClient = notFoundClient
        )
        assertEquals(DetectedSourceKind.UNSUPPORTED, insecureHttp.detectedKind)
        assertEquals(PlaybackFailureCategory.INVALID_URL, insecureHttp.failureCategory)
        assertNotNull(insecureHttp.validationError)

        val malformed = PlaybackSourceResolver.resolve(
            "not a valid url",
            "auto",
            httpClient = notFoundClient
        )
        assertEquals(DetectedSourceKind.UNSUPPORTED, malformed.detectedKind)
        assertEquals(PlaybackFailureCategory.INVALID_URL, malformed.failureCategory)
        assertNotNull(malformed.validationError)
    }
}
