package com.example.ui.state

import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.PlaybackProgress
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionTrackerTest {

    @Test
    fun isPlaybackCompleted_guardsShortDurationsAndApplies90PercentThreshold() {
        // Duration under 20s is rejected even at 100%
        assertFalse(CompletionTracker.isPlaybackCompleted(positionMs = 15_000L, durationMs = 15_000L))
        // Position under 10s is rejected
        assertFalse(CompletionTracker.isPlaybackCompleted(positionMs = 9_000L, durationMs = 25_000L))
        // Just below 90%
        assertFalse(CompletionTracker.isPlaybackCompleted(positionMs = 89_000L, durationMs = 100_000L))
        // Exactly 90%
        assertTrue(CompletionTracker.isPlaybackCompleted(positionMs = 90_000L, durationMs = 100_000L))
        // Above 90%
        assertTrue(CompletionTracker.isPlaybackCompleted(positionMs = 98_500L, durationMs = 100_000L))
    }

    @Test
    fun analyzeWatchHistory_removesCompletedEpisodesFromContinueWatchingAndDetectsNextEpisodeAndFinishedShow() {
        val movieDone = CatalogItem(
            id = "m_done",
            title = "Completed Film",
            mediaType = "movie",
            genres = listOf("Sci-Fi"),
            isVisible = true,
            isComingSoon = false
        )
        val movieInProgress = CatalogItem(
            id = "m_wip",
            title = "In Progress Film",
            mediaType = "movie",
            genres = listOf("Action"),
            isVisible = true,
            isComingSoon = false
        )
        val comingSoon = CatalogItem(
            id = "m_soon",
            title = "Coming Soon Film",
            mediaType = "movie",
            genres = listOf("Sci-Fi"),
            isVisible = true,
            isComingSoon = true
        )
        val activeSeries = CatalogItem(
            id = "s_active",
            title = "Active Series",
            mediaType = "series",
            genres = listOf("Mystery"),
            isVisible = true,
            isComingSoon = false
        )
        val finishedSeries = CatalogItem(
            id = "s_finished",
            title = "Finished Series",
            mediaType = "series",
            genres = listOf("Thriller"),
            isVisible = true,
            isComingSoon = false
        )

        val episodesBySeries = mapOf(
            "s_active" to listOf(
                Episode(id = "s1e1", seasonNumber = 1, episodeNumber = 1, title = "Ep 1", isVisible = true),
                Episode(id = "s1e2", seasonNumber = 1, episodeNumber = 2, title = "Ep 2", isVisible = true),
                Episode(id = "s2e1", seasonNumber = 2, episodeNumber = 1, title = "S2 Ep 1", isVisible = true)
            ),
            "s_finished" to listOf(
                Episode(id = "f_e1", seasonNumber = 1, episodeNumber = 1, title = "F1", isVisible = true),
                Episode(id = "f_e2", seasonNumber = 1, episodeNumber = 2, title = "F2", isVisible = true)
            )
        )

        val progress = listOf(
            // Movie finished at 92% (even if completed boolean wasn't persisted yet)
            PlaybackProgress(
                id = "m_done",
                contentId = "m_done",
                title = "Completed Film",
                positionMs = 92_000L,
                durationMs = 100_000L,
                completed = false,
                updatedAt = Timestamp(300L, 0)
            ),
            // Movie paused mid-way
            PlaybackProgress(
                id = "m_wip",
                contentId = "m_wip",
                title = "In Progress Film",
                positionMs = 45_000L,
                durationMs = 100_000L,
                completed = false,
                updatedAt = Timestamp(250L, 0)
            ),
            // Active series finished S1E1 -> should NOT be in continueWatching, should suggest S1E2
            PlaybackProgress(
                id = "s_active_s1e1",
                contentId = "s_active",
                episodeId = "s1e1",
                seasonNumber = 1,
                episodeNumber = 1,
                title = "Active Series S1E1",
                positionMs = 95_000L,
                durationMs = 100_000L,
                completed = true,
                updatedAt = Timestamp(400L, 0)
            ),
            // Finished series finale completed -> should mark show finished, NOT in continueWatching
            PlaybackProgress(
                id = "s_finished_f_e2",
                contentId = "s_finished",
                episodeId = "f_e2",
                seasonNumber = 1,
                episodeNumber = 2,
                title = "Finished Series Ep 2",
                positionMs = 100_000L,
                durationMs = 100_000L,
                completed = true,
                updatedAt = Timestamp(500L, 0)
            ),
            // Coming soon bogus progress should be ignored completely
            PlaybackProgress(
                id = "m_soon",
                contentId = "m_soon",
                title = "Coming Soon Film",
                positionMs = 50_000L,
                durationMs = 100_000L,
                completed = false,
                updatedAt = Timestamp(600L, 0)
            )
        )

        val summary = CompletionTracker.analyzeWatchHistory(
            catalog = listOf(movieDone, movieInProgress, comingSoon, activeSeries, finishedSeries),
            progressList = progress,
            episodesBySeriesId = episodesBySeries
        )

        // Continue Watching contains ONLY unfinished playable titles
        assertEquals(listOf("m_wip"), summary.continueWatching.map { it.catalogItem.id })

        // Next Episodes suggests S1E2 for s_active
        assertEquals(1, summary.nextEpisodes.size)
        assertEquals("s_active", summary.nextEpisodes.first().catalogItem.id)
        assertEquals("s1e2", summary.nextEpisodes.first().nextEpisode.id)

        // Active series includes s_active, not s_finished
        assertEquals(listOf("s_active"), summary.activeSeriesInProgress.map { it.id })

        // Finished items ordered by recency: s_finished (500), m_done (300)
        assertEquals(listOf("s_finished", "m_done"), summary.finishedItems.map { it.id })
        assertEquals(listOf("s_finished"), summary.finishedSeries.map { it.id })
    }
}
