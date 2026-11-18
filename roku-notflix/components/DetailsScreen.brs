sub init()
    m.backdropImg = m.top.findNode("backdropImg")
    m.posterImg = m.top.findNode("posterImg")
    m.sourceBadge = m.top.findNode("sourceBadge")
    m.titleLabel = m.top.findNode("titleLabel")
    m.metaLabel = m.top.findNode("metaLabel")
    m.synopsisLabel = m.top.findNode("synopsisLabel")
    m.compatNotice = m.top.findNode("compatNotice")

    m.btnPlayBorder = m.top.findNode("btnPlayBorder")
    m.btnPlayBg = m.top.findNode("btnPlayBg")
    m.btnPlayLabel = m.top.findNode("btnPlayLabel")

    m.btnStartOverGroup = m.top.findNode("btnStartOverGroup")
    m.btnStartOverBorder = m.top.findNode("btnStartOverBorder")
    m.btnStartOverBg = m.top.findNode("btnStartOverBg")

    m.btnListGroup = m.top.findNode("btnListGroup")
    m.btnListBorder = m.top.findNode("btnListBorder")
    m.btnListBg = m.top.findNode("btnListBg")
    m.btnListLabel = m.top.findNode("btnListLabel")

    m.btnBackGroup = m.top.findNode("btnBackGroup")
    m.btnBackBorder = m.top.findNode("btnBackBorder")
    m.btnBackBg = m.top.findNode("btnBackBg")

    m.seriesSection = m.top.findNode("seriesSection")
    m.seasonBarBorder = m.top.findNode("seasonBarBorder")
    m.seasonBarBg = m.top.findNode("seasonBarBg")
    m.seasonBarLabel = m.top.findNode("seasonBarLabel")
    m.seasonHintLabel = m.top.findNode("seasonHintLabel")
    m.seriesStatusLabel = m.top.findNode("seriesStatusLabel")
    m.episodeList = m.top.findNode("episodeList")

    m.movieInfoSection = m.top.findNode("movieInfoSection")
    m.movieFooterBody = m.top.findNode("movieFooterBody")

    m.currentItem = invalid
    m.seasons = []
    m.episodesBySeason = {}
    m.selectedSeasonIdx = 0
    m.currentEpisodes = []
    m.progressByKey = {}

    ' Focus state: "actions", "seasons", "episodes"
    m.focusZone = "actions"
    m.actionIds = ["play", "list", "back"]
    m.actionIndex = 0
    m.primaryResumeSec = 0
    m.primaryTargetEpisode = invalid
    m.primaryTargetSeasonNum = 0

    m.episodeList.observeField("itemSelected", "onEpisodeSelected")
end sub

sub onItemChanged()
    item = m.top.item
    m.currentItem = item
    m.seasons = []
    m.episodesBySeason = {}
    m.selectedSeasonIdx = 0
    m.currentEpisodes = []
    m.focusZone = "actions"
    m.actionIndex = 0

    if item = invalid then return

    m.backdropImg.uri = SafeStr(item.resolvedBackdropUri, "pkg:/images/hero_nebula.jpg")
    m.posterImg.uri = SafeStr(item.resolvedPosterUri, "pkg:/images/poster_fallback.jpg")
    m.titleLabel.text = SafeStr(item.title, "Untitled")
    m.synopsisLabel.text = SafeStr(item.synopsis, "No synopsis available.")

    metaParts = []
    if SafeInt(item.releaseYear, 0) > 0 then metaParts.Push(item.releaseYear.ToStr())
    if SafeStr(item.ageRating) <> "" then metaParts.Push(item.ageRating)
    if item.mediaType = "series"
        metaParts.Push("TV Series")
    else if SafeInt(item.runtimeMinutes, 0) > 0
        metaParts.Push(item.runtimeMinutes.ToStr() + " min")
    end if
    if item.genres <> invalid and item.genres.Count() > 0
        gStr = ""
        for i = 0 to item.genres.Count() - 1
            if i > 0 then gStr = gStr + ", "
            gStr = gStr + item.genres[i]
        end for
        metaParts.Push(gStr)
    end if

    metaOut = ""
    for i = 0 to metaParts.Count() - 1
        if i > 0 then metaOut = metaOut + "   •   "
        metaOut = metaOut + metaParts[i]
    end for
    m.metaLabel.text = metaOut

    if item.mediaType = "series"
        m.seriesSection.visible = true
        m.movieInfoSection.visible = false
        m.seriesStatusLabel.visible = true
        m.seriesStatusLabel.text = "Loading seasons and episodes..."
        m.episodeList.content = invalid
        m.sourceBadge.text = "TV SERIES  •  Select an episode to watch"
        m.sourceBadge.color = "0x38BDF8FF"
        m.compatNotice.text = ""
    else
        m.seriesSection.visible = false
        m.movieInfoSection.visible = true
        src = ResolveRokuPlayableSource(item)
        if src.playable
            m.sourceBadge.text = "MOVIE  •  Roku Ready (" + src.sourceLabel + ")"
            m.sourceBadge.color = "0x22C55EFF"
            m.compatNotice.text = ""
        else
            m.sourceBadge.text = "MOVIE  •  " + src.sourceLabel
            m.sourceBadge.color = "0xF59E0BFF"
            m.compatNotice.text = src.errorMessage
        end if

        infoLine = "Stream Format: " + src.sourceLabel
        if SafeBool(item.isKidsFriendly, false)
            infoLine = infoLine + "   •   Kids Friendly"
        end if
        if SafeStr(item.language) <> ""
            infoLine = infoLine + "   •   Language: " + UCase(item.language)
        end if
        infoLine = infoLine + Chr(10) + Chr(10) + "Notflix is Always Free and Ad-Free. Your playback progress and My List sync automatically with your Notflix account across Android and Roku TV."
        m.movieFooterBody.text = infoLine
    end if

    rebuildProgressIndex()
    refreshPrimaryActionState()
    updateWatchlistButtonLabel()
    updateFocusVisuals()
end sub

sub onWatchlistChanged()
    updateWatchlistButtonLabel()
end sub

sub onProgressListChanged()
    rebuildProgressIndex()
    refreshPrimaryActionState()
    if m.currentItem <> invalid and m.currentItem.mediaType = "series" and m.seasons.Count() > 0
        renderSelectedSeasonEpisodes()
    end if
end sub

sub onSeriesDataChanged()
    sd = m.top.seriesData
    if sd = invalid or m.currentItem = invalid then return
    if SafeStr(sd.contentId) <> "" and SafeStr(sd.contentId) <> m.currentItem.id then return

    if SafeBool(sd.loading, false)
        m.seriesStatusLabel.visible = true
        m.seriesStatusLabel.text = "Loading seasons and episodes..."
        m.episodeList.content = invalid
        return
    end if

    if SafeStr(sd.errorMessage) <> ""
        m.seriesStatusLabel.visible = true
        m.seriesStatusLabel.text = sd.errorMessage
        m.episodeList.content = invalid
        return
    end if

    m.seasons = []
    if sd.seasons <> invalid then m.seasons = sd.seasons
    m.episodesBySeason = {}
    if sd.episodesBySeason <> invalid then m.episodesBySeason = sd.episodesBySeason

    if m.seasons.Count() = 0
        m.seriesStatusLabel.visible = true
        m.seriesStatusLabel.text = "No visible seasons or episodes are published for this series yet."
        m.seasonBarLabel.text = "No Seasons"
        m.episodeList.content = invalid
        refreshPrimaryActionState()
        return
    end if

    ' Auto-select the season containing the latest unfinished episode, if any
    m.selectedSeasonIdx = 0
    bestEp = findBestResumeEpisode()
    if bestEp <> invalid
        for sIdx = 0 to m.seasons.Count() - 1
            if m.seasons[sIdx].seasonNumber = bestEp.seasonNumber
                m.selectedSeasonIdx = sIdx
                exit for
            end if
        end for
    end if

    renderSelectedSeasonEpisodes()
    refreshPrimaryActionState()
    updateFocusVisuals()
end sub

sub onFocusRequested()
    m.focusZone = "actions"
    m.actionIndex = 0
    m.top.setFocus(true)
    updateFocusVisuals()
end sub

sub rebuildProgressIndex()
    m.progressByKey = {}
    pList = m.top.progressList
    if pList = invalid then return
    for each prog in pList
        if prog <> invalid and SafeStr(prog.contentId) <> ""
            key = prog.contentId
            if SafeStr(prog.episodeId) <> ""
                key = prog.contentId + "_" + prog.episodeId
            end if
            m.progressByKey[key] = prog
        end if
    end for
end sub

function findBestResumeEpisode() as dynamic
    if m.currentItem = invalid or m.seasons.Count() = 0 then return invalid

    latestProg = invalid
    latestEp = invalid
    latestSeasonNum = 0
    firstEp = invalid
    firstSeasonNum = 0

    for each season in m.seasons
        eps = m.episodesBySeason[season.id]
        if eps <> invalid
            for each ep in eps
                if firstEp = invalid
                    firstEp = ep
                    firstSeasonNum = season.seasonNumber
                end if
                key = m.currentItem.id + "_" + ep.id
                if m.progressByKey.DoesExist(key)
                    p = m.progressByKey[key]
                    if latestProg = invalid or p.updatedAtEpoch > latestProg.updatedAtEpoch
                        latestProg = p
                        latestEp = ep
                        latestSeasonNum = season.seasonNumber
                    end if
                end if
            end for
        end if
    end for

    if latestEp <> invalid and latestProg <> invalid
        if latestProg.completed
            ' Advance to the next episode after the last completed one if available
            nextInfo = findNextEpisodeAfter(latestEp.id)
            if nextInfo <> invalid
                return { episode: nextInfo.episode, seasonNumber: nextInfo.seasonNumber, resumeSec: 0 }
            end if
            return { episode: latestEp, seasonNumber: latestSeasonNum, resumeSec: 0 }
        else if latestProg.positionMs >= 10000#
            return { episode: latestEp, seasonNumber: latestSeasonNum, resumeSec: Int(latestProg.positionMs / 1000#) }
        end if
    end if

    if firstEp <> invalid
        return { episode: firstEp, seasonNumber: firstSeasonNum, resumeSec: 0 }
    end if
    return invalid
end function

sub refreshPrimaryActionState()
    if m.currentItem = invalid then return

    m.primaryResumeSec = 0
    m.primaryTargetEpisode = invalid
    m.primaryTargetSeasonNum = 0

    if m.currentItem.mediaType = "series"
        target = findBestResumeEpisode()
        if target <> invalid
            m.primaryTargetEpisode = target.episode
            m.primaryTargetSeasonNum = target.seasonNumber
            m.primaryResumeSec = target.resumeSec
            epCode = "S" + target.seasonNumber.ToStr() + " E" + target.episode.episodeNumber.ToStr()
            if m.primaryResumeSec >= 10
                m.btnPlayLabel.text = "Resume " + epCode + " (" + FormatTimeSeconds(m.primaryResumeSec) + ")"
            else
                m.btnPlayLabel.text = "Play " + epCode
            end if
            epSrc = ResolveRokuPlayableSource(target.episode)
            if not epSrc.playable
                m.compatNotice.text = epCode + ": " + epSrc.errorMessage
            else
                m.compatNotice.text = ""
            end if
        else
            m.btnPlayLabel.text = "Play Episode"
        end if
    else
        key = m.currentItem.id
        if m.progressByKey.DoesExist(key)
            p = m.progressByKey[key]
            if not p.completed and p.positionMs >= 10000#
                m.primaryResumeSec = Int(p.positionMs / 1000#)
            end if
        end if
        if m.primaryResumeSec >= 10
            m.btnPlayLabel.text = "Resume (" + FormatTimeSeconds(m.primaryResumeSec) + ")"
        else
            m.btnPlayLabel.text = "Play Movie"
        end if
    end if

    ' Layout action buttons dynamically depending on whether Start Over is visible
    if m.primaryResumeSec >= 10
        m.btnStartOverGroup.visible = true
        m.btnListGroup.translation = [604, 0]
        m.btnBackGroup.translation = [880, 0]
        m.actionIds = ["play", "start_over", "list", "back"]
    else
        m.btnStartOverGroup.visible = false
        m.btnListGroup.translation = [348, 0]
        m.btnBackGroup.translation = [624, 0]
        m.actionIds = ["play", "list", "back"]
    end if

    if m.actionIndex >= m.actionIds.Count()
        m.actionIndex = 0
    end if
    updateFocusVisuals()
end sub

sub updateWatchlistButtonLabel()
    if m.top.inWatchlist
        m.btnListLabel.text = "✓  In My List"
    else
        m.btnListLabel.text = "+  My List"
    end if
end sub

sub renderSelectedSeasonEpisodes()
    if m.seasons.Count() = 0 then return
    if m.selectedSeasonIdx < 0 then m.selectedSeasonIdx = 0
    if m.selectedSeasonIdx >= m.seasons.Count() then m.selectedSeasonIdx = m.seasons.Count() - 1

    season = m.seasons[m.selectedSeasonIdx]
    sTitle = SafeStr(season.title)
    if sTitle = "" then sTitle = "Season " + season.seasonNumber.ToStr()
    if m.seasons.Count() > 1
        m.seasonBarLabel.text = "◀  " + sTitle + " (" + (m.selectedSeasonIdx + 1).ToStr() + "/" + m.seasons.Count().ToStr() + ")  ▶"
        m.seasonHintLabel.text = "Press Left / Right to switch seasons  •  Down for episodes"
    else
        m.seasonBarLabel.text = sTitle
        m.seasonHintLabel.text = "Press Down to browse episodes"
    end if

    eps = m.episodesBySeason[season.id]
    if eps = invalid then eps = []
    m.currentEpisodes = eps

    if eps.Count() = 0
        m.seriesStatusLabel.visible = true
        m.seriesStatusLabel.text = "No episodes published in " + sTitle + " yet."
        m.episodeList.content = invalid
        return
    end if

    m.seriesStatusLabel.visible = false
    rootNode = CreateObject("roSGNode", "ContentNode")
    for each ep in eps
        epNode = rootNode.CreateChild("ContentNode")
        epNode.title = ep.episodeNumber.ToStr() + ".  " + ep.title
        epNode.description = ep.synopsis
        epNode.HDPosterUrl = ep.resolvedThumbUri
        epNode.SDPosterUrl = ep.resolvedThumbUri

        rtLabel = ""
        if ep.runtimeMinutes > 0 then rtLabel = ep.runtimeMinutes.ToStr() + " min"

        src = ResolveRokuPlayableSource(ep)
        statusText = "Roku Ready  •  " + src.sourceLabel
        statusCol = "0x22C55EFF"
        if not src.playable
            statusText = src.sourceLabel + "  •  Not playable on Roku TV"
            statusCol = "0xF59E0BFF"
        end if

        progFrac = 0.0
        key = m.currentItem.id + "_" + ep.id
        if m.progressByKey.DoesExist(key)
            p = m.progressByKey[key]
            if p.completed
                statusText = "Watched  •  " + statusText
                progFrac = 1.0
            else if p.positionMs >= 5000#
                progFrac = p.progressFraction
                statusText = "Resume at " + FormatTimeSeconds(Int(p.positionMs / 1000#)) + "  •  " + src.sourceLabel
            end if
        end if

        epNode.AddFields({
            episodeId: ep.id
            runtimeLabel: rtLabel
            statusLabel: statusText
            statusColor: statusCol
            progressFraction: progFrac
        })
    end for

    m.episodeList.content = rootNode
end sub

function findNextEpisodeAfter(episodeId as string) as dynamic
    if m.seasons.Count() = 0 or episodeId = "" then return invalid
    foundCurrent = false
    for each season in m.seasons
        eps = m.episodesBySeason[season.id]
        if eps <> invalid
            for each ep in eps
                if foundCurrent
                    return { episode: ep, seasonNumber: season.seasonNumber }
                end if
                if ep.id = episodeId
                    foundCurrent = true
                end if
            end for
        end if
    end for
    return invalid
end function

sub launchPlayback(targetEpisode as dynamic, seasonNum as integer, startSec as integer)
    if m.currentItem = invalid then return

    mediaTarget = m.currentItem
    epNum = 0
    nextEpObj = invalid
    nextSeasonNum = 0

    if targetEpisode <> invalid
        mediaTarget = targetEpisode
        epNum = targetEpisode.episodeNumber
        nextInfo = findNextEpisodeAfter(targetEpisode.id)
        if nextInfo <> invalid
            nextEpObj = nextInfo.episode
            nextSeasonNum = nextInfo.seasonNumber
        end if
    end if

    src = ResolveRokuPlayableSource(mediaTarget)
    m.top.playRequest = {
        item: m.currentItem
        episode: targetEpisode
        seasonNumber: seasonNum
        episodeNumber: epNum
        resumeSec: startSec
        sourceInfo: src
        nextEpisode: nextEpObj
        nextSeasonNumber: nextSeasonNum
    }
end sub

sub onEpisodeSelected()
    idx = m.episodeList.itemSelected
    if idx < 0 or idx >= m.currentEpisodes.Count() then return
    ep = m.currentEpisodes[idx]
    season = m.seasons[m.selectedSeasonIdx]

    resumeSec = 0
    key = m.currentItem.id + "_" + ep.id
    if m.progressByKey.DoesExist(key)
        p = m.progressByKey[key]
        if not p.completed and p.positionMs >= 10000#
            resumeSec = Int(p.positionMs / 1000#)
        end if
    end if

    launchPlayback(ep, season.seasonNumber, resumeSec)
end sub

sub updateFocusVisuals()
    ' Reset action buttons
    m.btnPlayBorder.color = "0x2E2E3AFF"
    m.btnPlayBg.color = "0xA80710FF"
    m.btnStartOverBorder.color = "0x2E2E3AFF"
    m.btnStartOverBg.color = "0x1D1D28FF"
    m.btnListBorder.color = "0x2E2E3AFF"
    m.btnListBg.color = "0x1D1D28FF"
    m.btnBackBorder.color = "0x2E2E3AFF"
    m.btnBackBg.color = "0x1D1D28FF"

    ' Reset season bar
    m.seasonBarBorder.color = "0x2E2E3AFF"
    m.seasonBarBg.color = "0x181822FF"

    if m.focusZone = "actions"
        curId = m.actionIds[m.actionIndex]
        if curId = "play"
            m.btnPlayBorder.color = "0xFFFFFFFF"
            m.btnPlayBg.color = "0xE50914FF"
        else if curId = "start_over"
            m.btnStartOverBorder.color = "0xE50914FF"
            m.btnStartOverBg.color = "0x2A2A3AFF"
        else if curId = "list"
            m.btnListBorder.color = "0xE50914FF"
            m.btnListBg.color = "0x2A2A3AFF"
        else if curId = "back"
            m.btnBackBorder.color = "0xE50914FF"
            m.btnBackBg.color = "0x2A2A3AFF"
        end if
    else if m.focusZone = "seasons"
        m.seasonBarBorder.color = "0xE50914FF"
        m.seasonBarBg.color = "0x272736FF"
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    if key = "back"
        m.top.backRequested = true
        return true
    end if

    if m.focusZone = "actions"
        if key = "left"
            if m.actionIndex > 0
                m.actionIndex = m.actionIndex - 1
                updateFocusVisuals()
            end if
            return true
        else if key = "right"
            if m.actionIndex < m.actionIds.Count() - 1
                m.actionIndex = m.actionIndex + 1
                updateFocusVisuals()
            end if
            return true
        else if key = "down"
            if m.currentItem <> invalid and m.currentItem.mediaType = "series" and m.seasons.Count() > 0
                if m.seasons.Count() > 1
                    m.focusZone = "seasons"
                    m.top.setFocus(true)
                else if m.currentEpisodes.Count() > 0
                    m.focusZone = "episodes"
                    m.episodeList.setFocus(true)
                end if
                updateFocusVisuals()
                return true
            end if
        else if key = "OK" or key = "play"
            curId = m.actionIds[m.actionIndex]
            if curId = "play"
                if m.currentItem.mediaType = "series"
                    if m.primaryTargetEpisode <> invalid
                        launchPlayback(m.primaryTargetEpisode, m.primaryTargetSeasonNum, m.primaryResumeSec)
                    end if
                else
                    launchPlayback(invalid, 0, m.primaryResumeSec)
                end if
            else if curId = "start_over"
                if m.currentItem.mediaType = "series"
                    if m.primaryTargetEpisode <> invalid
                        launchPlayback(m.primaryTargetEpisode, m.primaryTargetSeasonNum, 0)
                    end if
                else
                    launchPlayback(invalid, 0, 0)
                end if
            else if curId = "list"
                m.top.toggleWatchlistRequest = {
                    item: m.currentItem
                    addToList: (not m.top.inWatchlist)
                }
            else if curId = "back"
                m.top.backRequested = true
            end if
            return true
        end if
    else if m.focusZone = "seasons"
        if key = "up"
            m.focusZone = "actions"
            m.top.setFocus(true)
            updateFocusVisuals()
            return true
        else if key = "down"
            if m.currentEpisodes.Count() > 0
                m.focusZone = "episodes"
                m.episodeList.setFocus(true)
                updateFocusVisuals()
                return true
            end if
        else if key = "left"
            if m.seasons.Count() > 1
                m.selectedSeasonIdx = m.selectedSeasonIdx - 1
                if m.selectedSeasonIdx < 0 then m.selectedSeasonIdx = m.seasons.Count() - 1
                renderSelectedSeasonEpisodes()
                return true
            end if
        else if key = "right" or key = "OK"
            if m.seasons.Count() > 1
                m.selectedSeasonIdx = m.selectedSeasonIdx + 1
                if m.selectedSeasonIdx >= m.seasons.Count() then m.selectedSeasonIdx = 0
                renderSelectedSeasonEpisodes()
                return true
            end if
        end if
    else if m.focusZone = "episodes"
        if key = "up"
            ' If already at top episode, move up to season bar or actions
            if m.episodeList.itemFocused <= 0
                if m.seasons.Count() > 1
                    m.focusZone = "seasons"
                else
                    m.focusZone = "actions"
                end if
                m.top.setFocus(true)
                updateFocusVisuals()
                return true
            end if
        end if
    end if

    return false
end function
