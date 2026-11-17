package com.example.ui.state

import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.PlaybackProgress

data class NextEpisodeEntry(
    val catalogItem: CatalogItem,
    val nextEpisode: Episode,
    val lastWatchedAtSeconds: Long
)

data class ProfileWatchSummary(
    val continueWatching: List<ContinueWatchingEntry>,
    val nextEpisodes: List<NextEpisodeEntry>,
    val activeSeriesInProgress: List<CatalogItem>,
    val finishedItems: List<CatalogItem>,
    val finishedSeries: List<CatalogItem>
)

object CompletionTracker {
    const val COMPLETION_THRESHOLD_FRACTION = 0.90f
    const val MIN_COMPLETION_DURATION_MS = 20_000L
    const val MIN_WATCHED_POSITION_MS = 10_000L

    /**
     * Marks playback completed at >= 90% of total duration while guarding against
     * accidental early completion when media duration hasn't loaded properly (< 20s).
     */
    fun isPlaybackCompleted(positionMs: Long, durationMs: Long): Boolean {
        if (durationMs < MIN_COMPLETION_DURATION_MS) return false
        if (positionMs < MIN_WATCHED_POSITION_MS) return false
        return positionMs >= (durationMs * COMPLETION_THRESHOLD_FRACTION).toLong()
    }

    fun isEntryCompleted(progress: PlaybackProgress): Boolean {
        if (progress.completed) return true
        return isPlaybackCompleted(progress.positionMs, progress.durationMs)
    }

    fun analyzeWatchHistory(
        catalog: List<CatalogItem>,
        progressList: List<PlaybackProgress>,
        episodesBySeriesId: Map<String, List<Episode>> = emptyMap()
    ): ProfileWatchSummary {
        if (catalog.isEmpty() || progressList.isEmpty()) {
            return ProfileWatchSummary(
                continueWatching = emptyList(),
                nextEpisodes = emptyList(),
                activeSeriesInProgress = emptyList(),
                finishedItems = emptyList(),
                finishedSeries = emptyList()
            )
        }

        val itemById = catalog.filter { it.isPlayableNow }.associateBy { it.id }
        val sortedProgress = progressList.sortedByDescending { it.updatedAt?.seconds ?: 0L }

        // Continue Watching: ONLY unfinished playable movies and unfinished episodes.
        // Completed episodes are excluded automatically.
        val unfinishedByContent = linkedMapOf<String, ContinueWatchingEntry>()
        sortedProgress.forEach { entry ->
            val item = itemById[entry.contentId] ?: return@forEach
            if (!isEntryCompleted(entry) && entry.positionMs > 0L) {
                unfinishedByContent.putIfAbsent(item.id, ContinueWatchingEntry(entry, item))
            }
        }

        val nextEpisodes = mutableListOf<NextEpisodeEntry>()
        val activeSeries = mutableListOf<CatalogItem>()
        val finishedWithTimestamp = mutableListOf<Pair<CatalogItem, Long>>()

        val progressByContent = sortedProgress.groupBy { it.contentId }

        progressByContent.forEach { (contentId, entries) ->
            val item = itemById[contentId] ?: return@forEach
            val latestTs = entries.maxOfOrNull { it.updatedAt?.seconds ?: 0L } ?: 0L

            if (!item.isSeries) {
                if (entries.any { isEntryCompleted(it) }) {
                    finishedWithTimestamp += item to latestTs
                }
            } else {
                val completedEntries = entries.filter { isEntryCompleted(it) }
                val completedEpIds = completedEntries.mapNotNull { it.episodeId }.toSet()
                val unfinishedEpEntry = entries.firstOrNull { !isEntryCompleted(it) && it.positionMs > 0L }

                val knownEpisodes = episodesBySeriesId[item.id]
                    .orEmpty()
                    .filter { it.isVisible }
                    .sortedWith(compareBy<Episode> { it.seasonNumber }.thenBy { it.episodeNumber })

                if (knownEpisodes.isNotEmpty()) {
                    val lastEp = knownEpisodes.last()
                    val allCompleted = knownEpisodes.all { it.id in completedEpIds }
                    val finaleCompleted = lastEp.id in completedEpIds && unfinishedEpEntry == null

                    if (allCompleted || finaleCompleted) {
                        finishedWithTimestamp += item to latestTs
                    } else {
                        if (unfinishedEpEntry != null) {
                            activeSeries += item
                        }
                        if (completedEpIds.isNotEmpty()) {
                            val highestCompleted = completedEntries
                                .sortedWith(
                                    compareByDescending<PlaybackProgress> { it.seasonNumber ?: 0 }
                                        .thenByDescending { it.episodeNumber ?: 0 }
                                )
                                .firstOrNull()
                            val lastSeason = highestCompleted?.seasonNumber ?: 0
                            val lastEpNum = highestCompleted?.episodeNumber ?: 0

                            val nextEp = knownEpisodes.firstOrNull { ep ->
                                ep.id !in completedEpIds &&
                                    (ep.seasonNumber > lastSeason ||
                                        (ep.seasonNumber == lastSeason && ep.episodeNumber > lastEpNum))
                            } ?: knownEpisodes.firstOrNull { ep -> ep.id !in completedEpIds }

                            if (nextEp != null) {
                                if (item !in activeSeries) activeSeries += item
                                nextEpisodes += NextEpisodeEntry(
                                    catalogItem = item,
                                    nextEpisode = nextEp,
                                    lastWatchedAtSeconds = latestTs
                                )
                            }
                        }
                    }
                } else {
                    // Fallback when episode list hasn't been loaded: use episode-agnostic completion markers
                    if (completedEntries.isNotEmpty() && unfinishedEpEntry == null) {
                        finishedWithTimestamp += item to latestTs
                    } else if (unfinishedEpEntry != null) {
                        activeSeries += item
                    }
                }
            }
        }

        val orderedFinished = finishedWithTimestamp
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.id }

        return ProfileWatchSummary(
            continueWatching = unfinishedByContent.values.toList(),
            nextEpisodes = nextEpisodes.sortedByDescending { it.lastWatchedAtSeconds },
            activeSeriesInProgress = activeSeries.distinctBy { it.id },
            finishedItems = orderedFinished,
            finishedSeries = orderedFinished.filter { it.isSeries }
        )
    }
}
