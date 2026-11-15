package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun validateVideoSource_checksTypeMatchingAndHttps() {
        assertTrue(
            VideoSourceUtils.validateVideoSource(
                "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                VideoSourceType.YOUTUBE
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
            VideoSourceType.MP4
        )
        assertFalse(httpRes.isValid)
        assertTrue(httpRes.errorOrNull!!.contains("https://"))

        // Mismatched YouTube link under MP4
        val mismatchYt = VideoSourceUtils.validateVideoSource(
            "https://youtu.be/dQw4w9WgXcQ",
            VideoSourceType.MP4
        )
        assertFalse(mismatchYt.isValid)
        assertTrue(mismatchYt.errorOrNull!!.contains("YouTube Embed"))

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
    fun resolveForPlayback_routesYoutubeHlsMp4AndSurfacesHelpfulErrors() {
        val yt = VideoSourceUtils.resolveForPlayback(
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            "youtube"
        )
        assertEquals(VideoSourceType.YOUTUBE, yt.sourceType)
        assertEquals("aqz-KE-bpKQ", yt.youtubeVideoId)
        assertNull(yt.validationError)

        val hls = VideoSourceUtils.resolveForPlayback(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            null
        )
        assertEquals(VideoSourceType.HLS, hls.sourceType)
        assertNull(hls.validationError)

        val missing = VideoSourceUtils.resolveForPlayback("   ", "mp4")
        assertTrue(missing.validationError!!.contains("No playable video source"))
    }
}
