# Wishes App

A private Android app for two people to share photo wishes, with a home-screen widget.

## Features
- Sign in with email (magic link — no password)
- Create a private space, share the 6-character invite code with your partner
- Add wishes: text + photo (stored as compressed JPEG in Firestore — no Storage billing needed)
- Home-screen **Glance widget**: shows the latest wish (updates instantly while the app is open; background refresh every 30 min) or a random wish (rotates every 30 min, tap to shuffle)
- Gifting-ready: any other couple installs the same APK and creates their own private space

---

## One-time setup (do this before first build)

### 1. Create a Firebase project

1. Go to [console.firebase.google.com](https://console.firebase.google.com) → **Add project**
2. Enable **Authentication** → Sign-in method → **Email link (passwordless)**
3. Enable **Firestore Database** → Start in **production mode**
4. Enable **Cloud Messaging** (it's on by default)
5. Register an Android app:
   - Package name: `com.wishesapp`
   - Download `google-services.json` → place it at `app/google-services.json`
6. Deploy Firestore security rules:
   ```
   firebase deploy --only firestore:rules
   ```
   (or paste `firestore.rules` content in the Firebase Console → Firestore → Rules)

### 2. Configure Firebase Dynamic Links (for email sign-in link)

In Firebase Console → Dynamic Links:
- Create a URL prefix: `wishesapp.page.link`
- Or update `MainActivity.kt` and `AuthRepository.kt` with your own domain

### 3. Generate a release keystore (do once, keep forever)

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias wishes_key \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Copy `release.keystore` to the `app/` directory, then create `keystore.properties` in the project root:

```properties
storeFile=release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=wishes_key
keyPassword=YOUR_KEY_PASSWORD
```

**Keep these files safe — losing the keystore means users can't update the app.**

### 4. Build a release APK locally

The `gradle-wrapper.jar` is not committed (binary file). Generate and commit it once:

```bash
# Requires Gradle 8.9 installed on your machine:
#   brew install gradle   (macOS)
#   sdk install gradle 8.9  (SDKMAN)
gradle wrapper --gradle-version=8.9

# Commit the generated jar (it's needed for CI and other developers)
git add gradle/wrapper/gradle-wrapper.jar
git commit -m "chore: add gradle wrapper jar"

# Then build:
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## GitHub distribution setup

### 1. Push to GitHub

Create a public repo (private also works), push all files **except** the secrets listed in `.gitignore`.

### 2. Enable GitHub Pages

Repo → Settings → Pages → Source: **Deploy from branch** → branch `main`, folder `/docs`

Your download page will be at: `https://YOUR_USERNAME.github.io/WishesApp/`

Also update `UpdateChecker.kt` with your real username so in-app update checks work:
```kotlin
// app/src/main/java/com/wishesapp/ui/update/UpdateChecker.kt
private val versionJsonUrl = "https://YOUR_USERNAME.github.io/WishesApp/version.json"
//  ↑ replace YOUR_USERNAME
```

### 3. Add GitHub Secrets (Settings → Secrets → Actions)

| Secret name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | `base64 app/google-services.json` |
| `KEYSTORE_B64` | `base64 app/release.keystore` |
| `KEYSTORE_PASSWORD` | your store password |
| `KEY_ALIAS` | `wishes_key` |
| `KEY_PASSWORD` | your key password |

On Linux/macOS: `base64 -i app/google-services.json | tr -d '\n'`
On Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\google-services.json"))`

### 4. Release a new version

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`
2. Commit and push
3. Create a git tag: `git tag v1.0.0 && git push origin v1.0.0`
4. GitHub Actions builds the APK, creates a Release, and updates `docs/version.json`
5. Users visiting the download page automatically get the new version link

---

## Architecture overview

```
app/
├── data/
│   ├── model/          Wish, Space, User, WidgetState
│   └── repository/     AuthRepository, SpaceRepository, WishRepository,
│                       WidgetPrefsRepository, ImageUtil
├── di/                 FirebaseModule (Hilt)
├── fcm/                WishesMessagingService (FCM → WorkManager)
├── ui/
│   ├── auth/           AuthScreen + AuthViewModel
│   ├── space/          SpaceSetupScreen + SpaceViewModel
│   ├── wishes/         WishListScreen, AddWishScreen, WishDetailScreen,
│   │                   SettingsScreen, WishViewModel
│   └── theme/          Material3 pink theme
├── widget/             WishWidget (Glance), WishWidgetWorker, ShuffleWishAction
├── MainActivity.kt     Edge-to-edge, handles email deep link
└── WishesApp.kt        Hilt + WorkManager init
```

## Firestore data model

```
users/{uid}
  email, displayName, spaceId, fcmToken

spaces/{spaceId}
  inviteCode, createdBy, members: {uid → name}

  wishes/{wishId}
    authorId, authorName, text, imageSmall (base64), imageFull (base64),
    createdAt, seenBy: [uid]

notifications/{notifId}   ← reserved for optional Cloud Function FCM push upgrade
  to (uid), token, spaceId, wishId, authorName, text, sentAt
```

## Widget behaviour

| Mode | How it updates | Battery cost |
|---|---|---|
| **Latest** | Instantly while app is open (Firestore listener → WorkManager expedited); every 30 min in background | Very low |
| **Random** | Auto every 30 min (WorkManager periodic) + tap to shuffle immediately | Low |

The widget image is downloaded once and cached as a file on disk. Only a file path (not a bitmap) crosses the Binder transaction boundary, avoiding `TransactionTooLargeException`.
