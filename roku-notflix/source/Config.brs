' =============================================================================
' Notflix for Roku TV — Shared Firebase Backend & Brand Configuration
' Connects directly to the existing Notflix Firebase project & Firestore DB.
' Uses ONLY the public Firebase client API key (never admin or service keys).
' =============================================================================

function NotflixConfig() as object
    projectId = "gen-lang-client-0851231801"
    databaseId = "ai-studio-android-velora-fa049f50-d743-4c93-8b72-1a2b2b3ed613"
    apiKey = "AIzaSyCxWGmZcuJPfJ0pEM4e41jAv2iazLu5yHc"
    docsBase = "https://firestore.googleapis.com/v1/projects/" + projectId + "/databases/" + databaseId + "/documents"

    return {
        appName: "Notflix"
        projectId: projectId
        databaseId: databaseId
        apiKey: apiKey
        storageBucket: "gen-lang-client-0851231801.firebasestorage.app"

        ' Firebase Auth REST API endpoints
        authSignInUrl: "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey
        authResetPasswordUrl: "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + apiKey
        authRefreshTokenUrl: "https://securetoken.googleapis.com/v1/token?key=" + apiKey

        ' Firestore REST API v1 endpoints
        firestoreDocsBase: docsBase
        firestoreRunQueryUrl: docsBase + ":runQuery"
        firestoreCommitUrl: docsBase + ":commit"
        firestoreDocPrefix: "projects/" + projectId + "/databases/" + databaseId + "/documents/"

        ' Local Roku registry persistence (per-channel isolated storage)
        registrySection: "NotflixSessionV1"

        ' Privacy & Product Promise
        privacyPromise: "Always Free  •  Ad-Free  •  We Never Sell Your Personal Information"

        ' Brand Palette (matches Notflix Android Theme)
        colors: {
            background: "0x080808FF"
            surface: "0x121217FF"
            surfaceElevated: "0x1A1A22FF"
            surfaceBright: "0x252530FF"
            crimson: "0xE50914FF"
            crimsonDark: "0x9E000AFF"
            gold: "0xF59E0BFF"
            matchGreen: "0x22C55EFF"
            kidsCyan: "0x38BDF8FF"
            textPrimary: "0xF5F5F7FF"
            textSecondary: "0xEAEAF0FF"
            textMuted: "0xA0A3B1FF"
            outline: "0x2E2E3AFF"
            errorRed: "0xFF5370FF"
        }

        ' Playback completion threshold (matches Android CompletionTracker.kt)
        completionThreshold: 0.90
        minCompletionDurationMs: 20000
        minWatchedPositionMs: 10000
        progressSaveIntervalSec: 15
    }
end function
