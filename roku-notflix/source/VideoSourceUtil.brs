' =============================================================================
' Notflix for Roku TV — Strict Roku-Compatible Video Source Classifier
' Supports native Roku Video playback: Direct MP4, HLS (.m3u8), DASH (.mpd), ISM.
' Never scrapes hidden URLs, never bypasses DRM/auth, never pretends web embeds work.
' =============================================================================

function IsPlaybackCompleted(positionMs as double, durationMs as double) as boolean
    cfg = NotflixConfig()
    if durationMs < cfg.minCompletionDurationMs
        return false
    end if
    if positionMs < cfg.minWatchedPositionMs
        return false
    end if
    return positionMs >= (durationMs * cfg.completionThreshold)
end function

function StripUrlQueryAndFragment(rawUrl as string) as string
    clean = rawUrl.Trim()
    qIdx = Instr(1, clean, "?")
    if qIdx > 0
        clean = Left(clean, qIdx - 1)
    end if
    hIdx = Instr(1, clean, "#")
    if hIdx > 0
        clean = Left(clean, hIdx - 1)
    end if
    return LCase(clean)
end function

function EndsWithStr(haystack as string, suffix as string) as boolean
    hLen = Len(haystack)
    sLen = Len(suffix)
    if sLen = 0 or hLen < sLen
        return false
    end if
    return Right(haystack, sLen) = suffix
end function

function ExtractUrlHost(rawUrl as string) as string
    clean = LCase(rawUrl.Trim())
    schemeIdx = Instr(1, clean, "://")
    if schemeIdx <= 0
        return ""
    end if
    rest = Mid(clean, schemeIdx + 3)
    slashIdx = Instr(1, rest, "/")
    if slashIdx > 0
        rest = Left(rest, slashIdx - 1)
    end if
    portIdx = Instr(1, rest, ":")
    if portIdx > 0
        rest = Left(rest, portIdx - 1)
    end if
    if Left(rest, 4) = "www."
        rest = Mid(rest, 5)
    end if
    return rest
end function

function IsFirebaseStorageHost(host as string) as boolean
    return host = "firebasestorage.googleapis.com" or host = "storage.googleapis.com" or EndsWithStr(host, ".firebasestorage.app")
end function

function DetectWebEmbedProvider(host as string, lowerUrl as string) as string
    if host = "youtube.com" or host = "m.youtube.com" or host = "youtu.be" or host = "youtube-nocookie.com" or host = "music.youtube.com"
        return "YouTube"
    else if host = "vimeo.com" or host = "player.vimeo.com"
        return "Vimeo"
    else if host = "dailymotion.com" or host = "dai.ly" or host = "geo.dailymotion.com"
        return "Dailymotion"
    else if host = "twitch.tv" or host = "m.twitch.tv" or host = "player.twitch.tv" or host = "clips.twitch.tv"
        return "Twitch"
    else if host = "drive.google.com" or host = "docs.google.com"
        return "Google Drive"
    else if host = "streamable.com"
        return "Streamable"
    else if host = "loom.com"
        return "Loom"
    else if host = "archive.org" and (Instr(1, lowerUrl, "/embed/") > 0 or Instr(1, lowerUrl, "/details/") > 0)
        return "Internet Archive Web Embed"
    else if Left(host, 7) = "player." or Left(host, 6) = "embed." or Instr(1, lowerUrl, "/embed/") > 0
        return "Web Embed"
    end if
    return ""
end function

' Evaluates a single candidate URL for native Roku Video node compatibility.
function ClassifyRokuStreamUrl(rawUrl as string, sourceTypeHint = "auto" as string, providerHint = "" as string) as object
    clean = rawUrl.Trim()
    if clean = ""
        return {
            playable: false
            url: ""
            streamFormat: ""
            sourceLabel: "No Stream"
            errorTitle: "No Video Source Configured"
            errorMessage: "This title does not have a playable video stream URL configured yet."
        }
    end if

    lower = LCase(clean)
    if Left(lower, 7) = "http://"
        return {
            playable: false
            url: clean
            streamFormat: ""
            sourceLabel: "Insecure HTTP"
            errorTitle: "Secure HTTPS Stream Required"
            errorMessage: "This stream uses insecure http://. Update the title in Notflix Admin to an https:// MP4 or HLS (.m3u8) stream."
        }
    end if
    if Left(lower, 8) <> "https://"
        return {
            playable: false
            url: clean
            streamFormat: ""
            sourceLabel: "Invalid URL"
            errorTitle: "Unsupported Video URL"
            errorMessage: "The configured stream URL is not a valid https:// media stream."
        }
    end if

    host = ExtractUrlHost(clean)
    pathNoQuery = StripUrlQueryAndFragment(clean)
    hint = LCase(sourceTypeHint.Trim())

    ' Reject web-embedded video providers clearly — Roku has no WebView/IFrame player
    embedProvider = DetectWebEmbedProvider(host, lower)
    if embedProvider <> ""
        displayProv = embedProvider
        if providerHint <> "" then displayProv = providerHint
        return {
            playable: false
            url: clean
            streamFormat: ""
            sourceLabel: displayProv + " (Web Only)"
            errorTitle: "Not Playable on Roku TV"
            errorMessage: "This title uses a " + displayProv + " web embed. Roku TV requires a direct MP4 or HLS (.m3u8) video stream and cannot play web browser embeds. Watch this title on Notflix for Android, or add an MP4/HLS stream URL in Admin."
        }
    end if

    ' 1. HLS (.m3u8)
    if EndsWithStr(pathNoQuery, ".m3u8") or Instr(1, lower, ".m3u8?") > 0 or hint = "hls"
        if EndsWithStr(pathNoQuery, ".html") or EndsWithStr(pathNoQuery, ".htm")
            return {
                playable: false
                url: clean
                streamFormat: ""
                sourceLabel: "Webpage"
                errorTitle: "Webpage Is Not a Video Stream"
                errorMessage: "This link points to an HTML webpage rather than a playable HLS (.m3u8) stream."
            }
        end if
        return {
            playable: true
            url: clean
            streamFormat: "hls"
            sourceLabel: "HLS Stream"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    ' 2. MPEG-DASH (.mpd)
    if EndsWithStr(pathNoQuery, ".mpd") or Instr(1, lower, ".mpd?") > 0
        return {
            playable: true
            url: clean
            streamFormat: "dash"
            sourceLabel: "MPEG-DASH"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    ' 3. Smooth Streaming (.ism)
    if EndsWithStr(pathNoQuery, ".ism") or Instr(1, lower, ".ism/manifest") > 0
        return {
            playable: true
            url: clean
            streamFormat: "ism"
            sourceLabel: "Smooth Streaming"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    ' 4. Direct MP4 / M4V / MOV / MKV
    if EndsWithStr(pathNoQuery, ".mp4") or EndsWithStr(pathNoQuery, ".m4v") or EndsWithStr(pathNoQuery, ".mov")
        return {
            playable: true
            url: clean
            streamFormat: "mp4"
            sourceLabel: "Direct MP4"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    if EndsWithStr(pathNoQuery, ".mkv")
        return {
            playable: true
            url: clean
            streamFormat: "mkv"
            sourceLabel: "Direct MKV"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    ' 5. Firebase Storage direct uploaded video files (matches Android VideoSourceUtils.isDirectMp4OrStorageFile)
    if IsFirebaseStorageHost(host) and not EndsWithStr(pathNoQuery, ".html") and not EndsWithStr(pathNoQuery, ".htm")
        if Instr(1, lower, ".m3u8") > 0
            return { playable: true, url: clean, streamFormat: "hls", sourceLabel: "HLS Stream", errorTitle: "", errorMessage: "" }
        end if
        return {
            playable: true
            url: clean
            streamFormat: "mp4"
            sourceLabel: "Notflix Cloud MP4"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    ' 6. Explicit MP4 hint from catalog metadata (if not an HTML webpage)
    if hint = "mp4" and not EndsWithStr(pathNoQuery, ".html") and not EndsWithStr(pathNoQuery, ".htm")
        return {
            playable: true
            url: clean
            streamFormat: "mp4"
            sourceLabel: "Direct MP4"
            errorTitle: ""
            errorMessage: ""
        }
    end if

    ' 7. Explicit embed/youtube hint or unrecognized webpage
    if hint = "youtube" or hint = "embed"
        return {
            playable: false
            url: clean
            streamFormat: ""
            sourceLabel: "Web Embed (Android Only)"
            errorTitle: "Web Embed Not Supported on Roku TV"
            errorMessage: "This title uses a web browser embed player, which Roku TV does not support natively. Add a direct MP4 or HLS (.m3u8) stream to play it on Roku TV."
        }
    end if

    return {
        playable: false
        url: clean
        streamFormat: ""
        sourceLabel: "Unsupported Webpage"
        errorTitle: "Unsupported Video Source"
        errorMessage: "This link does not appear to be a direct MP4 or HLS (.m3u8) video stream. Roku TV requires a direct media stream URL."
    }
end function

' Resolves the best Roku-playable source from a CatalogItem or Episode,
' checking hlsUrl AND videoUrl so titles with a backup HLS/MP4 stream play seamlessly!
function ResolveRokuPlayableSource(mediaItem as object) as object
    if mediaItem = invalid
        return ClassifyRokuStreamUrl("", "auto", "")
    end if

    hlsUrl = SafeStr(mediaItem.hlsUrl, "").Trim()
    videoUrl = SafeStr(mediaItem.videoUrl, "").Trim()
    srcType = SafeStr(mediaItem.videoSourceType, "auto")
    provider = SafeStr(mediaItem.providerName, "")

    ' Prefer explicit HLS URL when present and Roku-playable
    if hlsUrl <> ""
        hlsCheck = ClassifyRokuStreamUrl(hlsUrl, "hls", provider)
        if hlsCheck.playable
            return hlsCheck
        end if
    end if

    ' Otherwise classify primary videoUrl
    primaryCheck = ClassifyRokuStreamUrl(videoUrl, srcType, provider)
    if primaryCheck.playable
        return primaryCheck
    end if

    ' If videoUrl failed (e.g. YouTube embed) and hlsUrl was non-empty, return primary error
    return primaryCheck
end function
