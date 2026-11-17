package com.example.ui.state

import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.HomepageRowRule
import com.example.data.model.HomepageSettings
import com.example.data.model.PlaybackProgress
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserProfile

enum class HomeFeedRowType {
    CONTINUE_WATCHING,
    NEXT_EPISODES,
    TOP_TEN,
    COMING_SOON,
    STANDARD_CAROUSEL
}

data class HomeFeedRow(
    val id: String,
    val rowKey: String,
    val title: String,
    val subtitle: String? = null,
    val type: HomeFeedRowType = HomeFeedRowType.STANDARD_CAROUSEL,
    val items: List<CatalogItem> = emptyList(),
    val continueEntries: List<ContinueWatchingEntry> = emptyList(),
    val nextEpisodeEntries: List<NextEpisodeEntry> = emptyList(),
    val badgeTag: String? = null,
    val isPersonalized: Boolean = false
)

object HomeFeedRowBuilder {

    private const val MIN_SIMILARITY_SCORE = 90

    private val MANDATORY_PERSONALIZED_KEYS = setOf(
        "continue_watching",
        "next_episodes",
        "because_you_watched",
        "because_you_liked",
        "picked_for_you",
        "we_think_youll_love",
        "your_next_watch",
        "watch_again",
        "your_favorites",
        "based_on_history",
        "more_like_favorites",
        "continue_your_series",
        "your_recently_finished_shows",
        "my_list",
        "weekend_watchlist"
    )

    fun defaultRowRules(): List<HomepageRowRule> = listOf(
        HomepageRowRule(id = "continue_watching", rowKey = "continue_watching", title = "Continue Watching for {profile}", enabled = true, displayOrder = 0, maxItems = 12, personalized = true),
        HomepageRowRule(id = "next_episodes", rowKey = "next_episodes", title = "Next Episodes for You", enabled = true, displayOrder = 1, maxItems = 10, personalized = true),
        HomepageRowRule(id = "todays_top_picks", rowKey = "todays_top_picks", title = "Today's Top Picks", enabled = true, displayOrder = 2, maxItems = 8, personalized = true),
        HomepageRowRule(id = "because_you_watched", rowKey = "because_you_watched", title = "Because You Watched", enabled = true, displayOrder = 3, maxItems = 10, personalized = true),
        HomepageRowRule(id = "top_10_trending", rowKey = "top_10_trending", title = "Top 10 Trending", enabled = true, displayOrder = 4, maxItems = 10, personalized = false),
        HomepageRowRule(id = "picked_for_you", rowKey = "picked_for_you", title = "Picked for {profile}", enabled = true, displayOrder = 5, maxItems = 10, personalized = true),
        HomepageRowRule(id = "popular_on_notflix", rowKey = "popular_on_notflix", title = "Popular on Notflix", enabled = true, displayOrder = 6, maxItems = 8, personalized = false),
        HomepageRowRule(id = "watch_again", rowKey = "watch_again", title = "Watch Again", enabled = true, displayOrder = 7, maxItems = 12, personalized = true),
        HomepageRowRule(id = "new_on_notflix", rowKey = "new_on_notflix", title = "New on Notflix", enabled = true, displayOrder = 8, maxItems = 8, personalized = false),
        HomepageRowRule(id = "recently_added", rowKey = "recently_added", title = "Recently Added", enabled = true, displayOrder = 9, maxItems = 8, personalized = false),
        HomepageRowRule(id = "because_you_liked", rowKey = "because_you_liked", title = "Because You Liked", enabled = true, displayOrder = 10, maxItems = 10, personalized = true),
        HomepageRowRule(id = "cant_stop_one_episode", rowKey = "cant_stop_one_episode", title = "Can't Stop at Just One Episode", enabled = true, displayOrder = 11, maxItems = 8, personalized = false),
        HomepageRowRule(id = "we_think_youll_love", rowKey = "we_think_youll_love", title = "We Think You'll Love These", enabled = true, displayOrder = 12, maxItems = 8, personalized = true),
        HomepageRowRule(id = "coming_soon", rowKey = "coming_soon", title = "Coming Soon", enabled = true, displayOrder = 13, maxItems = 10, personalized = false),
        HomepageRowRule(id = "my_list", rowKey = "my_list", title = "My List", enabled = true, displayOrder = 14, maxItems = 15, personalized = true),
        HomepageRowRule(id = "continue_your_series", rowKey = "continue_your_series", title = "Continue Your Series", enabled = true, displayOrder = 15, maxItems = 10, personalized = true),
        HomepageRowRule(id = "your_recently_finished_shows", rowKey = "your_recently_finished_shows", title = "Your Recently Finished Shows", enabled = true, displayOrder = 16, maxItems = 10, personalized = true),
        HomepageRowRule(id = "your_next_watch", rowKey = "your_next_watch", title = "Your Next Watch", enabled = true, displayOrder = 17, maxItems = 8, personalized = true),
        HomepageRowRule(id = "based_on_history", rowKey = "based_on_history", title = "Based on Your Viewing History", enabled = true, displayOrder = 18, maxItems = 8, personalized = true),
        HomepageRowRule(id = "your_favorites", rowKey = "your_favorites", title = "Your Favorites", enabled = true, displayOrder = 19, maxItems = 10, personalized = true),
        HomepageRowRule(id = "more_like_favorites", rowKey = "more_like_favorites", title = "More Like Your Favorites", enabled = true, displayOrder = 20, maxItems = 8, personalized = true),
        HomepageRowRule(id = "most_watched_this_week", rowKey = "most_watched_this_week", title = "Most Watched This Week", enabled = true, displayOrder = 21, maxItems = 6, personalized = false),
        HomepageRowRule(id = "everyone_is_watching", rowKey = "everyone_is_watching", title = "Everyone Is Watching", enabled = true, displayOrder = 22, maxItems = 6, personalized = false),
        HomepageRowRule(id = "staff_picks", rowKey = "staff_picks", title = "Staff Picks", enabled = true, displayOrder = 23, maxItems = 8, personalized = false),
        HomepageRowRule(id = "notflix_favorites", rowKey = "notflix_favorites", title = "Notflix Favorites", enabled = true, displayOrder = 24, maxItems = 8, personalized = false),
        HomepageRowRule(id = "top_rated", rowKey = "top_rated", title = "Top Rated on Notflix", enabled = true, displayOrder = 25, maxItems = 8, personalized = false),
        HomepageRowRule(id = "hidden_gems", rowKey = "hidden_gems", title = "Hidden Gems", enabled = true, displayOrder = 26, maxItems = 8, personalized = false),
        HomepageRowRule(id = "top_searches", rowKey = "top_searches", title = "Top Searches", enabled = true, displayOrder = 27, maxItems = 8, personalized = true),
        HomepageRowRule(id = "new_episodes", rowKey = "new_episodes", title = "New Episodes", enabled = true, displayOrder = 28, maxItems = 8, personalized = false),
        HomepageRowRule(id = "binge_worthy_shows", rowKey = "binge_worthy_shows", title = "Binge-Worthy Shows", enabled = true, displayOrder = 29, maxItems = 8, personalized = false),
        HomepageRowRule(id = "popular_tv_shows", rowKey = "popular_tv_shows", title = "Popular TV Shows", enabled = true, displayOrder = 30, maxItems = 8, personalized = false),
        HomepageRowRule(id = "trending_tv_shows", rowKey = "trending_tv_shows", title = "Trending TV Shows", enabled = true, displayOrder = 31, maxItems = 8, personalized = false),
        HomepageRowRule(id = "genre_kids_tv", rowKey = "genre_kids_tv", title = "Exciting Kids' TV", enabled = true, displayOrder = 32, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_horror_movies", rowKey = "genre_horror_movies", title = "Horror Movies", enabled = true, displayOrder = 33, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_action_adventure", rowKey = "genre_action_adventure", title = "Action & Adventure", enabled = true, displayOrder = 34, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_anime_favorites", rowKey = "genre_anime_favorites", title = "Anime Favorites", enabled = true, displayOrder = 35, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_anime_comedies", rowKey = "genre_anime_comedies", title = "Anime Comedies", enabled = true, displayOrder = 36, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_comedy_movies", rowKey = "genre_comedy_movies", title = "Comedy Movies", enabled = true, displayOrder = 37, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_scifi_adventures", rowKey = "genre_scifi_adventures", title = "Sci-Fi Adventures", enabled = true, displayOrder = 38, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_mystery_suspense", rowKey = "genre_mystery_suspense", title = "Mystery & Suspense", enabled = true, displayOrder = 39, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_family_movie_night", rowKey = "genre_family_movie_night", title = "Family Movie Night", enabled = true, displayOrder = 40, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_animated_favorites", rowKey = "genre_animated_favorites", title = "Animated Favorites", enabled = true, displayOrder = 41, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_thrillers", rowKey = "genre_thrillers", title = "Thrillers", enabled = true, displayOrder = 42, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_romantic_comedies", rowKey = "genre_romantic_comedies", title = "Romantic Comedies", enabled = true, displayOrder = 43, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_documentaries", rowKey = "genre_documentaries", title = "Documentaries", enabled = true, displayOrder = 44, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_fantasy_worlds", rowKey = "genre_fantasy_worlds", title = "Fantasy Worlds", enabled = true, displayOrder = 45, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_superhero", rowKey = "genre_superhero", title = "Superhero Adventures", enabled = true, displayOrder = 46, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_crime_mystery", rowKey = "genre_crime_mystery", title = "Crime & Mystery", enabled = true, displayOrder = 47, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_adventure_movies", rowKey = "genre_adventure_movies", title = "Adventure Movies", enabled = true, displayOrder = 48, maxItems = 10, personalized = false),
        HomepageRowRule(id = "genre_feel_good", rowKey = "genre_feel_good", title = "Feel-Good Movies", enabled = true, displayOrder = 49, maxItems = 10, personalized = false),
        HomepageRowRule(id = "recently_announced", rowKey = "recently_announced", title = "Recently Announced", enabled = true, displayOrder = 50, maxItems = 8, personalized = false),
        HomepageRowRule(id = "leaving_soon", rowKey = "leaving_soon", title = "Leaving Soon", enabled = true, displayOrder = 51, maxItems = 8, personalized = false),
        HomepageRowRule(id = "weekend_watchlist", rowKey = "weekend_watchlist", title = "Weekend Watchlist", enabled = true, displayOrder = 52, maxItems = 10, personalized = true),
        HomepageRowRule(id = "short_movies", rowKey = "short_movies", title = "Short Movies for a Quick Watch", enabled = true, displayOrder = 53, maxItems = 8, personalized = false),
        HomepageRowRule(id = "award_winning", rowKey = "award_winning", title = "Award-Winning Movies", enabled = true, displayOrder = 54, maxItems = 8, personalized = false),
        HomepageRowRule(id = "new_releases", rowKey = "new_releases", title = "New Releases", enabled = true, displayOrder = 55, maxItems = 8, personalized = false),
        HomepageRowRule(id = "top_10_movies", rowKey = "top_10_movies", title = "Top 10 Movies", enabled = true, displayOrder = 56, maxItems = 10, personalized = false),
        HomepageRowRule(id = "top_10_series", rowKey = "top_10_series", title = "Top 10 TV Shows", enabled = true, displayOrder = 57, maxItems = 10, personalized = false)
    )

    fun mergeWithDefaultRules(remote: List<HomepageRowRule>): List<HomepageRowRule> {
        val defaults = defaultRowRules()
        val remoteByKey = remote.associateBy { it.rowKey }
        val orderedRemote = remote.sortedBy { it.displayOrder }
        val missingDefaults = defaults.filter { it.rowKey !in remoteByKey }
            .mapIndexed { idx, rule -> rule.copy(displayOrder = orderedRemote.size + idx) }
        return orderedRemote + missingDefaults
    }

    fun resolveFinishedCatalogItems(
        catalog: List<CatalogItem>,
        progressList: List<PlaybackProgress>,
        seriesEpisodesById: Map<String, List<Episode>> = emptyMap()
    ): List<CatalogItem> =
        CompletionTracker.analyzeWatchHistory(catalog, progressList, seriesEpisodesById).finishedItems

    fun buildRows(
        filteredCatalog: List<CatalogItem>,
        fullVisibleCatalog: List<CatalogItem>,
        recommendations: List<CatalogItem>,
        continueWatching: List<ContinueWatchingEntry>,
        watchlistIds: Set<String>,
        progressList: List<PlaybackProgress>,
        ratings: List<ContentRating>,
        searchHistory: List<SearchHistoryItem>,
        genres: List<Genre>,
        activeProfile: UserProfile?,
        selectedCategory: HomeCategoryFilter,
        selectedGenre: String?,
        homepageSettings: HomepageSettings = HomepageSettings(),
        homepageRowRules: List<HomepageRowRule> = defaultRowRules(),
        seriesEpisodesById: Map<String, List<Episode>> = emptyMap()
    ): List<HomeFeedRow> {
        if (filteredCatalog.isEmpty() && fullVisibleCatalog.isEmpty() && continueWatching.isEmpty()) {
            return emptyList()
        }

        // Strict active profile name only (never account email address)
        val profileDisplayName = activeProfile?.name
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.contains('@') }
            ?: "Viewer"

        val isKidsProfile = activeProfile?.isKids == true
        val playableFiltered = filteredCatalog.filter { it.isPlayableNow }
        val playableFull = fullVisibleCatalog.filter { it.isPlayableNow }
        val comingSoonFiltered = filteredCatalog.filter { it.isVisible && it.isComingSoon }

        val watchSummary = CompletionTracker.analyzeWatchHistory(
            catalog = playableFull,
            progressList = progressList,
            episodesBySeriesId = seriesEpisodesById
        )

        val dislikedIds = ratings.filter { it.liked < 0 }.map { it.contentId }.toSet()
        val likedIds = ratings.filter { it.liked > 0 || it.stars >= 4 }.map { it.contentId }.toSet()
        val watchedContentIds = progressList.map { it.contentId }.toSet()

        // Active profile taste affinity weights (strictly scoped to activeProfile)
        val affinityWeights = buildMap {
            activeProfile?.preferredGenres?.forEach { g ->
                val key = g.trim().lowercase()
                if (key.isNotBlank()) put(key, (get(key) ?: 0) + 35)
            }
            playableFull.filter { it.id in watchedContentIds || it.id in likedIds }.forEach { item ->
                val boost = if (item.id in likedIds) 40 else 25
                (item.genres + item.tags).forEach { token ->
                    val key = token.trim().lowercase()
                    if (key.isNotBlank()) put(key, (get(key) ?: 0) + boost)
                }
            }
        }

        val enabledGenresLower = homepageSettings.enabledGenres
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        fun isGenreEnabled(vararg candidates: String): Boolean {
            if (enabledGenresLower.isEmpty()) return true
            return candidates.any { it.lowercase() in enabledGenresLower }
        }

        val rulesToApply = homepageRowRules
            .sortedBy { it.displayOrder }
            .distinctBy { it.rowKey }

        val rows = mutableListOf<HomeFeedRow>()
        val usedRowIds = mutableSetOf<String>()
        val usedBecauseRecommendIds = mutableSetOf<String>()
        val maxBecauseCount = homepageSettings.maxBecauseYouWatchedRows.coerceIn(0, 10)
        val finishedSeeds = watchSummary.finishedItems.take(maxBecauseCount)
        val finishedSeedIds = finishedSeeds.map { it.id }.toSet()

        fun capFor(rule: HomepageRowRule): Int =
            rule.maxItems.coerceAtMost(homepageSettings.maxItemsPerRow).coerceIn(1, 30)

        fun applyCategoryAndGenreFilter(items: List<CatalogItem>): List<CatalogItem> =
            items.filter { item ->
                matchesHomeCategory(item, selectedCategory) &&
                    (selectedGenre == null || item.genres.any { it.equals(selectedGenre, ignoreCase = true) })
            }

        for (rule in rulesToApply) {
            if (!rule.enabled) continue
            val isEffectivePersonalized = rule.personalized || rule.rowKey in MANDATORY_PERSONALIZED_KEYS
            val cap = capFor(rule)
            val resolvedTitle = formatRowTitle(rule.title, profileDisplayName)

            when (rule.rowKey) {
                "continue_watching" -> {
                    val entries = watchSummary.continueWatching
                        .filter { entry ->
                            matchesHomeCategory(entry.catalogItem, selectedCategory) &&
                                (selectedGenre == null || entry.catalogItem.genres.any { it.equals(selectedGenre, ignoreCase = true) })
                        }
                        .take(cap)
                    if (entries.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = "continue_watching",
                            rowKey = rule.rowKey,
                            title = if (rule.title == "Continue Watching") "Continue Watching for $profileDisplayName" else resolvedTitle,
                            subtitle = "Pick up right where $profileDisplayName left off",
                            type = HomeFeedRowType.CONTINUE_WATCHING,
                            continueEntries = entries,
                            isPersonalized = true
                        )
                    }
                }

                "next_episodes" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val nextEps = watchSummary.nextEpisodes
                            .filter { entry ->
                                matchesHomeCategory(entry.catalogItem, selectedCategory) &&
                                    (selectedGenre == null || entry.catalogItem.genres.any { it.equals(selectedGenre, ignoreCase = true) })
                            }
                            .take(cap)
                        if (nextEps.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = "next_episodes",
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                subtitle = "Ready to continue with the next unwatched episode",
                                type = HomeFeedRowType.NEXT_EPISODES,
                                nextEpisodeEntries = nextEps,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "because_you_watched" -> {
                    // Strictly uses ONLY finished movies & finished shows watched by the active profile.
                    finishedSeeds.forEach { seed ->
                        val simPool = if (playableFiltered.isNotEmpty()) playableFiltered else playableFull
                        val matches = computeSimilarForSeed(
                            seed = seed,
                            pool = simPool,
                            dislikedIds = dislikedIds,
                            isKids = isKidsProfile,
                            prioritizeNovelOverIds = usedBecauseRecommendIds
                        ).take(cap)

                        if (matches.isNotEmpty()) {
                            usedBecauseRecommendIds.addAll(matches.map { it.id })
                            val rowId = "because_watched_${seed.id}"
                            if (usedRowIds.add(rowId)) {
                                rows += HomeFeedRow(
                                    id = rowId,
                                    rowKey = rule.rowKey,
                                    title = "Because You Watched ${seed.title}",
                                    subtitle = if (seed.isSeries) {
                                        "Since $profileDisplayName finished this series"
                                    } else {
                                        "Based on genres & themes in ${seed.title}"
                                    },
                                    items = matches,
                                    isPersonalized = true
                                )
                            }
                        }
                    }
                }

                "because_you_liked" -> {
                    val likedSeeds = playableFull
                        .filter { it.id in likedIds && it.id !in finishedSeedIds }
                        .take(2)
                    likedSeeds.forEach { seed ->
                        val simPool = if (playableFiltered.isNotEmpty()) playableFiltered else playableFull
                        val matches = computeSimilarForSeed(
                            seed = seed,
                            pool = simPool,
                            dislikedIds = dislikedIds,
                            isKids = isKidsProfile,
                            prioritizeNovelOverIds = usedBecauseRecommendIds
                        ).take(cap)
                        if (matches.isNotEmpty()) {
                            usedBecauseRecommendIds.addAll(matches.map { it.id })
                            val rowId = "because_liked_${seed.id}"
                            if (usedRowIds.add(rowId)) {
                                rows += HomeFeedRow(
                                    id = rowId,
                                    rowKey = rule.rowKey,
                                    title = "Because You Liked ${seed.title}",
                                    subtitle = "More titles matching what you rated highly",
                                    items = matches,
                                    isPersonalized = true
                                )
                            }
                        }
                    }
                }

                "top_10_trending" -> {
                    val items = playableFiltered
                        .filter { it.trendingScore >= 820 || it.popularityRank <= 10 }
                        .sortedWith(compareByDescending<CatalogItem> { it.trendingScore }.thenBy { it.popularityRank })
                        .take(cap.coerceAtMost(10))
                    if (items.size >= 3) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            subtitle = "Most watched right now",
                            type = HomeFeedRowType.TOP_TEN,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "top_10_movies" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES) {
                        val items = playableFiltered
                            .filter { !it.isSeries }
                            .sortedWith(compareBy<CatalogItem> { it.popularityRank }.thenByDescending { it.trendingScore })
                            .take(cap.coerceAtMost(10))
                        if (items.size >= 3) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                type = HomeFeedRowType.TOP_TEN,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "top_10_series" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = playableFiltered
                            .filter { it.isSeries }
                            .sortedWith(compareBy<CatalogItem> { it.popularityRank }.thenByDescending { it.trendingScore })
                            .take(cap.coerceAtMost(10))
                        if (items.size >= 3) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                type = HomeFeedRowType.TOP_TEN,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "todays_top_picks" -> {
                    val items = recommendations
                        .filter { it.isPlayableNow }
                        .let(::applyCategoryAndGenreFilter)
                        .take(cap.coerceAtMost(8))
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            subtitle = "Curated for $profileDisplayName",
                            items = items,
                            isPersonalized = true
                        )
                    }
                }

                "picked_for_you" -> {
                    val hasSignals = affinityWeights.isNotEmpty()
                    if (hasSignals) {
                        val items = playableFiltered
                            .asSequence()
                            .filter { it.id !in dislikedIds && it.id !in finishedSeedIds }
                            .map { item -> item to scoreAffinity(item, affinityWeights) }
                            .filter { (_, score) -> score > 0 }
                            .sortedByDescending { (item, score) -> score * 10 + item.trendingScore / 40 }
                            .map { (item, _) -> item }
                            .take(cap)
                            .toList()
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = if (rule.title == "Picked for You") "Picked for $profileDisplayName" else resolvedTitle,
                                subtitle = "Matched to $profileDisplayName's profile taste",
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "we_think_youll_love" -> {
                    if (affinityWeights.isNotEmpty()) {
                        val topPicksIds = recommendations.take(4).map { it.id }.toSet()
                        val items = playableFiltered
                            .asSequence()
                            .filter { it.id !in dislikedIds && it.id !in watchedContentIds && it.id !in topPicksIds }
                            .map { item -> item to scoreAffinity(item, affinityWeights) }
                            .filter { (_, score) -> score >= 25 }
                            .sortedByDescending { (item, score) -> score * 10 + item.trendingScore / 40 }
                            .map { (item, _) -> item }
                            .take(cap)
                            .toList()
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "your_next_watch" -> {
                    val anchor = watchSummary.finishedItems.firstOrNull()
                        ?: playableFull.firstOrNull { it.id in likedIds }
                        ?: playableFull.firstOrNull { it.id in watchlistIds }
                    if (anchor != null) {
                        val items = computeSimilarForSeed(
                            seed = anchor,
                            pool = playableFiltered.filter { it.id !in watchedContentIds },
                            dislikedIds = dislikedIds,
                            isKids = isKidsProfile
                        ).take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                subtitle = "Queued up from your recent activity",
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "based_on_history" -> {
                    if (progressList.isNotEmpty() && affinityWeights.isNotEmpty()) {
                        val items = playableFiltered
                            .asSequence()
                            .filter { it.id !in dislikedIds && it.id !in watchedContentIds }
                            .map { item -> item to scoreAffinity(item, affinityWeights) }
                            .filter { (_, score) -> score > 0 }
                            .sortedByDescending { (item, score) -> score * 10 + item.trendingScore / 50 }
                            .map { (item, _) -> item }
                            .take(cap)
                            .toList()
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "watch_again" -> {
                    val items = applyCategoryAndGenreFilter(watchSummary.finishedItems).take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            subtitle = "Completed titles for $profileDisplayName",
                            items = items,
                            badgeTag = "Watched",
                            isPersonalized = true
                        )
                    }
                }

                "your_favorites" -> {
                    val items = playableFiltered
                        .filter { it.id in likedIds }
                        .take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            subtitle = "Liked & top-rated by $profileDisplayName",
                            items = items,
                            isPersonalized = true
                        )
                    }
                }

                "more_like_favorites" -> {
                    val favSeeds = playableFull.filter { it.id in likedIds }
                    if (favSeeds.isNotEmpty()) {
                        val favIds = favSeeds.map { it.id }.toSet()
                        val pool = playableFiltered.filter { it.id !in favIds && it.id !in dislikedIds }
                        val items = favSeeds
                            .flatMap { seed ->
                                computeSimilarForSeed(seed, pool, dislikedIds, isKidsProfile)
                            }
                            .distinctBy { it.id }
                            .take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "popular_on_notflix" -> {
                    val items = playableFiltered
                        .filter { it.popularityRank <= 6 || it.trendingScore >= 900 }
                        .sortedWith(compareBy<CatalogItem> { it.popularityRank }.thenByDescending { it.trendingScore })
                        .take(cap.coerceAtMost(8))
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "most_watched_this_week" -> {
                    val items = playableFiltered
                        .filter { it.trendingScore >= 900 }
                        .sortedByDescending { it.trendingScore }
                        .take(cap)
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "everyone_is_watching" -> {
                    val items = playableFiltered
                        .filter { it.trendingScore >= 870 && it.popularityRank <= 6 }
                        .sortedWith(compareByDescending<CatalogItem> { it.trendingScore }.thenBy { it.popularityRank })
                        .take(cap)
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "new_on_notflix" -> {
                    val items = playableFiltered
                        .filter { it.releaseYear >= 2025 }
                        .sortedWith(
                            compareByDescending<CatalogItem> { it.releaseYear }
                                .thenByDescending { it.createdAt?.seconds ?: 0L }
                                .thenByDescending { it.trendingScore }
                        )
                        .take(cap)
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "new_releases" -> {
                    val items = playableFiltered
                        .filter { it.releaseYear >= 2026 }
                        .sortedByDescending { it.trendingScore }
                        .take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            badgeTag = "2026",
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "recently_added" -> {
                    val items = playableFiltered
                        .sortedWith(
                            compareByDescending<CatalogItem> { it.createdAt?.seconds ?: 0L }
                                .thenByDescending { it.updatedAt?.seconds ?: 0L }
                                .thenByDescending { it.releaseYear }
                        )
                        .take(cap.coerceAtMost(6))
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "hidden_gems" -> {
                    val items = playableFiltered
                        .filter {
                            (it.popularityRank >= 6 || hasToken(it, "Hidden Gem")) &&
                                !it.isFeatured &&
                                it.trendingScore >= 700
                        }
                        .sortedByDescending { it.trendingScore }
                        .take(cap)
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "notflix_favorites" -> {
                    val items = playableFiltered
                        .filter { it.isFeatured || it.popularityRank <= 3 }
                        .sortedWith(compareBy<CatalogItem> { it.popularityRank }.thenByDescending { it.trendingScore })
                        .take(cap)
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "staff_picks" -> {
                    val items = playableFiltered
                        .filter { it.isStaffPick || hasToken(it, "Staff Pick") }
                        .sortedByDescending { it.trendingScore }
                        .take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            badgeTag = "Staff Pick",
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "top_rated" -> {
                    val items = playableFiltered
                        .filter { it.trendingScore >= 910 || it.isAwardWinning }
                        .sortedByDescending { it.trendingScore }
                        .take(cap)
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "top_searches" -> {
                    val queries = searchHistory.map { it.query.trim().lowercase() }
                        .filter { it.length >= 2 }
                        .distinct()
                    if (queries.isNotEmpty()) {
                        val matched = playableFiltered.filter { item ->
                            queries.any { q ->
                                item.title.lowercase().contains(q) ||
                                    item.synopsis.lowercase().contains(q) ||
                                    item.genres.any { it.lowercase().contains(q) } ||
                                    item.tags.any { it.lowercase().contains(q) } ||
                                    item.keywords.any { it.lowercase().contains(q) } ||
                                    item.cast.any { it.lowercase().contains(q) }
                            }
                        }.take(cap)
                        if (matched.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                subtitle = "From $profileDisplayName's recent searches",
                                items = matched,
                                isPersonalized = true
                            )
                        }
                    } else {
                        val searchTrending = playableFiltered
                            .filter { it.trendingScore >= 890 }
                            .sortedByDescending { it.trendingScore }
                            .take(cap)
                        if (searchTrending.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = searchTrending,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "cant_stop_one_episode" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = playableFiltered
                            .filter {
                                it.isSeries && (
                                    hasToken(it, "Binge-Worthy") ||
                                        hasToken(it, "Thriller") ||
                                        hasToken(it, "Mystery") ||
                                        hasToken(it, "Sci-Fi")
                                    )
                            }
                            .sortedByDescending { it.trendingScore }
                            .take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "new_episodes" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = playableFiltered
                            .filter { it.isSeries && it.releaseYear >= 2025 }
                            .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                badgeTag = "New Ep",
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "binge_worthy_shows" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = playableFiltered
                            .filter { it.isSeries && (hasToken(it, "Binge-Worthy") || it.runtimeMinutes >= 40) }
                            .sortedByDescending { it.trendingScore }
                            .take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "popular_tv_shows" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = playableFiltered
                            .filter { it.isSeries && (it.popularityRank <= 5 || it.trendingScore >= 860) }
                            .sortedWith(compareBy<CatalogItem> { it.popularityRank }.thenByDescending { it.trendingScore })
                            .take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "trending_tv_shows" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = playableFiltered
                            .filter { it.isSeries && it.trendingScore >= 885 }
                            .sortedByDescending { it.trendingScore }
                            .take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "continue_your_series" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = applyCategoryAndGenreFilter(watchSummary.activeSeriesInProgress).take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "your_recently_finished_shows" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES) {
                        val items = applyCategoryAndGenreFilter(watchSummary.finishedSeries).take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                badgeTag = "Finished",
                                items = items,
                                isPersonalized = true
                            )
                        }
                    }
                }

                "my_list" -> {
                    val items = playableFiltered
                        .filter { it.id in watchlistIds }
                        .take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            subtitle = "Saved by $profileDisplayName",
                            items = items,
                            isPersonalized = true
                        )
                    }
                }

                "weekend_watchlist" -> {
                    val savedWatchlist = playableFiltered.filter { it.id in watchlistIds }
                    val items = if (savedWatchlist.size >= 2) {
                        savedWatchlist.take(cap)
                    } else if (watchlistIds.isNotEmpty()) {
                        val watchlistGenres = playableFull
                            .filter { it.id in watchlistIds }
                            .flatMap { it.genres }
                            .map { it.lowercase() }
                            .toSet()
                        playableFiltered
                            .filter { it.id in watchlistIds || it.genres.any { g -> g.lowercase() in watchlistGenres } }
                            .take(cap)
                    } else emptyList()
                    if (items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            items = items,
                            isPersonalized = true
                        )
                    }
                }

                "coming_soon" -> {
                    val items = comingSoonFiltered.take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            subtitle = "Upcoming releases — watch trailers & save reminders",
                            type = HomeFeedRowType.COMING_SOON,
                            items = items,
                            badgeTag = "Coming Soon",
                            isPersonalized = false
                        )
                    }
                }

                "recently_announced" -> {
                    val items = filteredCatalog
                        .filter { it.isVisible && (it.isComingSoon || hasToken(it, "Recently Announced")) }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .take(cap)
                    if (items.isNotEmpty() && comingSoonFiltered.isNotEmpty() && items.any { it.id !in comingSoonFiltered.map { c -> c.id } } || items.size >= 2) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            type = HomeFeedRowType.COMING_SOON,
                            items = items,
                            badgeTag = "Announced",
                            isPersonalized = false
                        )
                    }
                }

                "leaving_soon" -> {
                    val items = playableFiltered
                        .filter { !it.leavingSoonAt.isNullOrBlank() || hasToken(it, "Leaving Soon") }
                        .take(cap)
                    if (items.isNotEmpty()) {
                        rows += HomeFeedRow(
                            id = rule.rowKey,
                            rowKey = rule.rowKey,
                            title = resolvedTitle,
                            badgeTag = "Leaving Soon",
                            items = items,
                            isPersonalized = isEffectivePersonalized
                        )
                    }
                }

                "short_movies" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES) {
                        val items = playableFiltered
                            .filter { !it.isSeries && it.runtimeMinutes <= 35 }
                            .sortedBy { it.runtimeMinutes }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                "award_winning" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES) {
                        val items = playableFiltered
                            .filter { !it.isSeries && (it.isAwardWinning || hasToken(it, "Award-Winning")) }
                            .sortedByDescending { it.trendingScore }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(
                                id = rule.rowKey,
                                rowKey = rule.rowKey,
                                title = resolvedTitle,
                                badgeTag = "Award Winner",
                                items = items,
                                isPersonalized = isEffectivePersonalized
                            )
                        }
                    }
                }

                // --- Strict Genre Rows ---
                "genre_kids_tv" -> {
                    if (selectedCategory != HomeCategoryFilter.MOVIES && isGenreEnabled("Animation", "Adventure", "Kids")) {
                        val items = playableFiltered
                            .filter { it.isSeries && it.isKidsFriendly }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_horror_movies" -> {
                    if (!isKidsProfile && selectedCategory != HomeCategoryFilter.SERIES && selectedCategory != HomeCategoryFilter.KIDS && isGenreEnabled("Horror")) {
                        val items = playableFiltered
                            .filter { !it.isSeries && hasToken(it, "Horror") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_action_adventure" -> {
                    if (isGenreEnabled("Action", "Adventure")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Action") || hasToken(it, "Adventure") }
                            .take(cap)
                        if (items.size >= 2) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_anime_favorites" -> {
                    if (isGenreEnabled("Anime")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Anime") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_anime_comedies" -> {
                    if (isGenreEnabled("Anime", "Comedy")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Anime") && hasToken(it, "Comedy") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_comedy_movies" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES && isGenreEnabled("Comedy")) {
                        val items = playableFiltered
                            .filter { !it.isSeries && hasToken(it, "Comedy") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_scifi_adventures" -> {
                    if (isGenreEnabled("Sci-Fi")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Sci-Fi") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_mystery_suspense" -> {
                    if (isGenreEnabled("Mystery", "Thriller")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Mystery") || hasToken(it, "Suspense") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_family_movie_night" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES && isGenreEnabled("Animation", "Adventure", "Comedy", "Fantasy")) {
                        val items = playableFiltered
                            .filter { !it.isSeries && it.isKidsFriendly }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_animated_favorites" -> {
                    if (isGenreEnabled("Animation")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Animation") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_thrillers" -> {
                    if (isGenreEnabled("Thriller")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Thriller") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_romantic_comedies" -> {
                    if (isGenreEnabled("Romance", "Comedy")) {
                        val items = playableFiltered
                            .filter { (hasToken(it, "Romance") || hasToken(it, "Rom-Com")) && hasToken(it, "Comedy") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_documentaries" -> {
                    if (isGenreEnabled("Documentary")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Documentary") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_fantasy_worlds" -> {
                    if (isGenreEnabled("Fantasy")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Fantasy") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_superhero" -> {
                    if (isGenreEnabled("Action", "Sci-Fi", "Anime")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Superhero") || hasToken(it, "Comic") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_crime_mystery" -> {
                    if (isGenreEnabled("Mystery", "Thriller", "Drama")) {
                        val items = playableFiltered
                            .filter { hasToken(it, "Crime") || hasToken(it, "Detective") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_adventure_movies" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES && isGenreEnabled("Adventure")) {
                        val items = playableFiltered
                            .filter { !it.isSeries && hasToken(it, "Adventure") }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }

                "genre_feel_good" -> {
                    if (selectedCategory != HomeCategoryFilter.SERIES && isGenreEnabled("Comedy", "Animation", "Adventure")) {
                        val items = playableFiltered
                            .filter {
                                !it.isSeries && (hasToken(it, "Feel-Good") || hasToken(it, "Comedy")) &&
                                    !hasToken(it, "Horror") && !hasToken(it, "Thriller")
                            }
                            .take(cap)
                        if (items.isNotEmpty()) {
                            rows += HomeFeedRow(id = rule.rowKey, rowKey = rule.rowKey, title = resolvedTitle, items = items)
                        }
                    }
                }
            }
        }

        // Custom Admin Genres not directly covered by built-in single-genre shelves
        val builtinGenresLower = setOf(
            "horror", "action", "adventure", "anime", "comedy", "sci-fi",
            "mystery", "animation", "thriller", "romance", "documentary", "fantasy"
        )
        genres.filter { it.isActive }.forEach { genre ->
            val normalized = genre.name.trim().lowercase()
            if (normalized.isNotBlank() && normalized !in builtinGenresLower && isGenreEnabled(genre.name)) {
                val matching = playableFiltered
                    .filter { it.genres.any { g -> g.equals(genre.name, ignoreCase = true) } }
                    .take(homepageSettings.maxItemsPerRow)
                if (matching.isNotEmpty()) {
                    rows += HomeFeedRow(
                        id = "custom_genre_${genre.id.ifBlank { normalized }}",
                        rowKey = "custom_genre_${genre.id.ifBlank { normalized }}",
                        title = "${genre.name} Picks",
                        items = matching
                    )
                }
            }
        }

        return rows.distinctBy { it.id }
    }

    /**
     * Strict metadata-based similarity calculation:
     * Only includes candidates that share at least one genre, tag, keyword, director, or cast member
     * with the seed title so unrelated titles never leak into recommendation shelves.
     */
    fun computeSimilarForSeed(
        seed: CatalogItem,
        pool: List<CatalogItem>,
        dislikedIds: Set<String>,
        isKids: Boolean,
        prioritizeNovelOverIds: Set<String> = emptySet()
    ): List<CatalogItem> {
        val seedGenres = seed.genres.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val seedTags = seed.tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val seedKeywords = seed.keywords.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val seedCast = seed.cast.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val seedDirector = seed.director.trim().lowercase().takeIf { it.isNotBlank() && it != "notflix studios" }

        return pool
            .asSequence()
            .filter { candidate ->
                candidate.id != seed.id &&
                    candidate.id !in dislikedIds &&
                    candidate.isPlayableNow &&
                    (!isKids || candidate.isKidsFriendly)
            }
            .mapNotNull { candidate ->
                val genreOverlap = candidate.genres.count { it.trim().lowercase() in seedGenres }
                val tagOverlap = candidate.tags.count { it.trim().lowercase() in seedTags }
                val keywordOverlap = candidate.keywords.count { it.trim().lowercase() in seedKeywords }
                val crossMatch = candidate.tags.count { it.trim().lowercase() in seedGenres } +
                    candidate.genres.count { it.trim().lowercase() in seedTags }
                val castOverlap = candidate.cast.count { it.trim().lowercase() in seedCast }
                val sameDirector = if (seedDirector != null && candidate.director.trim().lowercase() == seedDirector) 1 else 0

                val metadataScore =
                    genreOverlap * 120 +
                        tagOverlap * 95 +
                        crossMatch * 80 +
                        keywordOverlap * 70 +
                        sameDirector * 100 +
                        castOverlap * 60

                if (metadataScore < MIN_SIMILARITY_SCORE) {
                    null
                } else {
                    val sameTypeBonus = if (candidate.mediaType == seed.mediaType) 15 else 0
                    val diversityPenalty = if (candidate.id in prioritizeNovelOverIds) -45 else 0
                    val totalScore = metadataScore + sameTypeBonus + diversityPenalty + (candidate.trendingScore / 80)
                    candidate to totalScore
                }
            }
            .sortedByDescending { (_, score) -> score }
            .map { (candidate, _) -> candidate }
            .distinctBy { it.id }
            .take(10)
            .toList()
    }

    private fun scoreAffinity(item: CatalogItem, affinityWeights: Map<String, Int>): Int {
        val tokens = (item.genres + item.tags + item.keywords)
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
        return tokens.sumOf { affinityWeights[it] ?: 0 }
    }

    private fun hasToken(item: CatalogItem, token: String): Boolean =
        item.genres.any { it.equals(token, ignoreCase = true) } ||
            item.tags.any { it.equals(token, ignoreCase = true) } ||
            item.keywords.any { it.equals(token, ignoreCase = true) }

    private fun matchesHomeCategory(item: CatalogItem, category: HomeCategoryFilter): Boolean =
        when (category) {
            HomeCategoryFilter.ALL -> true
            HomeCategoryFilter.MOVIES -> item.mediaType == "movie"
            HomeCategoryFilter.SERIES -> item.mediaType == "series"
            HomeCategoryFilter.NEW_POPULAR -> item.releaseYear >= 2025 || item.trendingScore >= 900
            HomeCategoryFilter.KIDS -> item.isKidsFriendly
        }

    private fun formatRowTitle(rawTitle: String, profileName: String): String {
        return rawTitle
            .replace("{profile}", profileName)
            .replace("[Profile Name]", profileName)
    }
}
