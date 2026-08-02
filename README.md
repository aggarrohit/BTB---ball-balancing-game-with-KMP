# Balance The Ball

A multiplayer, sensor-based mobile game built with Kotlin Multiplatform. Tilt your phone to keep a
ball balanced at the center of a table, walk to make progress, and race friends to be the first to
reach the target step count.

## How to Play

1. Sign in with Google.
2. Pick a username (unique, one-time).
3. From the Lobby, either **create a room** (choose player count, target steps to win, and a
   balance threshold) or **join a room** with a 4-digit code.
4. Once the room fills up, the round starts automatically for everyone.
5. Tilt your phone to keep the ball near the center of the table — the current distance from
   center is shown as a percentage.
6. Steps only count toward your progress while the ball stays within the room's balance
   threshold. Walk to rack up valid steps.
7. Tilt too far and the ball falls off the table — you're eliminated for the round, but can keep
   watching everyone else's progress live.
8. First player to reach the target step count wins. If everyone falls off before that happens,
   the round ends with no winner.
9. **Play Again** requires everyone still in the room to agree: the requester's tap sends every
   other player a Play/Deny prompt. Deny and you leave the room; the rest can still play on.

## Features

- Google Sign-In (Firebase Auth) with persistent sessions across app restarts
- Real-time multiplayer rooms via Firebase Realtime Database — 1 to 10 players (1 doubles as solo
  practice)
- Live tilt + step-count sensors on both Android and iOS
- Per-room configurable win condition (target steps) and difficulty (balance threshold %)
- Live opponent progress, elimination state, and win/no-winner end states
- Play-again consent flow with graceful player removal on decline
- Background-aware gameplay — sensors pause automatically when the app leaves the foreground
- System back-button handling (confirms before exiting a game or the app)

## Tech Stack

- **Kotlin Multiplatform** (Android + iOS) with **Compose Multiplatform** for a fully shared UI —
  no per-platform UI code
- Clean-ish layering (`domain` / `data` / `presentation`), with `expect`/`actual` for
  platform-specific sensor and OS integration
- **Firebase Realtime Database** + **Firebase Auth**, via the [gitlive](https://github.com/GitLiveApp/firebase-kotlin-sdk)
  multiplatform Firebase SDK, enforced by Realtime Database security rules (see
  `database.rules.json`) — no backend server
- Sensors: Android `SensorManager` (rotation vector for tilt, step detector for steps); iOS
  CoreMotion (device motion + `CMPedometer`)
- Google Sign-In: Android via Credential Manager; iOS via the GoogleSignIn-iOS SDK, bridged into
  Kotlin through a small Swift-implemented interface (no CocoaPods in this project)
- GitHub Actions CI — build + unit tests on every push/PR

## Project Structure

- `/androidApp` — Android application module (entry point, manifest, launcher icon)
- `/iosApp` — iOS Xcode project (entry point, Swift bridge for Google Sign-In)
- `/shared` — all shared Kotlin code
  - `commonMain` — shared UI (Compose) and business logic (`domain`, `data`, `presentation`)
  - `androidMain` / `iosMain` — platform-specific implementations (sensors, auth, config)
- `database.rules.json` — Firebase Realtime Database security rules
- `.github/workflows/android-ci.yml` — CI: build + test on every push/PR

## Setup

### 1. Firebase project

Create a Firebase project with:
- **Realtime Database** enabled — publish the rules from `database.rules.json` under
  Realtime Database → Rules
- **Authentication** → Sign-in method → **Google** provider enabled

Download and place (both gitignored — never commit them):
- `androidApp/google-services.json`
- `iosApp/iosApp/GoogleService-Info.plist`

### 2. Local config

Copy `local.properties.template` to `local.properties` and fill in:
- `FIREBASE_DATABASE_URL`
- `FIREBASE_PROJECT_ID`
- `GOOGLE_WEB_CLIENT_ID` — from Firebase Console → Authentication → Sign-in method → Google →
  "Web client (auto created by Google Service)"

### 3. iOS Google Sign-In

In Xcode, add the `GoogleSignIn-iOS` SPM package to the `iosApp` target, then add the
`REVERSED_CLIENT_ID` value (from `GoogleService-Info.plist`) as a URL scheme in `Info.plist`.

## Running

- Android: `./gradlew :androidApp:assembleDebug`, or the IDE run configuration
- iOS: open `/iosApp` in Xcode and run from there

## Testing

- Shared unit tests: `./gradlew :shared:testAndroidHostTest`
- iOS unit tests: `./gradlew :shared:iosSimulatorArm64Test`

## CI/CD

GitHub Actions (`.github/workflows/android-ci.yml`) builds the Android app and runs shared unit
tests on every push/PR to `main`. Deployment to Google Play isn't wired up yet (pending Play
Developer account activation).

## Known Limitations

- The iOS app icon is still the default placeholder — the Android launcher icon has a custom ball
  icon, but the iOS equivalent needs real raster assets this environment can't generate.
- A few guarantees (e.g. room join capacity) are enforced client-side rather than via Firebase
  rules — a deliberate, documented trade-off for a casual game, not hardened against a determined
  cheater.

---

Built on the [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) project template.
