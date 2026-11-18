sub init()
    m.videoPlayer = m.top.findNode("videoPlayer")
    m.progressTimer = m.top.findNode("progressTimer")

    m.titleBanner = m.top.findNode("titleBanner")
    m.bannerTitle = m.top.findNode("bannerTitle")
    m.bannerSubtitle = m.top.findNode("bannerSubtitle")

    m.bufferingOverlay = m.top.findNode("bufferingOverlay")
    m.bufferingLabel = m.top.findNode("bufferingLabel")

    m.errorOverlay = m.top.findNode("errorOverlay")
    m.errBadge = m.top.findNode("errBadge")
    m.errTitle = m.top.findNode("errTitle")
    m.errMessage = m.top.findNode("errMessage")
    m.errRetryGroup = m.top.findNode("errRetryGroup")
    m.errRetryBorder = m.top.findNode("errRetryBorder")
    m.errRetryBg = m.top.findNode("errRetryBg")
    m.errBackGroup = m.top.findNode("errBackGroup")
    m.errBackBorder = m.top.findNode("errBackBorder")
    m.errBackBg = m.top.findNode("errBackBg")

    m.upNextOverlay = m.top.findNode("upNextOverlay")
    m.upNextTitle = m.top.findNode("upNextTitle")
    m.upNextSynopsis = m.top.findNode("upNextSynopsis")
    m.upNextPlayBorder = m.top.findNode("upNextPlayBorder")
    m.upNextPlayBg = m.top.findNode("upNextPlayBg")
    m.upNextBackBorder = m.top.findNode("upNextBackBorder")
    m.upNextBackBg = m.top.findNode("upNextBackBg")

    m.curReq = invalid
    ' mode: "playing", "error", "up_next"
    m.mode = "playing"
    m.canRetryError = false
    m.errorBtnIdx = 0
    m.upNextBtnIdx = 0
    m.lastKnownPosSec = 0.0#
    m.lastKnownDurSec = 0.0#
    m.hasStartedPlaying = false

    m.videoPlayer.observeField("state", "onVideoStateChanged")
    m.videoPlayer.observeField("position", "onVideoPositionChanged")
    m.videoPlayer.observeField("duration", "onVideoDurationChanged")
    m.progressTimer.observeField("fire", "onProgressTimerFire")
end sub

sub onPlayRequestChanged()
    req = m.top.playRequest
    if req = invalid or req.item = invalid then return

    m.curReq = req
    m.lastKnownPosSec = 0.0#
    m.lastKnownDurSec = 0.0#
    m.hasStartedPlaying = false

    m.errorOverlay.visible = false
    m.upNextOverlay.visible = false
    m.bufferingOverlay.visible = false
    m.progressTimer.control = "stop"

    displayTitle = SafeStr(req.item.title, "Untitled")
    subText = "Notflix  •  Press Back to return to Details"
    if req.episode <> invalid
        epCode = "S" + SafeInt(req.seasonNumber, 1).ToStr() + " E" + SafeInt(req.episodeNumber, 1).ToStr()
        displayTitle = displayTitle + "  —  " + epCode + ": " + SafeStr(req.episode.title, "Episode")
    end if
    m.bannerTitle.text = displayTitle
    m.bannerSubtitle.text = subText

    src = req.sourceInfo
    if src = invalid
        if req.episode <> invalid
            src = ResolveRokuPlayableSource(req.episode)
        else
            src = ResolveRokuPlayableSource(req.item)
        end if
        m.curReq.sourceInfo = src
    end if

    if not src.playable
        m.videoPlayer.control = "stop"
        m.videoPlayer.visible = false
        showErrorOverlay("INCOMPATIBLE STREAM FORMAT", src.errorTitle, src.errorMessage, false)
        return
    end if

    startStreamPlayback(src, displayTitle, SafeInt(req.resumeSec, 0))
end sub

sub startStreamPlayback(src as object, displayTitle as string, resumeSec as integer)
    m.mode = "playing"
    m.errorOverlay.visible = false
    m.upNextOverlay.visible = false
    m.titleBanner.visible = true
    m.bufferingOverlay.visible = true
    m.bufferingLabel.text = "Loading " + src.sourceLabel + "..."

    contentNode = CreateObject("roSGNode", "ContentNode")
    contentNode.url = src.url
    contentNode.title = displayTitle
    contentNode.streamFormat = src.streamFormat
    if resumeSec >= 10
        contentNode.playStart = resumeSec
        m.lastKnownPosSec = resumeSec * 1.0#
    end if
    if m.curReq.item <> invalid
        contentNode.HDPosterUrl = SafeStr(m.curReq.item.resolvedPosterUri, "pkg:/images/poster_fallback.jpg")
    end if

    m.videoPlayer.visible = true
    m.videoPlayer.content = contentNode
    m.videoPlayer.setFocus(true)
    m.videoPlayer.control = "play"
end sub

sub onVideoPositionChanged()
    pVal = m.videoPlayer.position
    if pVal > 0
        m.lastKnownPosSec = pVal
    end if
end sub

sub onVideoDurationChanged()
    dVal = m.videoPlayer.duration
    if dVal > 0
        m.lastKnownDurSec = dVal
    end if
end sub

sub onVideoStateChanged()
    st = m.videoPlayer.state
    if st = "playing"
        m.hasStartedPlaying = true
        m.bufferingOverlay.visible = false
        m.titleBanner.visible = false
        m.progressTimer.control = "start"
    else if st = "buffering"
        if not m.hasStartedPlaying
            m.bufferingOverlay.visible = true
            m.titleBanner.visible = true
        end if
    else if st = "paused"
        m.titleBanner.visible = true
        m.progressTimer.control = "stop"
        emitProgressSave(false)
    else if st = "finished"
        m.progressTimer.control = "stop"
        m.titleBanner.visible = false
        m.bufferingOverlay.visible = false
        emitProgressSave(true)
        handlePlaybackFinished()
    else if st = "error"
        m.progressTimer.control = "stop"
        m.bufferingOverlay.visible = false
        m.titleBanner.visible = false
        m.videoPlayer.control = "stop"
        m.videoPlayer.visible = false
        errCode = m.videoPlayer.errorCode
        errMsg = SafeStr(m.videoPlayer.errorMsg, "")
        friendly = HumanizeRokuVideoError(errCode, errMsg)
        showErrorOverlay("VIDEO PLAYBACK ERROR", "Unable to Play Video Stream", friendly, true)
    end if
end sub

sub onProgressTimerFire()
    if m.mode = "playing" and m.videoPlayer.state = "playing"
        emitProgressSave(false)
    end if
end sub

sub emitProgressSave(forceCompleted as boolean)
    if m.curReq = invalid or m.curReq.item = invalid then return
    if not m.hasStartedPlaying and not forceCompleted then return

    curSec = m.videoPlayer.position
    if curSec <= 0 then curSec = m.lastKnownPosSec
    durSec = m.videoPlayer.duration
    if durSec <= 0 then durSec = m.lastKnownDurSec

    ' Fallback duration from catalog runtimeMinutes if Roku hasn't reported duration yet
    if durSec <= 0
        rtMin = SafeInt(m.curReq.item.runtimeMinutes, 0)
        if m.curReq.episode <> invalid and SafeInt(m.curReq.episode.runtimeMinutes, 0) > 0
            rtMin = SafeInt(m.curReq.episode.runtimeMinutes, 0)
        end if
        if rtMin > 0 then durSec = rtMin * 60.0#
    end if

    posMs = curSec * 1000.0#
    durMs = durSec * 1000.0#
    if forceCompleted and durMs > 0
        posMs = durMs
    end if
    if posMs < 3000# and not forceCompleted then return

    epId = ""
    sNum = 0
    eNum = 0
    if m.curReq.episode <> invalid
        epId = SafeStr(m.curReq.episode.id)
        sNum = SafeInt(m.curReq.seasonNumber, 0)
        eNum = SafeInt(m.curReq.episodeNumber, 0)
    end if

    m.top.saveProgressEvent = {
        contentId: m.curReq.item.id
        episodeId: epId
        seasonNumber: sNum
        episodeNumber: eNum
        title: SafeStr(m.curReq.item.title, "Untitled")
        positionMs: posMs
        durationMs: durMs
        completed: forceCompleted or IsPlaybackCompleted(posMs, durMs)
    }
end sub

sub handlePlaybackFinished()
    if m.curReq <> invalid and m.curReq.nextEpisode <> invalid
        m.mode = "up_next"
        m.upNextBtnIdx = 0
        nextEp = m.curReq.nextEpisode
        nextSn = SafeInt(m.curReq.nextSeasonNumber, 1)
        m.upNextTitle.text = "S" + nextSn.ToStr() + " E" + nextEp.episodeNumber.ToStr() + "  •  " + SafeStr(nextEp.title, "Next Episode")
        m.upNextSynopsis.text = SafeStr(nextEp.synopsis, "Continue watching the next episode.")
        m.videoPlayer.control = "stop"
        m.videoPlayer.visible = false
        m.upNextOverlay.visible = true
        m.top.setFocus(true)
        updateUpNextVisuals()
    else
        closePlayer(false)
    end if
end sub

sub showErrorOverlay(badgeText as string, titleText as string, msgText as string, allowRetry as boolean)
    m.mode = "error"
    m.canRetryError = allowRetry
    m.errBadge.text = badgeText
    m.errTitle.text = titleText
    m.errMessage.text = msgText

    if allowRetry
        m.errRetryGroup.visible = true
        m.errBackGroup.translation = [814, 648]
        m.errorBtnIdx = 0
    else
        m.errRetryGroup.visible = false
        m.errBackGroup.translation = [530, 648]
        m.errorBtnIdx = 1
    end if

    m.errorOverlay.visible = true
    m.top.setFocus(true)
    updateErrorVisuals()
end sub

sub updateErrorVisuals()
    if m.canRetryError and m.errorBtnIdx = 0
        m.errRetryBorder.color = "0xFFFFFFFF"
        m.errRetryBg.color = "0xE50914FF"
        m.errBackBorder.color = "0x2E2E3AFF"
        m.errBackBg.color = "0x1F1F2BFF"
    else
        m.errRetryBorder.color = "0x2E2E3AFF"
        m.errRetryBg.color = "0x8C0810FF"
        m.errBackBorder.color = "0xE50914FF"
        m.errBackBg.color = "0x2A2A3AFF"
    end if
end sub

sub updateUpNextVisuals()
    if m.upNextBtnIdx = 0
        m.upNextPlayBorder.color = "0xFFFFFFFF"
        m.upNextPlayBg.color = "0xE50914FF"
        m.upNextBackBorder.color = "0x2E2E3AFF"
        m.upNextBackBg.color = "0x1F1F2BFF"
    else
        m.upNextPlayBorder.color = "0x2E2E3AFF"
        m.upNextPlayBg.color = "0x8C0810FF"
        m.upNextBackBorder.color = "0xE50914FF"
        m.upNextBackBg.color = "0x2A2A3AFF"
    end if
end sub

sub closePlayer(saveFirst as boolean)
    m.progressTimer.control = "stop"
    if saveFirst
        emitProgressSave(false)
    end if
    m.videoPlayer.control = "stop"
    m.videoPlayer.visible = false
    m.errorOverlay.visible = false
    m.upNextOverlay.visible = false
    m.bufferingOverlay.visible = false
    m.titleBanner.visible = false
    m.top.closed = true
end sub

function HumanizeRokuVideoError(code as integer, rawMsg as string) as string
    detail = ""
    if rawMsg <> "" then detail = " (" + rawMsg + ")"
    if code = -1 or code = -2
        return "Network connection error while loading this stream. Check your TV internet connection and try again." + detail
    else if code = -3 or code = -4
        return "The video server returned an unexpected response or the stream URL is no longer accessible." + detail
    else if code = -5 or code = -6
        return "This media format or codec could not be decoded by Roku TV. Verify the stream is a standard H.264/HEVC MP4 or HLS (.m3u8) stream." + detail
    end if
    return "Roku could not play this stream (code " + code.ToStr() + "). Verify the video URL in Notflix Admin is a valid HTTPS MP4 or HLS (.m3u8) stream." + detail
end function

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    if m.mode = "error"
        if key = "back"
            closePlayer(false)
            return true
        else if key = "left" and m.canRetryError
            m.errorBtnIdx = 0
            updateErrorVisuals()
            return true
        else if key = "right" and m.canRetryError
            m.errorBtnIdx = 1
            updateErrorVisuals()
            return true
        else if key = "OK"
            if m.canRetryError and m.errorBtnIdx = 0 and m.curReq <> invalid
                startStreamPlayback(m.curReq.sourceInfo, m.bannerTitle.text, Int(m.lastKnownPosSec))
            else
                closePlayer(false)
            end if
            return true
        end if
        return true
    else if m.mode = "up_next"
        if key = "back"
            closePlayer(false)
            return true
        else if key = "left"
            m.upNextBtnIdx = 0
            updateUpNextVisuals()
            return true
        else if key = "right"
            m.upNextBtnIdx = 1
            updateUpNextVisuals()
            return true
        else if key = "OK" or key = "play"
            if m.upNextBtnIdx = 0 and m.curReq <> invalid and m.curReq.nextEpisode <> invalid
                m.upNextOverlay.visible = false
                m.top.playNextEpisodeEvent = {
                    item: m.curReq.item
                    episode: m.curReq.nextEpisode
                    seasonNumber: m.curReq.nextSeasonNumber
                }
            else
                closePlayer(false)
            end if
            return true
        end if
        return true
    end if

    ' Active video playback remote handling
    if key = "back"
        closePlayer(true)
        return true
    end if

    return false
end function
