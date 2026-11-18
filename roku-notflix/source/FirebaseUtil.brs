' =============================================================================
' Notflix for Roku TV — Firebase REST, Firestore Document Mapper & Registry Util
' Must be invoked from SceneGraph Task nodes (never blocks the UI render thread).
' =============================================================================

function HttpJsonRequest(method as string, url as string, bodyObj as dynamic, bearerToken as string, timeoutMs = 15000 as integer) as object
    transfer = CreateObject("roUrlTransfer")
    if transfer = invalid
        return { ok: false, statusCode: -1, data: invalid, errorMessage: "Network client unavailable on device." }
    end if

    port = CreateObject("roMessagePort")
    transfer.SetMessagePort(port)
    transfer.SetUrl(url)
    transfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    transfer.InitClientCertificates()
    transfer.EnableEncodings(true)
    transfer.RetainBodyOnError(true)
    transfer.AddHeader("Accept", "application/json")
    if bearerToken <> ""
        transfer.AddHeader("Authorization", "Bearer " + bearerToken)
    end if

    upperMethod = UCase(method)
    payload = ""
    if bodyObj <> invalid
        payload = FormatJson(bodyObj)
        transfer.AddHeader("Content-Type", "application/json; charset=utf-8")
    end if

    started = false
    if upperMethod = "GET"
        started = transfer.AsyncGetToString()
    else if upperMethod = "POST"
        started = transfer.AsyncPostFromString(payload)
    else
        transfer.SetRequest(upperMethod)
        if payload <> ""
            started = transfer.AsyncPostFromString(payload)
        else
            started = transfer.AsyncGetToString()
        end if
    end if

    if not started
        return { ok: false, statusCode: -2, data: invalid, errorMessage: "Unable to connect. Please check your internet connection." }
    end if

    msg = wait(timeoutMs, port)
    if msg = invalid
        transfer.AsyncCancel()
        return { ok: false, statusCode: -3, data: invalid, errorMessage: "Request timed out. Your connection may be slow or offline." }
    end if

    if Type(msg) = "roUrlEvent"
        code = msg.GetResponseCode()
        rawBody = msg.GetString()
        parsed = invalid
        if rawBody <> invalid and rawBody <> ""
            parsed = ParseJson(rawBody)
        end if
        if code >= 200 and code < 300
            return { ok: true, statusCode: code, data: parsed, rawBody: rawBody, errorMessage: "" }
        else
            errText = ExtractFirebaseError(parsed, code, msg.GetFailureReason())
            return { ok: false, statusCode: code, data: parsed, rawBody: rawBody, errorMessage: errText }
        end if
    end if

    return { ok: false, statusCode: -4, data: invalid, errorMessage: "Unexpected network interruption." }
end function

function HttpUrlEncodedPost(url as string, params as object, timeoutMs = 15000 as integer) as object
    transfer = CreateObject("roUrlTransfer")
    if transfer = invalid
        return { ok: false, statusCode: -1, data: invalid, errorMessage: "Network client unavailable on device." }
    end if

    port = CreateObject("roMessagePort")
    transfer.SetMessagePort(port)
    transfer.SetUrl(url)
    transfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    transfer.InitClientCertificates()
    transfer.EnableEncodings(true)
    transfer.RetainBodyOnError(true)
    transfer.AddHeader("Accept", "application/json")
    transfer.AddHeader("Content-Type", "application/x-www-form-urlencoded")

    queryString = ""
    for each key in params
        if queryString <> "" then queryString = queryString + "&"
        queryString = queryString + transfer.Escape(key) + "=" + transfer.Escape(SafeStr(params[key]))
    end for

    print "[NotflixAuth] Sending form-urlencoded POST to: "; url
    started = transfer.AsyncPostFromString(queryString)
    if not started
        return { ok: false, statusCode: -2, data: invalid, errorMessage: "Unable to connect. Please check your internet connection." }
    end if

    msg = wait(timeoutMs, port)
    if msg = invalid
        transfer.AsyncCancel()
        return { ok: false, statusCode: -3, data: invalid, errorMessage: "Request timed out. Your connection may be slow or offline." }
    end if

    if Type(msg) = "roUrlEvent"
        code = msg.GetResponseCode()
        rawBody = msg.GetString()
        parsed = invalid
        if rawBody <> invalid and rawBody <> ""
            parsed = ParseJson(rawBody)
        end if
        print "[NotflixAuth] Form POST HTTP response code: "; code
        if code >= 200 and code < 300
            return { ok: true, statusCode: code, data: parsed, rawBody: rawBody, errorMessage: "" }
        else
            errText = ExtractFirebaseError(parsed, code, msg.GetFailureReason())
            print "[NotflixAuth] Form POST failed with error: "; errText
            return { ok: false, statusCode: code, data: parsed, rawBody: rawBody, errorMessage: errText }
        end if
    end if

    return { ok: false, statusCode: -4, data: invalid, errorMessage: "Unexpected network interruption." }
end function

function ExtractFirebaseError(parsed as dynamic, statusCode as integer, fallbackReason as string) as string
    if parsed <> invalid and Type(parsed) = "roAssociativeArray"
        if parsed.error <> invalid and Type(parsed.error) = "roAssociativeArray"
            if parsed.error.message <> invalid
                return HumanizeFirebaseErrorCode(parsed.error.message)
            end if
        else if parsed.error <> invalid and Type(parsed.error) = "roString"
            return HumanizeFirebaseErrorCode(parsed.error)
        end if
    end if
    if statusCode = 401
        return "Your session expired. Please sign in again."
    else if statusCode = 403
        return "Access denied by Notflix security rules."
    else if statusCode <= 0
        return "Cannot reach Notflix servers. Check your TV internet connection."
    end if
    if fallbackReason <> invalid and fallbackReason <> ""
        return "Network error (" + statusCode.ToStr() + "): " + fallbackReason
    end if
    return "Request failed (HTTP " + statusCode.ToStr() + ")."
end function

function HumanizeFirebaseErrorCode(code as string) as string
    upper = UCase(code)
    if Instr(1, upper, "INVALID_LOGIN_CREDENTIALS") > 0 or Instr(1, upper, "INVALID_PASSWORD") > 0 or Instr(1, upper, "EMAIL_NOT_FOUND") > 0
        return "Incorrect email or password. If you signed up with Google on Android, choose Set / Reset Password below."
    else if Instr(1, upper, "INVALID_EMAIL") > 0
        return "Please enter a valid email address."
    else if Instr(1, upper, "USER_DISABLED") > 0
        return "This Notflix account has been disabled."
    else if Instr(1, upper, "TOO_MANY_ATTEMPTS") > 0
        return "Too many sign-in attempts. Please wait a moment and try again."
    else if Instr(1, upper, "TOKEN_EXPIRED") > 0 or Instr(1, upper, "INVALID_REFRESH_TOKEN") > 0
        return "Your session expired. Please sign in again."
    end if
    return code
end function

' =============================================================================
' Firestore REST Value Decoders & Encoders
' =============================================================================

function ExtractDocId(fullName as dynamic) as string
    if fullName = invalid or fullName = ""
        return ""
    end if
    parts = fullName.Split("/")
    if parts.Count() > 0
        return parts[parts.Count() - 1]
    end if
    return ""
end function

function ParseFirestoreValue(fv as dynamic) as dynamic
    if fv = invalid or Type(fv) <> "roAssociativeArray"
        return invalid
    end if

    if fv.DoesExist("stringValue")
        return fv.stringValue
    else if fv.DoesExist("booleanValue")
        return fv.booleanValue
    else if fv.DoesExist("integerValue")
        intRaw = fv.integerValue
        if Type(intRaw) = "roString" or Type(intRaw) = "String"
            return Val(intRaw)
        end if
        return intRaw
    else if fv.DoesExist("doubleValue")
        return fv.doubleValue
    else if fv.DoesExist("timestampValue")
        return fv.timestampValue
    else if fv.DoesExist("nullValue")
        return invalid
    else if fv.DoesExist("arrayValue")
        outArr = []
        if fv.arrayValue <> invalid and fv.arrayValue.values <> invalid
            for each elem in fv.arrayValue.values
                parsedElem = ParseFirestoreValue(elem)
                if parsedElem <> invalid
                    outArr.Push(parsedElem)
                end if
            end for
        end if
        return outArr
    else if fv.DoesExist("mapValue")
        outMap = {}
        if fv.mapValue <> invalid and fv.mapValue.fields <> invalid
            for each k in fv.mapValue.fields
                outMap[k] = ParseFirestoreValue(fv.mapValue.fields[k])
            end for
        end if
        return outMap
    end if
    return invalid
end function

function ParseFirestoreDocument(doc as dynamic) as dynamic
    if doc = invalid or Type(doc) <> "roAssociativeArray"
        return invalid
    end if
    result = {
        id: ExtractDocId(doc.name)
    }
    if doc.fields <> invalid and Type(doc.fields) = "roAssociativeArray"
        for each key in doc.fields
            result[key] = ParseFirestoreValue(doc.fields[key])
        end for
    end if
    return result
end function

function SafeStr(valObj as dynamic, fallback = "" as string) as string
    if valObj = invalid
        return fallback
    end if
    t = Type(valObj)
    if t = "roString" or t = "String"
        return valObj
    else if t = "roInt" or t = "Integer" or t = "roInteger" or t = "LongInteger" or t = "roLongInteger"
        return valObj.ToStr()
    else if t = "roFloat" or t = "Float" or t = "roDouble" or t = "Double"
        return Str(valObj).Trim()
    end if
    return fallback
end function

function SafeInt(valObj as dynamic, fallback = 0 as integer) as integer
    if valObj = invalid
        return fallback
    end if
    t = Type(valObj)
    if t = "roInt" or t = "Integer" or t = "roInteger"
        return valObj
    else if t = "roFloat" or t = "Float" or t = "roDouble" or t = "Double" or t = "LongInteger" or t = "roLongInteger"
        return Int(valObj)
    else if t = "roString" or t = "String"
        return Int(Val(valObj))
    end if
    return fallback
end function

function SafeNum(valObj as dynamic, fallback = 0# as double) as double
    if valObj = invalid
        return fallback
    end if
    t = Type(valObj)
    if t = "roInt" or t = "Integer" or t = "roInteger" or t = "roFloat" or t = "Float" or t = "roDouble" or t = "Double" or t = "LongInteger" or t = "roLongInteger"
        return valObj * 1.0#
    else if t = "roString" or t = "String"
        return Val(valObj)
    end if
    return fallback
end function

function SafeBool(valObj as dynamic, fallback = false as boolean) as boolean
    if valObj = invalid
        return fallback
    end if
    t = Type(valObj)
    if t = "roBoolean" or t = "Boolean"
        return valObj
    else if t = "roString" or t = "String"
        return LCase(valObj) = "true"
    end if
    return fallback
end function

function SafeStrArray(valObj as dynamic) as object
    outArr = []
    if valObj <> invalid and Type(valObj) = "roArray"
        for each elem in valObj
            s = SafeStr(elem, "").Trim()
            if s <> ""
                outArr.Push(s)
            end if
        end for
    end if
    return outArr
end function

function IsoTimestampToEpoch(ts as dynamic) as integer
    if ts = invalid
        return 0
    end if
    s = SafeStr(ts, "").Trim()
    if s = ""
        return 0
    end if
    dt = CreateObject("roDateTime")
    if dt = invalid
        return 0
    end if
    ' Strip fractional seconds if present so FromISO8601String parses consistently across Roku OS versions
    dotIdx = Instr(1, s, ".")
    if dotIdx > 0
        zIdx = Instr(dotIdx, s, "Z")
        if zIdx > 0
            s = Left(s, dotIdx - 1) + "Z"
        end if
    end if
    dt.FromISO8601String(s)
    return dt.AsSeconds()
end function

' =============================================================================
' Domain Mappers (Matching StreamingModels.kt & firestore.rules)
' =============================================================================

function MapUserAccount(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    return {
        id: SafeStr(raw.id)
        userId: SafeStr(raw.userId, SafeStr(raw.id))
        email: SafeStr(raw.email)
        displayName: SafeStr(raw.displayName, "Notflix Viewer")
        role: SafeStr(raw.role, "viewer")
        publisherStatus: SafeStr(raw.publisherStatus, "none")
        isSuspended: SafeBool(raw.isSuspended, false)
        autoplayNext: SafeBool(raw.autoplayNext, true)
        subtitlesEnabled: SafeBool(raw.subtitlesEnabled, true)
    }
end function

function MapUserProfile(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    return {
        id: SafeStr(raw.id)
        userId: SafeStr(raw.userId)
        name: SafeStr(raw.name, "Viewer")
        avatarKey: SafeStr(raw.avatarKey, "crimson")
        isKids: SafeBool(raw.isKids, false)
        preferredGenres: SafeStrArray(raw.preferredGenres)
        language: SafeStr(raw.language, "English")
        subtitlesEnabled: SafeBool(raw.subtitlesEnabled, true)
        createdAtEpoch: IsoTimestampToEpoch(raw.createdAt)
    }
end function

function MapCatalogItem(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    mediaType = LCase(SafeStr(raw.mediaType, "movie"))
    if mediaType <> "series"
        mediaType = "movie"
    end if
    item = {
        id: SafeStr(raw.id)
        title: SafeStr(raw.title, "Untitled")
        synopsis: SafeStr(raw.synopsis, "")
        mediaType: mediaType
        genres: SafeStrArray(raw.genres)
        releaseYear: SafeInt(raw.releaseYear, 2025)
        runtimeMinutes: SafeInt(raw.runtimeMinutes, 90)
        ageRating: SafeStr(raw.ageRating, "PG-13")
        director: SafeStr(raw.director, "")
        cast: SafeStrArray(raw.cast)
        posterKey: SafeStr(raw.posterKey, "img_poster_chronicle")
        posterUrl: SafeStr(raw.posterUrl, "")
        backdropKey: SafeStr(raw.backdropKey, "img_hero_nebula")
        backdropUrl: SafeStr(raw.backdropUrl, "")
        trailerUrl: SafeStr(raw.trailerUrl, "")
        videoUrl: SafeStr(raw.videoUrl, "")
        videoSourceType: LCase(SafeStr(raw.videoSourceType, "auto"))
        providerName: SafeStr(raw.providerName, "")
        hlsUrl: SafeStr(raw.hlsUrl, "")
        isFeatured: SafeBool(raw.isFeatured, false)
        isVisible: SafeBool(raw.isVisible, true)
        isKidsFriendly: SafeBool(raw.isKidsFriendly, false)
        trendingScore: SafeInt(raw.trendingScore, 80)
        popularityRank: SafeInt(raw.popularityRank, 50)
        tags: SafeStrArray(raw.tags)
        keywords: SafeStrArray(raw.keywords)
        isStaffPick: SafeBool(raw.isStaffPick, false)
        isAwardWinning: SafeBool(raw.isAwardWinning, false)
        isComingSoon: SafeBool(raw.isComingSoon, false)
        createdAtEpoch: IsoTimestampToEpoch(raw.createdAt)
        updatedAtEpoch: IsoTimestampToEpoch(raw.updatedAt)
    }
    item.resolvedPosterUri = ResolvePosterUri(item)
    item.resolvedBackdropUri = ResolveBackdropUri(item)
    return item
end function

function MapSeason(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    sNum = SafeInt(raw.seasonNumber, 1)
    return {
        id: SafeStr(raw.id)
        seasonNumber: sNum
        title: SafeStr(raw.title, "Season " + sNum.ToStr())
        year: SafeInt(raw.year, 2025)
        isVisible: SafeBool(raw.isVisible, true)
    }
end function

function MapEpisode(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    sNum = SafeInt(raw.seasonNumber, 1)
    eNum = SafeInt(raw.episodeNumber, 1)
    ep = {
        id: SafeStr(raw.id)
        seasonNumber: sNum
        episodeNumber: eNum
        title: SafeStr(raw.title, "Episode " + eNum.ToStr())
        synopsis: SafeStr(raw.synopsis, "")
        runtimeMinutes: SafeInt(raw.runtimeMinutes, 42)
        thumbnailKey: SafeStr(raw.thumbnailKey, "img_hero_nebula")
        thumbnailUrl: SafeStr(raw.thumbnailUrl, "")
        videoUrl: SafeStr(raw.videoUrl, "")
        videoSourceType: LCase(SafeStr(raw.videoSourceType, "auto"))
        providerName: SafeStr(raw.providerName, "")
        hlsUrl: SafeStr(raw.hlsUrl, "")
        isVisible: SafeBool(raw.isVisible, true)
    }
    ep.resolvedThumbnailUri = ResolveEpisodeThumbUri(ep)
    return ep
end function

function MapGenre(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    return {
        id: SafeStr(raw.id)
        name: SafeStr(raw.name)
        slug: SafeStr(raw.slug)
        displayOrder: SafeInt(raw.displayOrder, 99)
        isActive: SafeBool(raw.isActive, true)
    }
end function

function MapWatchlistItem(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    return {
        id: SafeStr(raw.id)
        contentId: SafeStr(raw.contentId, SafeStr(raw.id))
        mediaType: SafeStr(raw.mediaType, "movie")
        title: SafeStr(raw.title)
        addedAtEpoch: IsoTimestampToEpoch(raw.addedAt)
    }
end function

function MapPlaybackProgress(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    posMs = SafeNum(raw.positionMs, 0#)
    durMs = SafeNum(raw.durationMs, 0#)
    isComp = SafeBool(raw.completed, false)
    if not isComp
        isComp = IsPlaybackCompleted(posMs, durMs)
    end if
    frac = 0.0
    if durMs > 0#
        frac = posMs / durMs
        if frac < 0.0 then frac = 0.0
        if frac > 1.0 then frac = 1.0
    end if
    return {
        id: SafeStr(raw.id)
        contentId: SafeStr(raw.contentId)
        episodeId: SafeStr(raw.episodeId, "")
        seasonNumber: SafeInt(raw.seasonNumber, 0)
        episodeNumber: SafeInt(raw.episodeNumber, 0)
        title: SafeStr(raw.title)
        positionMs: posMs
        durationMs: durMs
        progressFraction: frac
        completed: isComp
        updatedAtEpoch: IsoTimestampToEpoch(raw.updatedAt)
    }
end function

function MapContentRating(raw as dynamic) as dynamic
    if raw = invalid
        return invalid
    end if
    return {
        contentId: SafeStr(raw.contentId, SafeStr(raw.id))
        liked: SafeInt(raw.liked, 0)
        stars: SafeInt(raw.stars, 0)
    }
end function

' =============================================================================
' Artwork Resolution (Remote HTTPS with Bundled Fallback)
' =============================================================================

function IsValidHttpsImageUrl(urlStr as string) as boolean
    clean = urlStr.Trim()
    if clean = ""
        return false
    end if
    lower = LCase(clean)
    return Left(lower, 8) = "https://"
end function

function ResolvePosterUri(item as object) as string
    if item <> invalid
        if IsValidHttpsImageUrl(SafeStr(item.posterUrl))
            return item.posterUrl.Trim()
        end if
        pKey = LCase(SafeStr(item.posterKey))
        if pKey = "img_poster_horizon"
            return "pkg:/images/poster_horizon.jpg"
        else if pKey = "img_poster_odyssey"
            return "pkg:/images/poster_odyssey.jpg"
        else if pKey = "img_poster_chronicle"
            return "pkg:/images/poster_chronicle.jpg"
        end if
    end if
    return "pkg:/images/poster_fallback.jpg"
end function

function ResolveBackdropUri(item as object) as string
    if item <> invalid
        if IsValidHttpsImageUrl(SafeStr(item.backdropUrl))
            return item.backdropUrl.Trim()
        end if
        if IsValidHttpsImageUrl(SafeStr(item.posterUrl))
            return item.posterUrl.Trim()
        end if
    end if
    return "pkg:/images/hero_nebula.jpg"
end function

function ResolveEpisodeThumbUri(ep as object) as string
    if ep <> invalid and IsValidHttpsImageUrl(SafeStr(ep.thumbnailUrl))
        return ep.thumbnailUrl.Trim()
    end if
    return "pkg:/images/episode_fallback.jpg"
end function

' =============================================================================
' Roku Registry Session Persistence
' =============================================================================

function RegistrySaveSession(session as object) as void
    cfg = NotflixConfig()
    sec = CreateObject("roRegistrySection", cfg.registrySection)
    if sec = invalid or session = invalid
        return
    end if
    sec.Write("uid", SafeStr(session.uid))
    sec.Write("email", SafeStr(session.email))
    sec.Write("idToken", SafeStr(session.idToken))
    sec.Write("refreshToken", SafeStr(session.refreshToken))
    sec.Flush()
end function

function RegistryLoadSession() as dynamic
    cfg = NotflixConfig()
    sec = CreateObject("roRegistrySection", cfg.registrySection)
    if sec = invalid
        return invalid
    end if
    refreshToken = sec.Read("refreshToken")
    uid = sec.Read("uid")
    if refreshToken = invalid or refreshToken = "" or uid = invalid or uid = ""
        return invalid
    end if
    return {
        uid: uid
        email: SafeStr(sec.Read("email"))
        idToken: SafeStr(sec.Read("idToken"))
        refreshToken: refreshToken
    }
end function

sub RegistryClearSession()
    cfg = NotflixConfig()
    sec = CreateObject("roRegistrySection", cfg.registrySection)
    if sec <> invalid
        sec.Delete("uid")
        sec.Delete("email")
        sec.Delete("idToken")
        sec.Delete("refreshToken")
        sec.Flush()
    end if
end sub

sub RegistrySaveActiveProfile(uid as string, profileId as string)
    if uid = "" or profileId = ""
        return
    end if
    cfg = NotflixConfig()
    sec = CreateObject("roRegistrySection", cfg.registrySection)
    if sec <> invalid
        sec.Write("profile_" + uid, profileId)
        sec.Flush()
    end if
end sub

function RegistryLoadActiveProfile(uid as string) as string
    if uid = ""
        return ""
    end if
    cfg = NotflixConfig()
    sec = CreateObject("roRegistrySection", cfg.registrySection)
    if sec <> invalid
        saved = sec.Read("profile_" + uid)
        if saved <> invalid
            return saved
        end if
    end if
    return ""
end function

function FormatTimeSeconds(totalSec as integer) as string
    if totalSec < 0 then totalSec = 0
    hrs = Int(totalSec / 3600)
    mins = Int((totalSec mod 3600) / 60)
    secs = totalSec mod 60
    minStr = mins.ToStr()
    secStr = secs.ToStr()
    if secs < 10 then secStr = "0" + secStr
    if hrs > 0
        if mins < 10 then minStr = "0" + minStr
        return hrs.ToStr() + ":" + minStr + ":" + secStr
    end if
    return minStr + ":" + secStr
end function
