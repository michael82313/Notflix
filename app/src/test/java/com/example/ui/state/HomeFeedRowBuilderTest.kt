package com.example.ui.state

import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.HomepageSettings
import com.example.data.model.PlaybackProgress
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedRowBuilderTest {

    private fun movie(
        id: String,
        title: String,
        genres: List<String> = listOf("Drama"),
        tags: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
        director: String = "Director $id",
        cast: List<String> = emptyList(),
        year: Int = 2025,
        runtime: Int = 105,
        trendingScore: Int = 800,
        popularityRank: Int = 10,
        isFeatured: Boolean = false,
        isKidsFriendly: Boolean = false,
        isStaffPick: Boolean = false,
        isAwardWinning: Boolean = false,
        isComingSoon: Boolean = false,
        expectedReleaseDate: String? = null,
        leavingSoonAt: String? = null,
        createdAtSec: Long = 1_700_000_000L
    ) = CatalogItem(
        id = id,
        title = title,
        synopsis = "Synopsis for $title",
        mediaType = "movie",
        genres = genres,
        tags = tags,
        keywords = keywords,
        director = director,
        cast = cast,
        releaseYear = year,
        runtimeMinutes = runtime,
        ageRating = if (isKidsFriendly) "PG" else "PG-13",
        videoUrl = if (isComingSoon) "" else "https://example.com/$id.mp4",
        isFeatured = isFeatured,
        isVisible = true,
        isKidsFriendly = isKidsFriendly,
        trendingScore = trendingScore,
        popularityRank = popularityRank,
        isStaffPick = isStaffPick,
        isAwardWinning = isAwardWinning,
        isComingSoon = isComingSoon,
        expectedReleaseDate = expectedReleaseDate,
        leavingSoonAt = leavingSoonAt,
        createdAt = Timestamp(createdAtSec, 0),
        updatedAt = Timestamp(createdAtSec, 0)
    )

    private fun series(
        id: String,
        title: String,
        genres: List<String> = listOf("Sci-Fi"),
        tags: List<String> = emptyList(),
        keywords: List<String> = emptyList(),
        director: String = "Showrunner $id",
        year: Int = 2026,
        runtime: Int = 45,
        trendingScore: Int = 900,
        popularityRank: Int = 5,
        isFeatured: Boolean = false,
        isKidsFriendly: Boolean = false,
        ageRating: String = if (isKidsFriendly) "TV-G" else "TV-14",
        createdAtSec: Long = 1_700_000_000L
    ) = CatalogItem(
        id = id,
        title = title,
        synopsis = "Series synopsis for $title",
        mediaType = "series",
        genres = genres,
        tags = tags,
        keywords = keywords,
        director = director,
        releaseYear = year,
        runtimeMinutes = runtime,
        ageRating = ageRating,
        videoUrl = "https://example.com/$id.mp4",
        isFeatured = isFeatured,
        isVisible = true,
        isKidsFriendly = isKidsFriendly,
        trendingScore = trendingScore,
        popularityRank = popularityRank,
        createdAt = Timestamp(createdAtSec, 0),
        updatedAt = Timestamp(createdAtSec, 0)
    )

    private fun sampleCatalog(): List<CatalogItem> = listOf(
        movie(
            id = "m_scifi_1",
            title = "Neon Drift",
            genres = listOf("Sci-Fi", "Action"),
            tags = listOf("Cyberpunk", "Superhero"),
            keywords = listOf("orbit", "neon"),
            year = 2026,
            trendingScore = 980,
            popularityRank = 1,
            isFeatured = true,
            isStaffPick = true,
            isAwardWinning = true
        ),
        movie(
            id = "m_scifi_2",
            title = "Eclipse Protocol",
            genres = listOf("Sci-Fi", "Drama"),
            tags = listOf("Space"),
            keywords = listOf("orbit", "solar"),
            year = 2026,
            trendingScore = 905,
            popularityRank = 7,
            leavingSoonAt = "Nov 30"
        ),
        movie(
            id = "m_horror_1",
            title = "Hollow Signal",
            genres = listOf("Horror", "Mystery"),
            tags = listOf("Supernatural", "Dark Suspense"),
            keywords = listOf("haunted", "radio"),
            year = 2026,
            trendingScore = 930,
            popularityRank = 2,
            isFeatured = true
        ),
        movie(
            id = "m_comedy_1",
            title = "Turbo Couriers",
            genres = listOf("Comedy", "Adventure", "Romance"),
            tags = listOf("Feel-Good", "Rom-Com"),
            keywords = listOf("comedy", "city"),
            year = 2025,
            runtime = 24,
            trendingScore = 820,
            popularityRank = 6,
            isKidsFriendly = true,
            isStaffPick = true
        ),
        movie(
            id = "m_thriller_1",
            title = "Silent Vault",
            genres = listOf("Thriller", "Action"),
            tags = listOf("Crime", "Suspense"),
            year = 2024,
            trendingScore = 870,
            popularityRank = 4
        ),
        movie(
            id = "m_doc_1",
            title = "Deep Ocean Atlas",
            genres = listOf("Documentary"),
            year = 2025,
            runtime = 28,
            trendingScore = 760,
            popularityRank = 9,
            isKidsFriendly = true
        ),
        movie(
            id = "m_fantasy_1",
            title = "Crown of Ash",
            genres = listOf("Fantasy", "Drama"),
            tags = listOf("Feel-Good"),
            year = 2025,
            trendingScore = 840,
            popularityRank = 8,
            isAwardWinning = true
        ),
        movie(
            id = "m_coming_soon",
            title = "Solstice 2099",
            genres = listOf("Sci-Fi", "Adventure"),
            tags = listOf("Recently Announced"),
            year = 2027,
            runtime = 128,
            trendingScore = 880,
            popularityRank = 12,
            isComingSoon = true,
            expectedReleaseDate = "Jan 22, 2027"
        ),
        series(
            id = "s_scifi_1",
            title = "Orbital Horizon",
            genres = listOf("Sci-Fi", "Thriller"),
            tags = listOf("Binge-Worthy"),
            keywords = listOf("orbit", "station"),
            year = 2026,
            trendingScore = 970,
            popularityRank = 1,
            isFeatured = true
        ),
        series(
            id = "s_mystery_1",
            title = "Blackwater Manor",
            genres = listOf("Mystery", "Horror", "Drama"),
            tags = listOf("Binge-Worthy", "Crime"),
            keywords = listOf("haunted", "mansion"),
            year = 2026,
            trendingScore = 940,
            popularityRank = 3
        ),
        series(
            id = "s_kids_1",
            title = "Starlight Cadets",
            genres = listOf("Animation", "Adventure"),
            tags = listOf("Feel-Good"),
            year = 2025,
            runtime = 22,
            trendingScore = 890,
            popularityRank = 5,
            isKidsFriendly = true,
            ageRating = "TV-G"
        ),
        series(
            id = "s_anime_1",
            title = "Starblade Academy",
            genres = listOf("Anime", "Comedy", "Action"),
            tags = listOf("Anime", "Superhero", "Binge-Worthy"),
            year = 2026,
            runtime = 24,
            trendingScore = 925,
            popularityRank = 4,
            isKidsFriendly = true,
            ageRating = "TV-PG"
        )
    )

    @Test
    fun buildRows_strictFilteringZeroFakeHistoryAndEmailSafeProfileNaming() {
        val catalog = sampleCatalog()
        // Profile has no watch history, no ratings, no watchlist; email-formatted name falls back safely
        val rows = HomeFeedRowBuilder.buildRows(
            filteredCatalog = catalog,
            fullVisibleCatalog = catalog,
            recommendations = catalog,
            continueWatching = emptyList(),
            watchlistIds = emptySet(),
            progressList = emptyList(),
            ratings = emptyList(),
            searchHistory = emptyList(),
            genres = emptyList(),
            activeProfile = UserProfile(id = "p1", name = "owner@example.com"),
            selectedCategory = HomeCategoryFilter.ALL,
            selectedGenre = null
        )

        // Never leak account email in row titles or subtitles
        rows.forEach { row ->
            assertFalse("Email leaked in title: ${row.title}", row.title.contains("@"))
            assertFalse("Email leaked in subtitle: ${row.subtitle}", row.subtitle?.contains("@") == true)
        }

        // Zero fake history / because_you_watched rows when nothing was watched
        assertNull(rows.firstOrNull { it.id == "continue_watching" })
        assertNull(rows.firstOrNull { it.id == "watch_again" })
        assertNull(rows.firstOrNull { it.rowKey == "because_you_watched" })
        assertNull(rows.firstOrNull { it.id == "my_list" })

        // Horror Movies contains ONLY Horror movies
        val horrorRow = rows.firstOrNull { it.rowKey == "genre_horror_movies" }
        assertNotNull(horrorRow)
        assertEquals(listOf("m_horror_1"), horrorRow?.items?.map { it.id })

        // Anime Comedies contains ONLY titles matching both Anime and Comedy
        val animeComediesRow = rows.firstOrNull { it.rowKey == "genre_anime_comedies" }
        assertNotNull(animeComediesRow)
        assertEquals(listOf("s_anime_1"), animeComediesRow?.items?.map { it.id })

        // Comedy Movies excludes Horror/Sci-Fi/etc. and only includes Comedy movies
        val comedyRow = rows.firstOrNull { it.rowKey == "genre_comedy_movies" }
        assertEquals(listOf("m_comedy_1"), comedyRow?.items?.map { it.id })

        // Coming Soon is ONLY in coming_soon, NEVER leaked into playable shelves
        val comingSoonRow = rows.firstOrNull { it.rowKey == "coming_soon" }
        assertNotNull(comingSoonRow)
        assertEquals(HomeFeedRowType.COMING_SOON, comingSoonRow?.type)
        assertEquals(listOf("m_coming_soon"), comingSoonRow?.items?.map { it.id })

        rows.filter { it.type != HomeFeedRowType.COMING_SOON }.forEach { playableRow ->
            assertFalse(
                "Coming Soon leaked into ${playableRow.rowKey}",
                playableRow.items.any { it.id == "m_coming_soon" }
            )
        }
    }

    @Test
    fun buildRows_multipleProfilesReceiveIsolatedPersonalizedRowsAndMultipleBecauseYouWatchedShelves() {
        val catalog = sampleCatalog()
        val seriesEpisodes = mapOf(
            "s_mystery_1" to listOf(
                Episode(id = "ep1", seasonNumber = 1, episodeNumber = 1, title = "M1", isVisible = true),
                Episode(id = "ep2", seasonNumber = 1, episodeNumber = 2, title = "M2", isVisible = true)
            )
        )

        val jordanProfile = UserProfile(
            id = "prof_jordan",
            userId = "u1",
            name = "Jordan",
            isKids = false,
            preferredGenres = listOf("Sci-Fi", "Horror")
        )
        val jordanProgress = listOf(
            // Finished Sci-Fi movie
            PlaybackProgress(
                id = "m_scifi_1",
                contentId = "m_scifi_1",
                positionMs = 95_000L,
                durationMs = 100_000L,
                completed = true,
                updatedAt = Timestamp(400L, 0)
            ),
            // Finished Horror movie
            PlaybackProgress(
                id = "m_horror_1",
                contentId = "m_horror_1",
                positionMs = 92_000L,
                durationMs = 100_000L,
                completed = false,
                updatedAt = Timestamp(350L, 0)
            ),
            // Finished episode 1 of Blackwater Manor -> triggers Next Episodes (ep2)
            PlaybackProgress(
                id = "s_mystery_1_ep1",
                contentId = "s_mystery_1",
                episodeId = "ep1",
                seasonNumber = 1,
                episodeNumber = 1,
                positionMs = 98_000L,
                durationMs = 100_000L,
                completed = true,
                updatedAt = Timestamp(300L, 0)
            ),
            // In-progress movie
            PlaybackProgress(
                id = "m_thriller_1",
                contentId = "m_thriller_1",
                positionMs = 40_000L,
                durationMs = 100_000L,
                completed = false,
                updatedAt = Timestamp(500L, 0)
            )
        )

        val jordanRows = HomeFeedRowBuilder.buildRows(
            filteredCatalog = catalog,
            fullVisibleCatalog = catalog,
            recommendations = catalog.filter { it.isPlayableNow }.take(5),
            continueWatching = emptyList(),
            watchlistIds = setOf("m_scifi_2"),
            progressList = jordanProgress,
            ratings = listOf(ContentRating(contentId = "m_comedy_1", liked = 1, stars = 5)),
            searchHistory = listOf(SearchHistoryItem(query = "Turbo")),
            genres = listOf(Genre(id = "custom_western", name = "Western", isActive = true)),
            activeProfile = jordanProfile,
            selectedCategory = HomeCategoryFilter.ALL,
            selectedGenre = null,
            seriesEpisodesById = seriesEpisodes
        )

        assertTrue(jordanRows.any { it.title == "Continue Watching for Jordan" })
        assertTrue(jordanRows.any { it.rowKey == "next_episodes" && it.nextEpisodeEntries.first().nextEpisode.id == "ep2" })
        assertTrue(jordanRows.any { it.rowKey == "picked_for_you" && it.title == "Picked for Jordan" })
        assertTrue(jordanRows.any { it.rowKey == "watch_again" && it.items.map { i -> i.id } == listOf("m_scifi_1", "m_horror_1") })
        assertTrue(jordanRows.any { it.rowKey == "my_list" && it.items.map { i -> i.id } == listOf("m_scifi_2") })

        val becauseRows = jordanRows.filter { it.rowKey == "because_you_watched" }
        assertEquals(2, becauseRows.size)
        assertEquals(
            listOf("Because You Watched Neon Drift", "Because You Watched Hollow Signal"),
            becauseRows.map { it.title }
        )
        // Strict similarity check: Because You Watched Hollow Signal should never recommend unrelated Comedy m_comedy_1
        val becauseHorror = becauseRows.first { it.id == "because_watched_m_horror_1" }
        assertFalse(becauseHorror.items.any { it.id == "m_comedy_1" })
        assertTrue(becauseHorror.items.any { it.id == "s_mystery_1" })

        // Second profile (Kids profile Mia) with empty progress gets ZERO of Jordan's history or Horror rows
        val miaCatalog = catalog.filter { it.isKidsFriendly }
        val miaProfile = UserProfile(id = "prof_mia", userId = "u1", name = "Mia", isKids = true)
        val miaRows = HomeFeedRowBuilder.buildRows(
            filteredCatalog = miaCatalog,
            fullVisibleCatalog = miaCatalog,
            recommendations = miaCatalog,
            continueWatching = emptyList(),
            watchlistIds = emptySet(),
            progressList = emptyList(),
            ratings = emptyList(),
            searchHistory = emptyList(),
            genres = emptyList(),
            activeProfile = miaProfile,
            selectedCategory = HomeCategoryFilter.ALL,
            selectedGenre = null
        )

        assertNull(miaRows.firstOrNull { it.rowKey == "continue_watching" })
        assertNull(miaRows.firstOrNull { it.rowKey == "watch_again" })
        assertNull(miaRows.firstOrNull { it.rowKey == "because_you_watched" })
        assertNull(miaRows.firstOrNull { it.rowKey == "genre_horror_movies" })
        assertNotNull(miaRows.firstOrNull { it.rowKey == "genre_kids_tv" })
        miaRows.forEach { row ->
            assertTrue(row.items.all { it.isKidsFriendly })
        }
    }

    @Test
    fun buildRows_respectsAdminRowOrderVisibilityRenameCapsAndGenreFilters() {
        val catalog = sampleCatalog()
        val customRules = HomeFeedRowBuilder.defaultRowRules().map { rule ->
            when (rule.rowKey) {
                "staff_picks" -> rule.copy(displayOrder = 0, title = "Curator Picks for {profile}", enabled = true, maxItems = 1)
                "genre_horror_movies" -> rule.copy(displayOrder = 1, enabled = false)
                "popular_on_notflix" -> rule.copy(displayOrder = 2, enabled = true)
                else -> rule.copy(displayOrder = rule.displayOrder + 10)
            }
        }

        val settings = HomepageSettings(
            maxItemsPerRow = 2,
            maxBecauseYouWatchedRows = 1,
            enabledGenres = listOf("Action", "Sci-Fi", "Comedy")
        )

        val rows = HomeFeedRowBuilder.buildRows(
            filteredCatalog = catalog,
            fullVisibleCatalog = catalog,
            recommendations = catalog.filter { it.isPlayableNow },
            continueWatching = emptyList(),
            watchlistIds = emptySet(),
            progressList = emptyList(),
            ratings = emptyList(),
            searchHistory = emptyList(),
            genres = emptyList(),
            activeProfile = UserProfile(id = "p1", name = "Ava"),
            selectedCategory = HomeCategoryFilter.ALL,
            selectedGenre = null,
            homepageSettings = settings,
            homepageRowRules = customRules
        )

        // First row reflects admin reordering + {profile} formatting + maxItems cap (1)
        assertEquals("staff_picks", rows.first().rowKey)
        assertEquals("Curator Picks for Ava", rows.first().title)
        assertEquals(1, rows.first().items.size)

        // Disabled row and disabled genres are omitted
        assertNull(rows.firstOrNull { it.rowKey == "genre_horror_movies" })
        assertNull(rows.firstOrNull { it.rowKey == "genre_documentaries" })

        // Enabled genre rows are kept and capped at maxItemsPerRow (2)
        assertNotNull(rows.firstOrNull { it.rowKey == "genre_scifi_adventures" })
        rows.forEach { row ->
            assertTrue(row.items.size <= 2)
        }
    }
}
