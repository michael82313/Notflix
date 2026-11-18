# Notflix for Roku TV

A standalone Roku TV (BrightScript + SceneGraph) streaming channel connected directly to the live **Notflix** Firebase backend (`gen-lang-client-0851231801`).

> **Always Free  •  Ad-Free  •  We Never Sell Your Personal Information**  
> Zero ads, zero tracking SDKs, zero admin secrets, and zero modifications to the Android app or Firestore data.

---

## Quick Sideload Guide (Install `Notflix_Roku.zip` on Your Roku TV)

### Step 1: Enable Developer Mode on Your Roku TV
1. Turn on your Roku TV and go to the Roku **Home** screen.
2. On your Roku remote, press this exact button sequence:
   - **Home** (3 times)
   - **Up** (2 times)
   - **Right**, **Left**, **Right**, **Left**, **Right**
3. The **Developer Settings** screen will open. Note the **URL / IP address** shown on screen (for example, `http://192.168.1.42`).
4. Select **Enable installer and restart** and accept the Developer Tools License Agreement.
5. When prompted, create a **Web Server Password** (remember this password; username is `rokudev`).
6. Wait for your Roku TV to reboot.

### Step 2: Upload & Install `Notflix_Roku.zip`
1. Make sure your computer or phone is on the **same Wi-Fi network** as your Roku TV.
2. Open a web browser and go to the Roku IP address from Step 1 (e.g., `http://192.168.1.42`).
3. Sign in when prompted:
   - **Username:** `rokudev`
   - **Password:** the password you set in Step 1.
4. In the **Development Application Installer**:
   - Click **Upload** (or **Choose File**) and select **`Notflix_Roku.zip`**.
   - Click **Install** (or **Replace** if updating an earlier sideloaded build).
5. Notflix will install and launch automatically on your Roku TV!

---

## Signing In on Roku TV

- **Existing Notflix Email & Password Users:** Enter your Notflix email and password using the Roku on-screen keyboard and select **SIGN IN**. Your session is persisted in Roku's local channel registry and refreshes automatically.
- **Users Who Originally Signed Up with Google Sign-In on Android:**  
  Roku OS does not ship Google Play Services / Android Credential Manager. To sign into your **exact existing Notflix account** (same Firebase UID, profiles, My List, and Continue Watching):
  1. Enter your Google account email in the **Email Address** field on Roku.
  2. Select **Google Sign-In User? Send Password Setup / Reset Email**.
  3. Open the Firebase email link on your phone/computer and choose a password.
  4. Return to Roku and sign in with that email and password — Firebase links it to the exact same UID without creating a duplicate account.

---

## Remote Control Guide

| Button | Behavior |
|---|---|
| **Up / Down / Left / Right** | Navigate rows, posters, episodes, and buttons |
| **OK** | Select title / Play / Toggle My List / Open keyboard |
| **Left (at start of row)** | Open the Left TV Navigation Rail (`Home`, `Search`, `My List`, `Switch Profile`, `Sign Out`) |
| **Back** | Close player (saves progress) → Details → Home → Exit confirmation dialog |
| **Play/Pause, Rew, FF, \*** | Native Roku trick-play bar, scrubbing, and captions/audio menu during playback |

---

## Video Stream Compatibility on Roku TV

Notflix for Roku TV uses Roku's native hardware-accelerated `Video` player:

| Source Format | Roku Support | Notes |
|---|---|---|
| **Direct HTTPS MP4 / M4V / MOV / MKV** | Supported | Including Firebase Storage direct uploads |
| **HLS (`.m3u8`)** | Supported | Preferred when both `hlsUrl` and `videoUrl` exist |
| **MPEG-DASH (`.mpd`) & Smooth Streaming (`.ism`)** | Supported | Native Roku streaming formats |
| **YouTube / Vimeo / Dailymotion / Twitch / Web Embeds** | Not playable natively | Roku OS has no WebView/IFrame player; Notflix clearly explains this instead of crashing or scraping |
| **Insecure `http://` or HTML webpages** | Rejected safely | Prompts admin to configure an `https://` MP4 or HLS stream |

---

## Project Structure

```
roku-notflix/
├── manifest                        # Roku channel manifest (FHD 1920x1080)
├── source/
│   ├── Main.brs                    # Entrypoint + roSGScreen event loop
│   ├── Config.brs                  # Firebase project, named DB, REST URLs, palette
│   ├── FirebaseUtil.brs            # HTTPS JSON client, Firestore parser/mappers, registry
│   ├── VideoSourceUtil.brs         # Roku stream classifier & completion rules
│   └── HomeRowBuilder.brs          # Personalized dynamic row engine
├── components/
│   ├── MainScene.xml / .brs        # Root router, Left TV Nav Rail, task orchestration
│   ├── SignInScreen.xml / .brs     # TV email/password + password setup screen
│   ├── ProfileScreen.xml / .brs    # "Who's watching?" profile selector
│   ├── HomeScreen.xml / .brs       # Hero billboard + dynamic RowList
│   ├── DetailsScreen.xml / .brs    # Movie & TV details, seasons, episodes, My List
│   ├── SearchScreen.xml / .brs     # MiniKeyboard + real-time filtered catalog grid
│   ├── MyListScreen.xml / .brs     # Synced per-profile watchlist grid
│   ├── PlayerScreen.xml / .brs     # Native Roku Video player, resume, sync, Up Next
│   ├── PosterItem.xml / .brs       # Poster card with focus frame & progress bar
│   ├── EpisodeItem.xml / .brs      # Episode row item with progress & compatibility status
│   ├── ProfileItem.xml / .brs      # Profile avatar tile
│   └── tasks/
│       ├── AuthTask.xml / .brs     # Firebase Auth REST task (restore, sign-in, reset, sign-out)
│       └── FirestoreTask.xml / .brs# Firestore REST task (:runQuery, :commit, 401 refresh)
└── images/                         # Channel posters, FHD/HD/SD splash art, hero & fallbacks
```

---

## Rebuilding `Notflix_Roku.zip`

If you edit files inside `roku-notflix/`, repackage the ZIP from inside `roku-notflix/` so `manifest` stays at the ZIP root:

```bash
cd roku-notflix
zip -r ../Notflix_Roku.zip manifest source components images
```
