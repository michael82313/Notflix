package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSourceUtilsTest {

    @Test
    fun extractYouTubeVideoId_handlesWatchShortEmbedAndShortsUrls() {
        assertEquals(
            "aqz-KE-bpKQ",
            VideoSourceUtils.extractYouTubeVideoId("https://www.youtube.com/watch?v=aqz-KE-bpKQ&t=12s")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            VideoSourceUtils.extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ?si=test")
        )
        assertEquals(
            "aqz-KE-bpKQ",
            VideoSourceUtils.extractYouTubeVideoId("https://www.youtube.com/embed/aqz-KE-bpKQ")
        )
        assertEquals(
            "aqz-KE-bpKQ",
            VideoSourceUtils.extractYouTubeVideoId("https://youtube.com/shorts/aqz-KE-bpKQ")
        )
        assertNull(VideoSourceUtils.extractYouTubeVideoId("https://example.com/movie.mp4"))
        assertNull(VideoSourceUtils.extractYouTubeVideoId("https://www.youtube.com/watch?v=short"))
    }

    @Test
    fun detectClassification_identifiesAllFiveSourceKindsAccurately() {
        val mp4 = VideoSourceUtils.detectClassification(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        )
        assertEquals(DetectedSourceKind.DIRECT_MP4, mp4.detectedKind)
        assertEquals(VideoSourceType.MP4, mp4.sourceType)

        val hls = VideoSourceUtils.detectClassification(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        )
        assertEquals(DetectedSourceKind.HLS_STREAM, hls.detectedKind)
        assertEquals(VideoSourceType.HLS, hls.sourceType)

        val youtube = VideoSourceUtils.detectClassification(
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ"
        )
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, youtube.detectedKind)
        assertEquals("YouTube", youtube.providerName)
        assertEquals("aqz-KE-bpKQ", youtube.youtubeVideoId)

        val vimeo = VideoSourceUtils.detectClassification("https://vimeo.com/76979871")
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, vimeo.detectedKind)
        assertEquals("Vimeo", vimeo.providerName)
        assertTrue(vimeo.embedUrl!!.startsWith("https://player.vimeo.com/video/76979871"))

        val dailymotion = VideoSourceUtils.detectClassification("https://www.dailymotion.com/video/x8m3abc")
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, dailymotion.detectedKind)
        assertEquals("Dailymotion", dailymotion.providerName)
        assertTrue(dailymotion.embedUrl!!.contains("/embed/video/x8m3abc"))

        val drive = VideoSourceUtils.detectClassification(
            "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQr/view?usp=sharing"
        )
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, drive.detectedKind)
        assertEquals("Google Drive", drive.providerName)
        assertEquals("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQr/preview", drive.embedUrl)

        val webpage = VideoSourceUtils.detectClassification(
            "https://example.org/streaming/movie-landing-page"
        )
        assertEquals(DetectedSourceKind.ORDINARY_WEBPAGE, webpage.detectedKind)
        assertNotNull(webpage.infoMessage)

        val httpUnsupported = VideoSourceUtils.detectClassification("http://example.com/movie.mp4")
        assertEquals(DetectedSourceKind.UNSUPPORTED, httpUnsupported.detectedKind)
        assertNotNull(httpUnsupported.validationError)
    }

    @Test
    fun validateVideoSource_checksAutoEmbedMp4HlsAndHttps() {
        assertTrue(
            VideoSourceUtils.validateVideoSource(
                "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                VideoSourceType.AUTO
            ).isValid
        )
        assertTrue(
            VideoSourceUtils.validateVideoSource(
                "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                VideoSourceType.EMBED
            ).isValid
        )
        assertTrue(
            VideoSourceUtils.validateVideoSource(
                "https://vimeo.com/76979871",
                VideoSourceType.EMBED
            ).isValid
        )
        assertTrue(
            VideoSourceUtils.validateVideoSource(
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                VideoSourceType.MP4
            ).isValid
        )
        assertTrue(
            VideoSourceUtils.validateVideoSource(
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                VideoSourceType.HLS
            ).isValid
        )

        // Cleartext http:// rejected
        val httpRes = VideoSourceUtils.validateVideoSource(
            "http://example.com/video.mp4",
            VideoSourceType.AUTO
        )
        assertFalse(httpRes.isValid)
        assertTrue(httpRes.errorOrNull!!.contains("https://"))

        // Mismatched YouTube link under MP4
        val mismatchYt = VideoSourceUtils.validateVideoSource(
            "https://youtu.be/dQw4w9WgXcQ",
            VideoSourceType.MP4
        )
        assertFalse(mismatchYt.isValid)
        assertTrue(mismatchYt.errorOrNull!!.contains("YouTube"))

        // Ordinary webpage cannot be marked as direct MP4
        val mismatchWeb = VideoSourceUtils.validateVideoSource(
            "https://example.org/watch/some-movie",
            VideoSourceType.MP4
        )
        assertFalse(mismatchWeb.isValid)
        assertTrue(mismatchWeb.errorOrNull!!.contains("webpage"))

        // Mismatched HLS manifest under MP4
        val mismatchHls = VideoSourceUtils.validateVideoSource(
            "https://cdn.example.com/live/master.m3u8",
            VideoSourceType.MP4
        )
        assertFalse(mismatchHls.isValid)
        assertTrue(mismatchHls.errorOrNull!!.contains("HLS"))

        // MP4 URL under HLS
        val mp4UnderHls = VideoSourceUtils.validateVideoSource(
            "https://cdn.example.com/video.mp4",
            VideoSourceType.HLS
        )
        assertFalse(mp4UnderHls.isValid)
        assertTrue(mp4UnderHls.errorOrNull!!.contains("MP4"))
    }

    @Test
    fun resolveForPlayback_routesAutoEmbedHlsMp4WebpageAndProviderName() {
        val yt = VideoSourceUtils.resolveForPlayback(
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            "youtube"
        )
        assertEquals(VideoSourceType.EMBED, yt.sourceType)
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, yt.detectedKind)
        assertEquals("aqz-KE-bpKQ", yt.youtubeVideoId)
        assertNull(yt.validationError)

        val hls = VideoSourceUtils.resolveForPlayback(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "auto"
        )
        assertEquals(VideoSourceType.HLS, hls.sourceType)
        assertEquals(DetectedSourceKind.HLS_STREAM, hls.detectedKind)
        assertNull(hls.validationError)

        val customProvider = VideoSourceUtils.resolveForPlayback(
            "https://vimeo.com/76979871",
            "auto",
            "Notflix Partner Vimeo"
        )
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, customProvider.detectedKind)
        assertEquals("Notflix Partner Vimeo", customProvider.providerName)

        // Legacy entry saved as "mp4" pointing to an ordinary webpage is NOT assumed to be MP4
        val mislabeledWebpage = VideoSourceUtils.resolveForPlayback(
            "https://example.com/movie-page",
            "mp4"
        )
        assertEquals(DetectedSourceKind.ORDINARY_WEBPAGE, mislabeledWebpage.detectedKind)

        // Explicit EMBED on compatible page loads embedded endpoint
        val explicitEmbed = VideoSourceUtils.resolveForPlayback(
            "https://example.com/partner-player/123",
            "embed",
            "Partner Host"
        )
        assertEquals(DetectedSourceKind.SUPPORTED_EMBED, explicitEmbed.detectedKind)
        assertEquals("https://example.com/partner-player/123", explicitEmbed.embedUrl)
        assertEquals("Partner Host", explicitEmbed.providerName)

        val missing = VideoSourceUtils.resolveForPlayback("   ", "auto")
        assertTrue(missing.validationError!!.contains("No playable"))
    }
}
