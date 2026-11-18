' =============================================================================
' Notflix for Roku TV — Firebase Authentication Background Task
' Handles session restore/refresh, Email/Password sign-in, Password Reset, and
' Sign Out via Firebase Auth REST APIs without blocking the TV UI thread.
' =============================================================================

sub init()
    m.top.functionName = "executeAuthTask"
end sub

sub executeAuthTask()
    req = m.top.request
    if req = invalid or req.action = invalid
        m.top.response = { ok: false, action: "", errorMessage: "Missing auth request." }
        return
    end if

    action = req.action
    if action = "restore"
        handleRestoreSession()
    else if action = "signIn"
        handleSignIn(SafeStr(req.email).Trim(), SafeStr(req.password))
    else if action = "resetPassword"
        handleResetPassword(SafeStr(req.email).Trim())
    else if action = "signOut"
        RegistryClearSession()
        m.top.response = { ok: true, action: "signOut", message: "Signed out." }
    else
        m.top.response = { ok: false, action: action, errorMessage: "Unknown auth action." }
    end if
end sub

sub handleRestoreSession()
    saved = RegistryLoadSession()
    if saved = invalid
        print "[NotflixAuth] handleRestoreSession: No saved session found."
        m.top.response = { ok: false, action: "restore", needsSignIn: true, errorMessage: "" }
        return
    end if

    print "[NotflixAuth] handleRestoreSession: Restoring session for uid: "; saved.uid
    refreshed = RefreshFirebaseSession(saved.refreshToken, saved.email)
    if not refreshed.ok
        print "[NotflixAuth] handleRestoreSession: Refresh failed with statusCode: "; refreshed.statusCode
        if refreshed.statusCode = 400 or refreshed.statusCode = 401 or refreshed.statusCode = 403
            RegistryClearSession()
            m.top.response = { ok: false, action: "restore", needsSignIn: true, errorMessage: "Session expired. Please sign in again." }
        else
            ' Transient network/offline error — keep saved session so user can retry when online
            m.top.response = { ok: false, action: "restore", needsSignIn: false, networkError: true, errorMessage: refreshed.errorMessage }
        end if
        return
    end if

    accountRes = FetchUserAccountDoc(refreshed.session.uid, refreshed.session.idToken)
    if accountRes.account <> invalid and accountRes.account.isSuspended
        RegistryClearSession()
        m.top.response = { ok: false, action: "restore", needsSignIn: true, errorMessage: "This Notflix account is currently suspended." }
        return
    end if

    print "[NotflixAuth] handleRestoreSession: Session restored and refreshed successfully."
    m.top.response = {
        ok: true
        action: "restore"
        session: refreshed.session
        account: accountRes.account
    }
end sub

sub handleSignIn(email as string, password as string)
    print "[NotflixAuth] handleSignIn: Starting sign in for email: "; email
    if email = "" or Instr(1, email, "@") <= 1 or Instr(1, email, ".") <= 1
        m.top.response = { ok: false, action: "signIn", errorMessage: "Please enter a valid email address." }
        return
    end if
    if Len(password) < 6
        m.top.response = { ok: false, action: "signIn", errorMessage: "Password must be at least 6 characters." }
        return
    end if

    cfg = NotflixConfig()
    body = {
        email: email
        password: password
        returnSecureToken: true
    }

    res = HttpJsonRequest("POST", cfg.authSignInUrl, body, "", 15000)
    if not res.ok or res.data = invalid
        print "[NotflixAuth] handleSignIn: HTTP request failed: "; res.errorMessage
        m.top.response = { ok: false, action: "signIn", errorMessage: res.errorMessage }
        return
    end if

    uid = SafeStr(res.data.localId)
    idToken = SafeStr(res.data.idToken)
    refreshToken = SafeStr(res.data.refreshToken)
    resolvedEmail = SafeStr(res.data.email, email)

    print "[NotflixAuth] handleSignIn: Response received. uid present: "; (uid <> ""); ", idToken present: "; (idToken <> "")

    if uid = "" or idToken = ""
        m.top.response = { ok: false, action: "signIn", errorMessage: "Authentication succeeded but session token was missing." }
        return
    end if

    session = {
        uid: uid
        email: resolvedEmail
        idToken: idToken
        refreshToken: refreshToken
    }

    accountRes = FetchUserAccountDoc(uid, idToken)
    if accountRes.account <> invalid and accountRes.account.isSuspended
        m.top.response = { ok: false, action: "signIn", errorMessage: "This Notflix account is currently suspended." }
        return
    end if

    RegistrySaveSession(session)
    print "[NotflixAuth] handleSignIn: Session saved successfully."

    m.top.response = {
        ok: true
        action: "signIn"
        session: session
        account: accountRes.account
    }
end sub

sub handleResetPassword(email as string)
    if email = "" or Instr(1, email, "@") <= 1 or Instr(1, email, ".") <= 1
        m.top.response = { ok: false, action: "resetPassword", errorMessage: "Enter your Notflix email address first, then select Set / Reset Password." }
        return
    end if

    cfg = NotflixConfig()
    body = {
        requestType: "PASSWORD_RESET"
        email: email
    }
    res = HttpJsonRequest("POST", cfg.authResetPasswordUrl, body, "", 15000)
    if not res.ok
        m.top.response = { ok: false, action: "resetPassword", errorMessage: res.errorMessage }
        return
    end if

    m.top.response = {
        ok: true
        action: "resetPassword"
        message: "Password link sent to " + email + ". Set your password from that email, then sign in here using your existing Notflix account."
    }
end sub

function RefreshFirebaseSession(refreshToken as string, fallbackEmail as string) as object
    cfg = NotflixConfig()
    params = {
        grant_type: "refresh_token"
        refresh_token: refreshToken
    }
    print "[NotflixAuth] RefreshFirebaseSession: Requesting token refresh..."
    res = HttpUrlEncodedPost(cfg.authRefreshTokenUrl, params, 15000)
    if not res.ok or res.data = invalid
        print "[NotflixAuth] RefreshFirebaseSession: HTTP form post failed with status: "; res.statusCode
        return { ok: false, statusCode: res.statusCode, errorMessage: res.errorMessage }
    end if

    ' Firebase Secure Token API returns access_token (ID token) and refresh_token
    idToken = SafeStr(res.data.access_token)
    if idToken = ""
        idToken = SafeStr(res.data.id_token)
    end if
    newRefresh = SafeStr(res.data.refresh_token, refreshToken)
    uid = SafeStr(res.data.user_id)
    if uid = ""
        uid = SafeStr(res.data.project_id)
    end if

    if idToken = ""
        print "[NotflixAuth] RefreshFirebaseSession: access_token missing in response."
        return { ok: false, statusCode: 401, errorMessage: "Unable to refresh Firebase session token." }
    end if

    if uid = ""
        saved = RegistryLoadSession()
        if saved <> invalid then uid = saved.uid
    end if

    session = {
        uid: uid
        email: fallbackEmail
        idToken: idToken
        refreshToken: newRefresh
    }
    RegistrySaveSession(session)
    print "[NotflixAuth] RefreshFirebaseSession: Token refreshed successfully for uid: "; uid
    return { ok: true, statusCode: 200, session: session }
end function

function FetchUserAccountDoc(uid as string, idToken as string) as object
    cfg = NotflixConfig()
    url = cfg.firestoreDocsBase + "/users/" + uid
    res = HttpJsonRequest("GET", url, invalid, idToken, 12000)
    if res.ok and res.data <> invalid
        parsed = ParseFirestoreDocument(res.data)
        return { ok: true, account: MapUserAccount(parsed) }
    end if
    ' If user document does not exist yet in Firestore, return a valid default viewer account so sign-in proceeds smoothly
    return {
        ok: true
        account: {
            id: uid
            userId: uid
            email: ""
            displayName: "Notflix Viewer"
            role: "viewer"
            publisherStatus: "none"
            isSuspended: false
            autoplayNext: true
            subtitlesEnabled: true
        }
    }
end function
