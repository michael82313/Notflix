' =============================================================================
' Notflix for Roku TV — Dynamic Personalized Homepage Row Builder
' Builds real catalog-driven rows per profile (never invents titles, never dumps
' the entire catalog into every row, enforces Kids safety, avoids completed items
' in Continue Watching).
' =============================================================================

function BuildNotflixHomeFeed(catalog as object, genres as object, profile as dynamic, watchlist as object, progressList as object, ratings as object) as object
    isKidsProfile = false
    profileName = "You"
    preferredGenres = {}
    if profile <> invalid
        isKidsProfile = SafeBool(profile.isKids, false)
        profileName = SafeStr(profile.name, "You")
        if profile.preferredGenres <> invalid
            for each g in profile.preferredGenres
                preferredGenres[LCase(g)] = 30
            end for
        end if
    end if

    ' 1. Filter playable catalog items (visible, not coming soon, kids-safe if kids profile)
    playable = []
    catalogById = {}
    for each item in catalog
        if SafeBool(item.isVisible, true) and not SafeBool(item.isComingSoon, false)
            if (not isKidsProfile) or SafeBool(item.isKidsFriendly, false)
                playable.Push(item)
                catalogById[item.id] = item
            end if
        end if
    end for

    if playable.Count() = 0
        return { heroItem: invalid, rows: [] }
    end if

    ' Build lookup maps for progress, watchlist, ratings
    dislikedIds = {}
    likedIds = {}
    for each r in ratings
        if r.liked < 0
            dislikedIds[r.contentId] = true
        else if r.liked > 0 or r.stars >= 4
            likedIds[r.contentId] = true
        end if
    end for

    ' Latest progress per contentId (sorted descending by updatedAtEpoch)
    sortedProgress = SortItemsByFieldDesc(progressList, "updatedAtEpoch")
    latestProgressByContent = {}
    unfinishedContentIds = []
    completedContentIds = []
    watchedContentIds = {}

    for each prog in sortedProgress
        cid = prog.contentId
        if cid <> "" and catalogById.DoesExist(cid)
            watchedContentIds[cid] = true
            if not latestProgressByContent.DoesExist(cid)
                latestProgressByContent[cid] = prog
                if prog.completed
                    completedContentIds.Push(cid)
                else if prog.positionMs >= 3000#
                    unfinishedContentIds.Push(cid)
                end if
            end if
        end if
    end for

    ' Compute genre affinity weights from profile preferences + watch/like history
    affinity = {}
    for each gk in preferredGenres
        affinity[gk] = preferredGenres[gk]
    end for
    for each item in playable
        if watchedContentIds.DoesExist(item.id) or likedIds.DoesExist(item.id)
            boost = 20
            if likedIds.DoesExist(item.id) then boost = 35
            for each g in item.genres
                gl = LCase(g)
                if affinity.DoesExist(gl)
                    affinity[gl] = affinity[gl] + boost
                else
                    affinity[gl] = boost
                end if
            end for
        end if
    end for

    rows = []
    maxPerRow = 12

    ' ROW 1: Continue Watching (only unfinished items with progress, NEVER completed!)
    cwItems = []
    for each cid in unfinishedContentIds
        if catalogById.DoesExist(cid) and cwItems.Count() < maxPerRow
            cwItems.Push(CloneWithProgress(catalogById[cid], latestProgressByContent[cid]))
        end if
    end for
    if cwItems.Count() > 0
        rows.Push({ title: "Continue Watching for " + profileName, rowKey: "continue_watching", items: cwItems })
    end if

    ' ROW 2: My List (real profile watchlist sorted by addedAt desc)
    sortedWatchlist = SortItemsByFieldDesc(watchlist, "addedAtEpoch")
    myListItems = []
    for each wl in sortedWatchlist
        if catalogById.DoesExist(wl.contentId) and myListItems.Count() < maxPerRow
            myListItems.Push(AttachProgressIfAny(catalogById[wl.contentId], latestProgressByContent))
        end if
    end for
    if myListItems.Count() > 0
        rows.Push({ title: "My List", rowKey: "my_list", items: myListItems })
    end if

    ' ROW 3: Popular on Notflix (sorted by popularityRank asc, trendingScore desc)
    popularSorted = SortByPopularity(playable)
    popularSlice = TakeUpTo(popularSorted, maxPerRow, latestProgressByContent)
    if popularSlice.Count() > 0
        rows.Push({ title: "Popular on Notflix", rowKey: "popular", items: popularSlice })
    end if

    ' ROW 4: Trending Now (sorted by trendingScore desc; only include if distinct order or >= 2 items)
    trendingSorted = SortItemsByFieldDesc(playable, "trendingScore")
    trendingFiltered = []
    for each tItem in trendingSorted
        if tItem.trendingScore >= 70 or trendingFiltered.Count() < 6
            trendingFiltered.Push(tItem)
        end if
    end for
    if trendingFiltered.Count() >= 2
        rows.Push({ title: "Trending Now", rowKey: "trending", items: TakeUpTo(trendingFiltered, maxPerRow, latestProgressByContent) })
    end if

    ' ROW 5: Recently Added (sorted by createdAtEpoch desc, then releaseYear desc)
    recentSorted = SortByRecentlyAdded(playable)
    if recentSorted.Count() >= 2
        rows.Push({ title: "Recently Added", rowKey: "recently_added", items: TakeUpTo(recentSorted, maxPerRow, latestProgressByContent) })
    end if

    ' ROW 6: Recommended Movies (personalized genre score, movies only)
    moviePool = []
    for each item in playable
        if item.mediaType = "movie" and not dislikedIds.DoesExist(item.id)
            moviePool.Push(item)
        end if
    end for
    if moviePool.Count() >= 2
        recMovies = SortByRecommendationScore(moviePool, affinity, watchedContentIds)
        rows.Push({ title: "Recommended Movies", rowKey: "rec_movies", items: TakeUpTo(recMovies, maxPerRow, latestProgressByContent) })
    end if

    ' ROW 7: Recommended TV Shows (personalized genre score, series only)
    seriesPool = []
    for each item in playable
        if item.mediaType = "series" and not dislikedIds.DoesExist(item.id)
            seriesPool.Push(item)
        end if
    end for
    if seriesPool.Count() >= 1
        recSeries = SortByRecommendationScore(seriesPool, affinity, watchedContentIds)
        rows.Push({ title: "Recommended TV Shows", rowKey: "rec_series", items: TakeUpTo(recSeries, maxPerRow, latestProgressByContent) })
    end if

    ' ROW 8: Because You Watched [Title] (up to 2 rows seeded from completed/watched titles)
    seedIds = []
    for each cid in completedContentIds
        if seedIds.Count() < 2 then seedIds.Push(cid)
    end for
    if seedIds.Count() < 2
        for each cid in unfinishedContentIds
            if seedIds.Count() < 2 then seedIds.Push(cid)
        end for
    end if
    for each seedId in seedIds
        seedItem = catalogById[seedId]
        similarItems = FindSimilarCatalogItems(seedItem, playable, dislikedIds)
        if similarItems.Count() >= 2
            rows.Push({
                title: "Because You Watched " + seedItem.title
                rowKey: "because_watched_" + seedId
                items: TakeUpTo(similarItems, maxPerRow, latestProgressByContent)
            })
        end if
    end for

    ' ROW 9: Watch Again (completed titles)
    if completedContentIds.Count() >= 1
        againItems = []
        for each cid in completedContentIds
            if catalogById.DoesExist(cid) and againItems.Count() < maxPerRow
                againItems.Push(AttachProgressIfAny(catalogById[cid], latestProgressByContent))
            end if
        end for
        if againItems.Count() >= 1
            rows.Push({ title: "Watch Again", rowKey: "watch_again", items: againItems })
        end if
    end if

    ' ROW 10: Staff Picks & Award Winners (only if tagged items exist)
    staffPicks = []
    for each item in playable
        if item.isStaffPick or item.isAwardWinning
            staffPicks.Push(item)
        end if
    end for
    if staffPicks.Count() >= 2
        rows.Push({ title: "Notflix Staff Picks", rowKey: "staff_picks", items: TakeUpTo(staffPicks, maxPerRow, latestProgressByContent) })
    end if

    ' ROW 11+: Genre Collections (only for genres that actually have >= 2 real matching titles!)
    orderedGenreNames = CollectOrderedGenreNames(genres, playable)
    genreRowsAdded = 0
    for each gName in orderedGenreNames
        if genreRowsAdded >= 8 then exit for
        matches = []
        targetLower = LCase(gName)
        for each item in playable
            if ItemHasGenre(item, targetLower)
                matches.Push(item)
            end if
        end for
        if matches.Count() >= 2
            sortedMatches = SortItemsByFieldDesc(matches, "trendingScore")
            rows.Push({
                title: gName
                rowKey: "genre_" + targetLower
                items: TakeUpTo(sortedMatches, maxPerRow, latestProgressByContent)
            })
            genreRowsAdded = genreRowsAdded + 1
        end if
    end for

    ' Select Featured Hero Item
    heroItem = invalid
    for each item in trendingSorted
        if item.isFeatured
            heroItem = AttachProgressIfAny(item, latestProgressByContent)
            exit for
        end if
    end for
    if heroItem = invalid and trendingSorted.Count() > 0
        heroItem = AttachProgressIfAny(trendingSorted[0], latestProgressByContent)
    end if

    return {
        heroItem: heroItem
        rows: rows
    }
end function

function CloneWithProgress(item as object, prog as dynamic) as object
    copyObj = {}
    for each k in item
        copyObj[k] = item[k]
    end for
    if prog <> invalid
        copyObj.progressFraction = prog.progressFraction
        copyObj.resumePositionSec = Int(prog.positionMs / 1000#)
        copyObj.progressCompleted = prog.completed
        copyObj.resumeEpisodeId = prog.episodeId
        copyObj.resumeSeasonNumber = prog.seasonNumber
        copyObj.resumeEpisodeNumber = prog.episodeNumber
    else
        copyObj.progressFraction = 0.0
        copyObj.resumePositionSec = 0
        copyObj.progressCompleted = false
    end if
    return copyObj
end function

function AttachProgressIfAny(item as object, progressMap as object) as object
    if progressMap <> invalid and progressMap.DoesExist(item.id)
        return CloneWithProgress(item, progressMap[item.id])
    end if
    return CloneWithProgress(item, invalid)
end function

function TakeUpTo(items as object, limitCount as integer, progressMap as object) as object
    outArr = []
    for each it in items
        if outArr.Count() >= limitCount then exit for
        outArr.Push(AttachProgressIfAny(it, progressMap))
    end for
    return outArr
end function

function ItemHasGenre(item as object, lowerGenre as string) as boolean
    if item = invalid or item.genres = invalid then return false
    for each g in item.genres
        if LCase(g) = lowerGenre then return true
    end for
    return false
end function

function FindSimilarCatalogItems(seed as object, pool as object, dislikedIds as object) as object
    if seed = invalid then return []
    seedGenres = {}
    for each g in seed.genres
        seedGenres[LCase(g)] = true
    end for
    scored = []
    for each cand in pool
        if cand.id <> seed.id and not dislikedIds.DoesExist(cand.id)
            overlap = 0
            for each cg in cand.genres
                if seedGenres.DoesExist(LCase(cg)) then overlap = overlap + 1
            end for
            if overlap > 0
                score = (overlap * 50) + Int(cand.trendingScore / 10)
                if cand.mediaType = seed.mediaType then score = score + 10
                scored.Push({ item: cand, score: score })
            end if
        end if
    end for
    scored = SortScoredDesc(scored)
    outArr = []
    for each entry in scored
        outArr.Push(entry.item)
    end for
    return outArr
end function

function CollectOrderedGenreNames(genres as object, playable as object) as object
    names = []
    seen = {}
    sortedGenres = SortItemsByFieldAsc(genres, "displayOrder")
    for each g in sortedGenres
        if g.isActive and g.name <> ""
            key = LCase(g.name)
            if not seen.DoesExist(key)
                seen[key] = true
                names.Push(g.name)
            end if
        end if
    end for
    for each item in playable
        for each gn in item.genres
            key = LCase(gn)
            if gn <> "" and not seen.DoesExist(key)
                seen[key] = true
                names.Push(gn)
            end if
        end for
    end for
    return names
end function

function SortByPopularity(items as object) as object
    scored = []
    for each it in items
        rankScore = 10000 - it.popularityRank
        if it.popularityRank <= 0 then rankScore = 0
        total = (rankScore * 10) + it.trendingScore
        scored.Push({ item: it, score: total })
    end for
    scored = SortScoredDesc(scored)
    outArr = []
    for each e in scored
        outArr.Push(e.item)
    end for
    return outArr
end function

function SortByRecentlyAdded(items as object) as object
    scored = []
    for each it in items
        score = it.createdAtEpoch
        if score <= 0 then score = it.releaseYear * 1000
        scored.Push({ item: it, score: score })
    end for
    scored = SortScoredDesc(scored)
    outArr = []
    for each e in scored
        outArr.Push(e.item)
    end for
    return outArr
end function

function SortByRecommendationScore(items as object, affinity as object, watchedIds as object) as object
    scored = []
    for each it in items
        gScore = 0
        for each g in it.genres
            gl = LCase(g)
            if affinity.DoesExist(gl) then gScore = gScore + affinity[gl]
        end for
        unwatchedBonus = 15
        if watchedIds.DoesExist(it.id) then unwatchedBonus = 0
        total = gScore + unwatchedBonus + Int(it.trendingScore / 20)
        scored.Push({ item: it, score: total })
    end for
    scored = SortScoredDesc(scored)
    outArr = []
    for each e in scored
        outArr.Push(e.item)
    end for
    return outArr
end function

function SortItemsByFieldDesc(items as object, fieldName as string) as object
    scored = []
    for each it in items
        scored.Push({ item: it, score: SafeNum(it[fieldName], 0#) })
    end for
    scored = SortScoredDesc(scored)
    outArr = []
    for each e in scored
        outArr.Push(e.item)
    end for
    return outArr
end function

function SortItemsByFieldAsc(items as object, fieldName as string) as object
    scored = []
    for each it in items
        scored.Push({ item: it, score: -SafeNum(it[fieldName], 0#) })
    end for
    scored = SortScoredDesc(scored)
    outArr = []
    for each e in scored
        outArr.Push(e.item)
    end for
    return outArr
end function

function SortScoredDesc(listObj as object) as object
    count = listObj.Count()
    if count <= 1 then return listObj
    ' Simple in-place insertion sort (fast & low memory for Roku catalog sizes <= 300)
    for i = 1 to count - 1
        cur = listObj[i]
        j = i - 1
        while j >= 0 and listObj[j].score < cur.score
            listObj[j + 1] = listObj[j]
            j = j - 1
        end while
        listObj[j + 1] = cur
    end for
    return listObj
end function
