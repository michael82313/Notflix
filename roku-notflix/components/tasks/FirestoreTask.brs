' =============================================================================
' Notflix for Roku TV — Firestore REST Background Task
' Queries existing Notflix collections strictly within existing firestore.rules:
'   - catalog (where isVisible == true)
'   - catalog/{id}/seasons (where isVisible == true)
'   - catalog/{id}/seasons/{sid}/episodes (where isVisible == true)
'   - genres (where isActive == true)
'   - users/{uid}/profiles
'   - users/{uid}/profiles/{pid}/watchlist
'   - users/{uid}/profiles/{pid}/progress
'   - users/{uid}/profiles/{pid}/ratings
' Uses Firestore :commit with REQUEST_TIME server timestamps for all writes.
' =============================================================================

sub init()
    m.top.functionName = "executeFirestoreTask"
end sub

sub executeFirestoreTask()
    req = m.top.request
    if req = invalid or req.action = invalid
        m.top.response = { ok: false, action: "", errorMessage: "Missing Firestore request." }
        return
    end if

    action = req.action
    session = req.session
    if session = invalid or SafeStr(session.uid) = "" or SafeStr(session.idToken) = ""
        m.top.response = { ok: false, action: action, authExpired: true, errorMessage: "Sign-in required." }
        return
    end if

    m.curSession = {
        uid: SafeStr(session.uid)
        email: SafeStr(session.email)
        idToken: SafeStr(session.idToken)
        refreshToken: SafeStr(session.refreshToken)
    }

    if action = "loadProfiles"
        handleLoadProfiles()
    else if action = "loadHomeData"
        handleLoadHomeData(SafeStr(req.profileId), SafeBool(req.skipCatalog, false))
    else if action = "loadSeriesDetails"
        handleLoadSeriesDetails(SafeStr(req.contentId))
    else if action = "toggleWatchlist"
        handleToggleWatchlist(SafeStr(req.profileId), req.item, SafeBool(req.addToList, true))
    else if action = "saveProgress"
        handleSaveProgress(SafeStr(req.profileId), req.progress)
    else
        m.top.response = { ok: false, action: action, errorMessage: "Unknown Firestore action: " + action }
    end if
end sub

' Authenticated HTTP wrapper with automatic single token refresh on HTTP 401
function AuthedJsonRequest(method as string, url as string, bodyObj as dynamic, timeoutMs = 15000 as integer) as object
    res = HttpJsonRequest(method, url, bodyObj, m.curSession.idToken, timeoutMs)
    if not res.ok and res.statusCode = 401 and m.curSession.refreshToken <> ""
        if TryRefreshTokenInTask()
            res = HttpJsonRequest(method, url, bodyObj, m.curSession.idToken, timeoutMs)
        end if
    end if
    return res
end function

function TryRefreshTokenInTask() as boolean
    cfg = NotflixConfig()
    body = {
        grant_type: "refresh_token"
        refresh_token: m.curSession.refreshToken
    }
    res = HttpJsonRequest("POST", cfg.authRefreshTokenUrl, body, "", 12000)
    if res.ok and res.data <> invalid and SafeStr(res.data.id_token) <> ""
        m.curSession.idToken = SafeStr(res.data.id_token)
        if SafeStr(res.data.refresh_token) <> ""
            m.curSession.refreshToken = SafeStr(res.data.refresh_token)
        end if
        RegistrySaveSession(m.curSession)
        return true
    end if
    return false
end function

function RunStructuredQuery(parentPath as string, collectionId as string, filterField as string, filterBoolVal as boolean, limitCount as integer) as object
    cfg = NotflixConfig()
    url = cfg.firestoreDocsBase
    if parentPath <> ""
        url = url + "/" + parentPath
    end if
    url = url + ":runQuery"

    queryBody = {
        structuredQuery: {
            from: [{ collectionId: collectionId }]
            where: {
                fieldFilter: {
                    field: { fieldPath: filterField }
                    op: "EQUAL"
                    value: { booleanValue: filterBoolVal }
                }
            }
            limit: limitCount
        }
    }

    res = AuthedJsonRequest("POST", url, queryBody, 18000)
    if not res.ok
        return { ok: false, docs: [], statusCode: res.statusCode, errorMessage: res.errorMessage }
    end if

    docs = []
    if res.data <> invalid and Type(res.data) = "roArray"
        for each row in res.data
            if row <> invalid and row.document <> invalid
                parsed = ParseFirestoreDocument(row.document)
                if parsed <> invalid and parsed.id <> ""
                    docs.Push(parsed)
                end if
            end if
        end for
    end if
    return { ok: true, docs: docs, statusCode: 200, errorMessage: "" }
end function

function ListSubcollection(relativePath as string, pageSize = 200 as integer) as object
    cfg = NotflixConfig()
    url = cfg.firestoreDocsBase + "/" + relativePath + "?pageSize=" + pageSize.ToStr()
    res = AuthedJsonRequest("GET", url, invalid, 15000)
    if not res.ok
        return { ok: false, docs: [], statusCode: res.statusCode, errorMessage: res.errorMessage }
    end if
    docs = []
    if res.data <> invalid and res.data.documents <> invalid and Type(res.data.documents) = "roArray"
        for each doc in res.data.documents
            parsed = ParseFirestoreDocument(doc)
            if parsed <> invalid and parsed.id <> ""
                docs.Push(parsed)
            end if
        end for
    end if
    return { ok: true, docs: docs, statusCode: 200, errorMessage: "" }
end function

sub handleLoadProfiles()
    uid = m.curSession.uid
    listRes = ListSubcollection("users/" + uid + "/profiles", 20)
    if not listRes.ok
        m.top.response = {
            ok: false
            action: "loadProfiles"
            session: m.curSession
            authExpired: (listRes.statusCode = 401)
            errorMessage: listRes.errorMessage
        }
        return
    end if

    profiles = []
    for each raw in listRes.docs
        p = MapUserProfile(raw)
        if p <> invalid then profiles.Push(p)
    end for

    ' Sort ascending by createdAtEpoch
    count = profiles.Count()
    if count > 1
        for i = 1 to count - 1
            cur = profiles[i]
            j = i - 1
            while j >= 0 and profiles[j].createdAtEpoch > cur.createdAtEpoch
                profiles[j + 1] = profiles[j]
                j = j - 1
            end while
            profiles[j + 1] = cur
        end for
    end if

    m.top.response = {
        ok: true
        action: "loadProfiles"
        session: m.curSession
        profiles: profiles
    }
end sub

sub handleLoadHomeData(profileId as string, skipCatalog as boolean)
    uid = m.curSession.uid
    catalog = invalid
    genres = invalid

    if not skipCatalog
        catRes = RunStructuredQuery("", "catalog", "isVisible", true, 200)
        if not catRes.ok
            m.top.response = {
                ok: false
                action: "loadHomeData"
                session: m.curSession
                authExpired: (catRes.statusCode = 401)
                errorMessage: "Failed to load Notflix catalog: " + catRes.errorMessage
            }
            return
        end if

        catalog = []
        for each raw in catRes.docs
            mapped = MapCatalogItem(raw)
            if mapped <> invalid then catalog.Push(mapped)
        end for

        genreRes = RunStructuredQuery("", "genres", "isActive", true, 60)
        genres = []
        if genreRes.ok
            for each gRaw in genreRes.docs
                gMapped = MapGenre(gRaw)
                if gMapped <> invalid then genres.Push(gMapped)
            end for
        end if
    end if

    watchlist = []
    progressList = []
    ratings = []

    if profileId <> ""
        profileBase = "users/" + uid + "/profiles/" + profileId

        wlRes = ListSubcollection(profileBase + "/watchlist", 200)
        if wlRes.ok
            for each wRaw in wlRes.docs
                wMapped = MapWatchlistItem(wRaw)
                if wMapped <> invalid then watchlist.Push(wMapped)
            end for
        end if

        progRes = ListSubcollection(profileBase + "/progress", 200)
        if progRes.ok
            for each pRaw in progRes.docs
                pMapped = MapPlaybackProgress(pRaw)
                if pMapped <> invalid then progressList.Push(pMapped)
            end for
        end if

        ratRes = ListSubcollection(profileBase + "/ratings", 200)
        if ratRes.ok
            for each rRaw in ratRes.docs
                rMapped = MapContentRating(rRaw)
                if rMapped <> invalid then ratings.Push(rMapped)
            end for
        end if
    end if

    m.top.response = {
        ok: true
        action: "loadHomeData"
        session: m.curSession
        profileId: profileId
        catalog: catalog
        genres: genres
        watchlist: watchlist
        progress: progressList
        ratings: ratings
    }
end sub

sub handleLoadSeriesDetails(contentId as string)
    if contentId = ""
        m.top.response = { ok: false, action: "loadSeriesDetails", errorMessage: "Missing series ID." }
        return
    end if

    seasonRes = RunStructuredQuery("catalog/" + contentId, "seasons", "isVisible", true, 50)
    if not seasonRes.ok
        m.top.response = {
            ok: false
            action: "loadSeriesDetails"
            session: m.curSession
            contentId: contentId
            errorMessage: "Unable to load seasons: " + seasonRes.errorMessage
        }
        return
    end if

    seasons = []
    for each sRaw in seasonRes.docs
        sMapped = MapSeason(sRaw)
        if sMapped <> invalid then seasons.Push(sMapped)
    end for

    ' Sort seasons ascending by seasonNumber
    sc = seasons.Count()
    if sc > 1
        for i = 1 to sc - 1
            cur = seasons[i]
            j = i - 1
            while j >= 0 and seasons[j].seasonNumber > cur.seasonNumber
                seasons[j + 1] = seasons[j]
                j = j - 1
            end while
            seasons[j + 1] = cur
        end for
    end if

    episodesBySeason = {}
    for each season in seasons
        epRes = RunStructuredQuery("catalog/" + contentId + "/seasons/" + season.id, "episodes", "isVisible", true, 120)
        eps = []
        if epRes.ok
            for each eRaw in epRes.docs
                eMapped = MapEpisode(eRaw)
                if eMapped <> invalid then eps.Push(eMapped)
            end for
            ' Sort episodes ascending by episodeNumber
            ec = eps.Count()
            if ec > 1
                for i = 1 to ec - 1
                    curE = eps[i]
                    j = i - 1
                    while j >= 0 and eps[j].episodeNumber > curE.episodeNumber
                        eps[j + 1] = eps[j]
                        j = j - 1
                    end while
                    eps[j + 1] = curE
                end for
            end if
        end if
        episodesBySeason[season.id] = eps
    end for

    m.top.response = {
        ok: true
        action: "loadSeriesDetails"
        session: m.curSession
        contentId: contentId
        seasons: seasons
        episodesBySeason: episodesBySeason
    }
end sub

sub handleToggleWatchlist(profileId as string, item as dynamic, addToList as boolean)
    if profileId = "" or item = invalid or SafeStr(item.id) = ""
        m.top.response = { ok: false, action: "toggleWatchlist", errorMessage: "Invalid watchlist parameters." }
        return
    end if

    cfg = NotflixConfig()
    uid = m.curSession.uid
    contentId = SafeStr(item.id)
    relDoc = "users/" + uid + "/profiles/" + profileId + "/watchlist/" + contentId

    if addToList
        mediaType = SafeStr(item.mediaType, "movie")
        if mediaType <> "series" then mediaType = "movie"
        titleStr = Left(SafeStr(item.title, "Untitled").Trim(), 200)
        if titleStr = "" then titleStr = "Untitled"

        commitBody = {
            writes: [
                {
                    update: {
                        name: cfg.firestoreDocPrefix + relDoc
                        fields: {
                            contentId: { stringValue: contentId }
                            mediaType: { stringValue: mediaType }
                            title: { stringValue: titleStr }
                        }
                    }
                    updateTransforms: [
                        {
                            fieldPath: "addedAt"
                            setToServerValue: "REQUEST_TIME"
                        }
                    ]
                }
            ]
        }
        res = AuthedJsonRequest("POST", cfg.firestoreCommitUrl, commitBody, 12000)
        m.top.response = {
            ok: res.ok
            action: "toggleWatchlist"
            session: m.curSession
            contentId: contentId
            inWatchlist: addToList
            errorMessage: res.errorMessage
        }
    else
        delUrl = cfg.firestoreDocsBase + "/" + relDoc
        res = AuthedJsonRequest("DELETE", delUrl, invalid, 12000)
        m.top.response = {
            ok: res.ok
            action: "toggleWatchlist"
            session: m.curSession
            contentId: contentId
            inWatchlist: false
            errorMessage: res.errorMessage
        }
    end if
end sub

sub handleSaveProgress(profileId as string, prog as dynamic)
    if profileId = "" or prog = invalid or SafeStr(prog.contentId) = ""
        m.top.response = { ok: false, action: "saveProgress", errorMessage: "Invalid playback progress payload." }
        return
    end if

    cfg = NotflixConfig()
    uid = m.curSession.uid
    contentId = SafeStr(prog.contentId)
    episodeId = SafeStr(prog.episodeId, "")
    progressId = contentId
    if episodeId <> ""
        progressId = contentId + "_" + episodeId
    end if

    posMs = Int(SafeNum(prog.positionMs, 0#))
    if posMs < 0 then posMs = 0
    durMs = Int(SafeNum(prog.durationMs, 0#))
    if durMs < 0 then durMs = 0

    completed = SafeBool(prog.completed, false)
    if not completed
        completed = IsPlaybackCompleted(posMs * 1.0#, durMs * 1.0#)
    end if

    titleStr = Left(SafeStr(prog.title, "Untitled").Trim(), 200)
    if titleStr = "" then titleStr = "Untitled"

    fieldsObj = {
        contentId: { stringValue: contentId }
        title: { stringValue: titleStr }
        positionMs: { integerValue: posMs.ToStr() }
        durationMs: { integerValue: durMs.ToStr() }
        completed: { booleanValue: completed }
    }

    if episodeId <> ""
        fieldsObj.episodeId = { stringValue: Left(episodeId, 128) }
        sNum = SafeInt(prog.seasonNumber, 0)
        eNum = SafeInt(prog.episodeNumber, 0)
        if sNum > 0 then fieldsObj.seasonNumber = { integerValue: sNum.ToStr() }
        if eNum > 0 then fieldsObj.episodeNumber = { integerValue: eNum.ToStr() }
    end if

    relDoc = "users/" + uid + "/profiles/" + profileId + "/progress/" + progressId
    commitBody = {
        writes: [
            {
                update: {
                    name: cfg.firestoreDocPrefix + relDoc
                    fields: fieldsObj
                }
                updateTransforms: [
                    {
                        fieldPath: "updatedAt"
                        setToServerValue: "REQUEST_TIME"
                    }
                ]
            }
        ]
    }

    res = AuthedJsonRequest("POST", cfg.firestoreCommitUrl, commitBody, 12000)
    m.top.response = {
        ok: res.ok
        action: "saveProgress"
        session: m.curSession
        progressId: progressId
        contentId: contentId
        episodeId: episodeId
        completed: completed
        errorMessage: res.errorMessage
    }
end sub
