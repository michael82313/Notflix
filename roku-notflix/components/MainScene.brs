sub init()
    m.signInScreen = m.top.findNode("signInScreen")
    m.profileScreen = m.top.findNode("profileScreen")
    m.homeScreen = m.top.findNode("homeScreen")
    m.searchScreen = m.top.findNode("searchScreen")
    m.myListScreen = m.top.findNode("myListScreen")
    m.detailsScreen = m.top.findNode("detailsScreen")
    m.playerScreen = m.top.findNode("playerScreen")

    m.navRailContainer = m.top.findNode("navRailContainer")
    m.navScrim = m.top.findNode("navScrim")
    m.navBg = m.top.findNode("navBg")
    m.navDivider = m.top.findNode("navDivider")
    m.navBrandLabel = m.top.findNode("navBrandLabel")
    m.navPrivacyFooter = m.top.findNode("navPrivacyFooter")

    m.navBgNodes = []
    m.navBarNodes = []
    m.navShortNodes = []
    m.navFullNodes = []
    for i = 0 to 4
        m.navBgNodes.Push(m.top.findNode("navBg" + i.ToStr()))
        m.navBarNodes.Push(m.top.findNode("navBar" + i.ToStr()))
        m.navShortNodes.Push(m.top.findNode("navShort" + i.ToStr()))
        m.navFullNodes.Push(m.top.findNode("navFull" + i.ToStr()))
    end for

    m.toastBanner = m.top.findNode("toastBanner")
    m.toastAccent = m.top.findNode("toastAccent")
    m.toastLabel = m.top.findNode("toastLabel")
    m.toastTimer = m.top.findNode("toastTimer")
    m.toastTimer.observeField("fire", "onToastTimerFire")

    m.loadingOverlay = m.top.findNode("loadingOverlay")
    m.loadingText = m.top.findNode("loadingText")

    ' App State
    m.session = invalid
    m.account = invalid
    m.profiles = []
    m.activeProfile = invalid
    m.catalog = []
    m.genres = []
    m.watchlist = []
    m.progressList = []
    m.ratings = []
    m.seriesCache = {}
    m.selectedDetailItem = invalid
    m.pendingDeepLinkId = ""

    ' Navigation State
    ' activeScreen: "loading", "signin", "profiles", "home", "search", "mylist", "details", "player"
    m.activeScreen = "loading"
    m.returnTab = "home"
    m.navExpanded = false
    m.navFocusedIdx = 0
    m.navActions = ["home", "search", "mylist", "profiles", "signout"]

    ' Background Tasks (separate read & write Firestore tasks so background saves never block UI loads)
    m.authTask = CreateObject("roSGNode", "AuthTask")
    m.authTask.observeField("response", "onAuthResponse")

    m.firestoreReadTask = CreateObject("roSGNode", "FirestoreTask")
    m.firestoreReadTask.observeField("response", "onFirestoreReadResponse")

    m.firestoreWriteTask = CreateObject("roSGNode", "FirestoreTask")
    m.firestoreWriteTask.observeField("response", "onFirestoreWriteResponse")

    ' Screen Event Observers
    m.signInScreen.observeField("signInRequest", "onSignInRequested")
    m.signInScreen.observeField("resetPasswordRequest", "onResetPasswordRequested")

    m.profileScreen.observeField("selectedProfile", "onProfileSelected")
    m.profileScreen.observeField("retryRequested", "onProfileRetryRequested")
    m.profileScreen.observeField("signOutRequested", "onSignOutRequested")

    m.homeScreen.observeField("selectedItem", "onContentItemSelected")
    m.homeScreen.observeField("requestNavFocus", "onRequestNavFocus")

    m.searchScreen.observeField("selectedItem", "onContentItemSelected")
    m.searchScreen.observeField("requestNavFocus", "onRequestNavFocus")

    m.myListScreen.observeField("selectedItem", "onContentItemSelected")
    m.myListScreen.observeField("requestNavFocus", "onRequestNavFocus")

    m.detailsScreen.observeField("playRequest", "onDetailsPlayRequested")
    m.detailsScreen.observeField("toggleWatchlistRequest", "onToggleWatchlistRequested")
    m.detailsScreen.observeField("backRequested", "onDetailsBackRequested")

    m.playerScreen.observeField("saveProgressEvent", "onPlayerSaveProgress")
    m.playerScreen.observeField("playNextEpisodeEvent", "onPlayerNextEpisode")
    m.playerScreen.observeField("closed", "onPlayerClosed")

    ' Start session restoration on boot
    showLoading("Connecting to Notflix...")
    runAuthTask({ action: "restore" })
end sub

sub onLaunchArgsChanged()
    args = m.top.launchArgs
    if args <> invalid and SafeStr(args.contentId) <> ""
        m.pendingDeepLinkId = SafeStr(args.contentId)
    end if
end sub

' =============================================================================
' Task Runners
' =============================================================================
sub runAuthTask(req as object)
    m.authTask.control = "stop"
    m.authTask.request = req
    m.authTask.control = "run"
end sub

sub runFirestoreRead(req as object)
    m.firestoreReadTask.control = "stop"
    m.firestoreReadTask.request = req
    m.firestoreReadTask.control = "run"
end sub

sub runFirestoreWrite(req as object)
    m.firestoreWriteTask.control = "stop"
    m.firestoreWriteTask.request = req
    m.firestoreWriteTask.control = "run"
end sub

' =============================================================================
' Auth Handlers
' =============================================================================
sub onSignInRequested()
    req = m.signInScreen.signInRequest
    if req = invalid then return
    m.signInScreen.errorMessage = ""
    m.signInScreen.infoMessage = ""
    m.signInScreen.isBusy = true
    runAuthTask({
        action: "signIn"
        email: SafeStr(req.email)
        password: SafeStr(req.password)
    })
end sub

sub onResetPasswordRequested()
    email = SafeStr(m.signInScreen.resetPasswordRequest).Trim()
    if email = ""
        m.signInScreen.errorMessage = "Enter your Notflix email address above first, then choose Password Setup / Reset."
        return
    end if
    m.signInScreen.errorMessage = ""
    m.signInScreen.infoMessage = ""
    m.signInScreen.isBusy = true
    runAuthTask({
        action: "resetPassword"
        email: email
    })
end sub

sub onSignOutRequested()
    showLoading("Signing out...")
    m.session = invalid
    m.account = invalid
    m.profiles = []
    m.activeProfile = invalid
    m.catalog = []
    m.genres = []
    m.watchlist = []
    m.progressList = []
    m.ratings = []
    m.seriesCache = {}
    runAuthTask({ action: "signOut" })
end sub

sub onAuthResponse()
    res = m.authTask.response
    if res = invalid then return
    action = SafeStr(res.action)

    if action = "restore"
        if SafeBool(res.ok, false) and res.session <> invalid
            m.session = res.session
            m.account = res.account
            loadUserProfiles(true)
        else
            hideLoading()
            showScreen("signin")
        end if
    else if action = "signIn"
        m.signInScreen.isBusy = false
        if SafeBool(res.ok, false) and res.session <> invalid
            m.session = res.session
            m.account = res.account
            m.signInScreen.errorMessage = ""
            m.signInScreen.infoMessage = ""
            loadUserProfiles(true)
        else
            m.signInScreen.errorMessage = SafeStr(res.errorMessage, "Sign-in failed. Please try again.")
        end if
    else if action = "resetPassword"
        m.signInScreen.isBusy = false
        if SafeBool(res.ok, false)
            m.signInScreen.errorMessage = ""
            m.signInScreen.infoMessage = SafeStr(res.message, "Password setup email sent. Check your inbox.")
        else
            m.signInScreen.errorMessage = SafeStr(res.errorMessage, "Unable to send password setup email.")
        end if
    else if action = "signOut"
        hideLoading()
        m.signInScreen.errorMessage = ""
        m.signInScreen.infoMessage = "You have been signed out."
        showScreen("signin")
    end if
end sub

' =============================================================================
' Profiles & Home Data Loading
' =============================================================================
sub loadUserProfiles(allowAutoSelectSingle as boolean)
    m.allowAutoSelectSingle = allowAutoSelectSingle
    showLoading("Loading your Notflix profiles...")
    runFirestoreRead({
        action: "loadProfiles"
        session: m.session
    })
end sub

sub onProfileRetryRequested()
    if m.session <> invalid
        loadUserProfiles(false)
    else
        showScreen("signin")
    end if
end sub

sub onProfileSelected()
    prof = m.profileScreen.selectedProfile
    if prof = invalid then return
    selectActiveProfile(prof)
end sub

sub selectActiveProfile(prof as object)
    m.activeProfile = prof
    if m.session <> invalid
        RegistrySaveLastProfileId(m.session.uid, prof.id)
    end if
    m.homeScreen.profileName = SafeStr(prof.name, "Profile")
    m.homeScreen.isKidsProfile = SafeBool(prof.isKids, false)
    m.myListScreen.profileName = SafeStr(prof.name, "Profile")
    m.searchScreen.isKidsProfile = SafeBool(prof.isKids, false)

    showLoading("Loading Notflix for " + SafeStr(prof.name, "you") + "...")
    runFirestoreRead({
        action: "loadHomeData"
        session: m.session
        profileId: prof.id
        skipCatalog: (m.catalog.Count() > 0)
    })
end sub

sub onFirestoreReadResponse()
    res = m.firestoreReadTask.response
    if res = invalid then return

    if res.session <> invalid
        m.session = res.session
    end if

    if SafeBool(res.authExpired, false)
        hideLoading()
        showToast("Session expired. Please sign in again.", true)
        showScreen("signin")
        return
    end if

    action = SafeStr(res.action)

    if action = "loadProfiles"
        hideLoading()
        if not SafeBool(res.ok, false)
            m.profileScreen.accountEmail = SafeStr(m.session.email)
            m.profileScreen.profiles = []
            m.profileScreen.errorMessage = SafeStr(res.errorMessage, "Unable to load profiles. Check your connection and try again.")
            showScreen("profiles")
            return
        end if

        m.profiles = []
        if res.profiles <> invalid then m.profiles = res.profiles

        if m.profiles.Count() = 1 and m.allowAutoSelectSingle
            selectActiveProfile(m.profiles[0])
            return
        end if

        m.profileScreen.accountEmail = SafeStr(m.session.email)
        if m.profiles.Count() = 0
            m.profileScreen.errorMessage = "No profiles found for this account yet. Create a profile in Notflix for Android, then select Refresh Profiles."
        else
            m.profileScreen.errorMessage = ""
        end if
        m.profileScreen.profiles = m.profiles
        showScreen("profiles")

    else if action = "loadHomeData"
        hideLoading()
        if not SafeBool(res.ok, false)
            showToast(SafeStr(res.errorMessage, "Unable to load Notflix catalog."), true)
            if m.catalog.Count() = 0
                showScreen("profiles")
            else
                showScreen("home")
            end if
            return
        end if

        if res.catalog <> invalid then m.catalog = res.catalog
        if res.genres <> invalid then m.genres = res.genres
        m.watchlist = []
        if res.watchlist <> invalid then m.watchlist = res.watchlist
        m.progressList = []
        if res.progress <> invalid then m.progressList = res.progress
        m.ratings = []
        if res.ratings <> invalid then m.ratings = res.ratings

        refreshAllCatalogViews()
        showScreen("home")

        ' Open deep-linked content if Roku launched with a contentId
        if m.pendingDeepLinkId <> ""
            targetId = m.pendingDeepLinkId
            m.pendingDeepLinkId = ""
            for each cItem in m.catalog
                if cItem.id = targetId
                    openDetailsForItem(cItem)
                    exit for
                end if
            end for
        end if

    else if action = "loadSeriesDetails"
        cid = SafeStr(res.contentId)
        if SafeBool(res.ok, false)
            sd = {
                loading: false
                contentId: cid
                seasons: res.seasons
                episodesBySeason: res.episodesBySeason
                errorMessage: ""
            }
            m.seriesCache[cid] = sd
            if m.selectedDetailItem <> invalid and m.selectedDetailItem.id = cid
                m.detailsScreen.seriesData = sd
            end if
        else
            if m.selectedDetailItem <> invalid and m.selectedDetailItem.id = cid
                m.detailsScreen.seriesData = {
                    loading: false
                    contentId: cid
                    seasons: []
                    episodesBySeason: {}
                    errorMessage: SafeStr(res.errorMessage, "Could not load seasons and episodes.")
                }
            end if
        end if
    end if
end sub

sub onFirestoreWriteResponse()
    res = m.firestoreWriteTask.response
    if res = invalid then return
    if res.session <> invalid then m.session = res.session

    action = SafeStr(res.action)
    if action = "toggleWatchlist"
        if not SafeBool(res.ok, false)
            showToast("Could not sync My List change: " + SafeStr(res.errorMessage, "Network error"), true)
        end if
    else if action = "saveProgress"
        if not SafeBool(res.ok, false)
            ' Progress failed silently in background without interrupting video
            print "[Notflix] Progress sync warning: " + SafeStr(res.errorMessage)
        end if
    end if
end sub

sub refreshAllCatalogViews()
    feed = BuildNotflixHomeFeed(m.catalog, m.genres, m.activeProfile, m.watchlist, m.progressList, m.ratings)
    m.homeScreen.feedData = feed
    m.searchScreen.catalog = m.catalog
    m.searchScreen.isKidsProfile = (m.activeProfile <> invalid and SafeBool(m.activeProfile.isKids, false))
    m.myListScreen.items = buildResolvedMyListItems()
end sub

function buildResolvedMyListItems() as object
    isKids = (m.activeProfile <> invalid and SafeBool(m.activeProfile.isKids, false))
    catById = {}
    for each it in m.catalog
        if SafeBool(it.isVisible, true) and not SafeBool(it.isComingSoon, false)
            if (not isKids) or SafeBool(it.isKidsFriendly, false)
                catById[it.id] = it
            end if
        end if
    end for

    sortedWl = SortItemsByFieldDesc(m.watchlist, "addedAtEpoch")
    outArr = []
    for each wl in sortedWl
        if catById.DoesExist(wl.contentId)
            outArr.Push(catById[wl.contentId])
        end if
    end for
    return outArr
end function

function isItemInWatchlist(contentId as string) as boolean
    for each wl in m.watchlist
        if wl.contentId = contentId then return true
    end for
    return false
end function

' =============================================================================
' Details, Watchlist & Playback Flows
' =============================================================================
sub onContentItemSelected()
    chosen = invalid
    if m.activeScreen = "home"
        chosen = m.homeScreen.selectedItem
        m.returnTab = "home"
    else if m.activeScreen = "search"
        chosen = m.searchScreen.selectedItem
        m.returnTab = "search"
    else if m.activeScreen = "mylist"
        chosen = m.myListScreen.selectedItem
        m.returnTab = "mylist"
    end if
    if chosen = invalid then return
    openDetailsForItem(chosen)
end sub

sub openDetailsForItem(item as object)
    m.selectedDetailItem = item
    m.detailsScreen.progressList = m.progressList
    m.detailsScreen.inWatchlist = isItemInWatchlist(item.id)
    m.detailsScreen.item = item

    if item.mediaType = "series"
        if m.seriesCache.DoesExist(item.id)
            m.detailsScreen.seriesData = m.seriesCache[item.id]
        else
            m.detailsScreen.seriesData = { loading: true, contentId: item.id, seasons: [], episodesBySeason: {}, errorMessage: "" }
            runFirestoreRead({
                action: "loadSeriesDetails"
                session: m.session
                contentId: item.id
            })
        end if
    end if

    showScreen("details")
end sub

sub onToggleWatchlistRequested()
    req = m.detailsScreen.toggleWatchlistRequest
    if req = invalid or req.item = invalid or m.activeProfile = invalid then return

    item = req.item
    addToList = SafeBool(req.addToList, true)

    ' Optimistic local update for crisp TV UX
    newWl = []
    for each wl in m.watchlist
        if wl.contentId <> item.id
            newWl.Push(wl)
        end if
    end for
    if addToList
        dt = CreateObject("roDateTime")
        newWl.Push({
            contentId: item.id
            mediaType: item.mediaType
            title: item.title
            addedAtEpoch: dt.AsSeconds()
        })
        showToast("Added " + Chr(34) + item.title + Chr(34) + " to My List", false)
    else
        showToast("Removed " + Chr(34) + item.title + Chr(34) + " from My List", false)
    end if

    m.watchlist = newWl
    m.detailsScreen.inWatchlist = addToList
    refreshAllCatalogViews()

    runFirestoreWrite({
        action: "toggleWatchlist"
        session: m.session
        profileId: m.activeProfile.id
        item: item
        addToList: addToList
    })
end sub

sub onDetailsBackRequested()
    showScreen(m.returnTab)
end sub

sub onDetailsPlayRequested()
    req = m.detailsScreen.playRequest
    if req = invalid then return
    showScreen("player")
    m.playerScreen.playRequest = req
end sub

sub onPlayerSaveProgress()
    prog = m.playerScreen.saveProgressEvent
    if prog = invalid or m.activeProfile = invalid then return

    ' Update local progress list immediately so Continue Watching / Details update on exit
    cid = SafeStr(prog.contentId)
    epId = SafeStr(prog.episodeId, "")
    pKey = cid
    if epId <> "" then pKey = cid + "_" + epId

    dt = CreateObject("roDateTime")
    posMs = SafeNum(prog.positionMs, 0#)
    durMs = SafeNum(prog.durationMs, 0#)
    frac = 0.0
    if durMs > 0#
        frac = posMs / durMs
        if frac > 1.0 then frac = 1.0
    end if

    updatedRec = {
        id: pKey
        contentId: cid
        episodeId: epId
        seasonNumber: SafeInt(prog.seasonNumber, 0)
        episodeNumber: SafeInt(prog.episodeNumber, 0)
        title: SafeStr(prog.title)
        positionMs: posMs
        durationMs: durMs
        completed: SafeBool(prog.completed, false)
        updatedAtEpoch: dt.AsSeconds()
        progressFraction: frac
    }

    nextList = []
    for each existing in m.progressList
        exKey = existing.contentId
        if SafeStr(existing.episodeId) <> "" then exKey = existing.contentId + "_" + existing.episodeId
        if exKey <> pKey
            nextList.Push(existing)
        end if
    end for
    nextList.Push(updatedRec)
    m.progressList = nextList

    ' Persist to Firestore in background
    runFirestoreWrite({
        action: "saveProgress"
        session: m.session
        profileId: m.activeProfile.id
        progress: prog
    })
end sub

sub onPlayerNextEpisode()
    ev = m.playerScreen.playNextEpisodeEvent
    if ev = invalid or ev.item = invalid or ev.episode = invalid then return

    nextAfter = findNextSeriesEpisode(ev.item.id, ev.episode.id)
    nextEpObj = invalid
    nextSn = 0
    if nextAfter <> invalid
        nextEpObj = nextAfter.episode
        nextSn = nextAfter.seasonNumber
    end if

    src = ResolveRokuPlayableSource(ev.episode)
    m.playerScreen.playRequest = {
        item: ev.item
        episode: ev.episode
        seasonNumber: SafeInt(ev.seasonNumber, 1)
        episodeNumber: ev.episode.episodeNumber
        resumeSec: 0
        sourceInfo: src
        nextEpisode: nextEpObj
        nextSeasonNumber: nextSn
    }
end sub

function findNextSeriesEpisode(contentId as string, currentEpisodeId as string) as dynamic
    if not m.seriesCache.DoesExist(contentId) then return invalid
    sd = m.seriesCache[contentId]
    if sd = invalid or sd.seasons = invalid then return invalid
    found = false
    for each season in sd.seasons
        eps = sd.episodesBySeason[season.id]
        if eps <> invalid
            for each ep in eps
                if found
                    return { episode: ep, seasonNumber: season.seasonNumber }
                end if
                if ep.id = currentEpisodeId then found = true
            end for
        end if
    end for
    return invalid
end function

sub onPlayerClosed()
    refreshAllCatalogViews()
    if m.selectedDetailItem <> invalid
        m.detailsScreen.progressList = m.progressList
        m.detailsScreen.inWatchlist = isItemInWatchlist(m.selectedDetailItem.id)
    end if
    showScreen("details")
end sub

' =============================================================================
' Screen Switching & Left TV Navigation Rail
' =============================================================================
sub showScreen(target as string)
    m.activeScreen = target
    m.navExpanded = false

    m.signInScreen.visible = (target = "signin")
    m.profileScreen.visible = (target = "profiles")
    m.homeScreen.visible = (target = "home")
    m.searchScreen.visible = (target = "search")
    m.myListScreen.visible = (target = "mylist")
    m.detailsScreen.visible = (target = "details")
    m.playerScreen.visible = (target = "player")

    showRail = (target = "home" or target = "search" or target = "mylist")
    m.navRailContainer.visible = showRail
    if showRail
        if target = "home" then m.navFocusedIdx = 0
        if target = "search" then m.navFocusedIdx = 1
        if target = "mylist" then m.navFocusedIdx = 2
        renderNavRail()
    end if

    if target = "signin"
        m.signInScreen.focusRequested = true
    else if target = "profiles"
        m.profileScreen.focusRequested = true
    else if target = "home"
        m.homeScreen.focusRequested = true
    else if target = "search"
        m.searchScreen.focusRequested = true
    else if target = "mylist"
        m.myListScreen.focusRequested = true
    else if target = "details"
        m.detailsScreen.focusRequested = true
    end if
end sub

sub onRequestNavFocus()
    if m.activeScreen = "home" or m.activeScreen = "search" or m.activeScreen = "mylist"
        m.navExpanded = true
        if m.activeScreen = "home" then m.navFocusedIdx = 0
        if m.activeScreen = "search" then m.navFocusedIdx = 1
        if m.activeScreen = "mylist" then m.navFocusedIdx = 2
        renderNavRail()
        m.top.setFocus(true)
    end if
end sub

sub collapseNavRailToActiveScreen()
    m.navExpanded = false
    renderNavRail()
    if m.activeScreen = "home"
        m.homeScreen.focusRequested = true
    else if m.activeScreen = "search"
        m.searchScreen.focusRequested = true
    else if m.activeScreen = "mylist"
        m.myListScreen.focusRequested = true
    end if
end sub

sub renderNavRail()
    if m.navExpanded
        m.navScrim.visible = true
        m.navBg.width = 340
        m.navDivider.translation = [340, 0]
        m.navBrandLabel.width = 340
        m.navBrandLabel.text = "NOTFLIX"
        m.navPrivacyFooter.visible = true
    else
        m.navScrim.visible = false
        m.navBg.width = 104
        m.navDivider.translation = [104, 0]
        m.navBrandLabel.width = 104
        m.navBrandLabel.text = "N"
        m.navPrivacyFooter.visible = false
    end if

    for i = 0 to 4
        isCurTab = (m.navActions[i] = m.activeScreen)
        isFocused = (m.navExpanded and m.navFocusedIdx = i)

        m.navBgNodes[i].width = 76
        if m.navExpanded then m.navBgNodes[i].width = 312

        m.navShortNodes[i].visible = (not m.navExpanded)
        m.navFullNodes[i].visible = m.navExpanded

        if isFocused
            m.navBgNodes[i].color = "0xE50914FF"
            m.navBarNodes[i].color = "0xFFFFFFFF"
            m.navFullNodes[i].color = "0xFFFFFFFF"
            m.navShortNodes[i].color = "0xFFFFFFFF"
        else if isCurTab
            m.navBgNodes[i].color = "0x1E1E2CFF"
            m.navBarNodes[i].color = "0xE50914FF"
            m.navFullNodes[i].color = "0xF5F5F7FF"
            m.navShortNodes[i].color = "0xF5F5F7FF"
        else
            m.navBgNodes[i].color = "0x12121AFF"
            m.navBarNodes[i].color = "0x00000000"
            m.navFullNodes[i].color = "0xB8BBC8FF"
            m.navShortNodes[i].color = "0xA0A3B1FF"
        end if
    end for
end sub

sub activateNavAction(idx as integer)
    if idx < 0 or idx >= m.navActions.Count() then return
    act = m.navActions[idx]
    if act = "home"
        showScreen("home")
    else if act = "search"
        showScreen("search")
    else if act = "mylist"
        showScreen("mylist")
    else if act = "profiles"
        m.navExpanded = false
        loadUserProfiles(false)
    else if act = "signout"
        m.navExpanded = false
        onSignOutRequested()
    end if
end sub

' =============================================================================
' Overlays & Exit Confirmation Dialog
' =============================================================================
sub showLoading(msg as string)
    m.loadingText.text = msg
    m.loadingOverlay.visible = true
end sub

sub hideLoading()
    m.loadingOverlay.visible = false
end sub

sub showToast(msg as string, isError as boolean)
    m.toastLabel.text = msg
    if isError
        m.toastAccent.color = "0xFF5370FF"
    else
        m.toastAccent.color = "0x22C55EFF"
    end if
    m.toastBanner.visible = true
    m.toastTimer.control = "stop"
    m.toastTimer.control = "start"
end sub

sub onToastTimerFire()
    m.toastBanner.visible = false
end sub

sub showExitDialog()
    dlg = CreateObject("roSGNode", "Dialog")
    dlg.title = "Exit Notflix?"
    dlg.message = "Do you want to exit Notflix and return to the Roku Home screen?"
    dlg.buttons = ["Cancel", "Exit Notflix"]
    dlg.observeField("buttonSelected", "onExitDialogButton")
    m.top.dialog = dlg
end sub

sub onExitDialogButton()
    dlg = m.top.dialog
    if dlg = invalid then return
    idx = dlg.buttonSelected
    m.top.dialog = invalid
    if idx = 1
        m.top.exitChannel = true
    else
        m.homeScreen.focusRequested = true
    end if
end sub

' =============================================================================
' Top-Level Remote Key Handling
' =============================================================================
function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false

    ' When Left Navigation Rail is expanded, Scene handles its D-pad navigation
    if m.navExpanded
        if key = "up"
            if m.navFocusedIdx > 0
                m.navFocusedIdx = m.navFocusedIdx - 1
                renderNavRail()
            end if
            return true
        else if key = "down"
            if m.navFocusedIdx < 4
                m.navFocusedIdx = m.navFocusedIdx + 1
                renderNavRail()
            end if
            return true
        else if key = "OK"
            activateNavAction(m.navFocusedIdx)
            return true
        else if key = "right" or key = "back"
            collapseNavRailToActiveScreen()
            return true
        end if
        return true
    end if

    ' Back key handling for top-level screens
    if key = "back"
        if m.activeScreen = "search" or m.activeScreen = "mylist"
            showScreen("home")
            return true
        else if m.activeScreen = "home"
            showExitDialog()
            return true
        else if m.activeScreen = "profiles"
            if m.activeProfile <> invalid and m.catalog.Count() > 0
                showScreen("home")
                return true
            end if
            showExitDialog()
            return true
        else if m.activeScreen = "signin"
            ' Allow Roku to exit cleanly from Sign-In screen
            return false
        end if
    end if

    return false
end function
