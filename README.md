# BeatBox

BeatBox is an early native Android music-player reconstruction. The original splash branding and gradient are retained, and the first functional foundation focuses on device music and offline playback.

## Current features

- Timed BeatBox splash screen.
- Local audio discovery through Android MediaStore.
- Runtime music permission prompts for Android 13+ and earlier supported Android versions.
- Local title, artist, and album search.
- Media3/ExoPlayer playback in a `MediaSessionService`, with basic queue, play/pause, previous/next, and seek controls.
- A mini-player and Android's default media-session notification integration.

The **application ID `com.beatbox.player` is provisional**. Confirm the final package ID before release; changing it after publishing would create a different app identity.

## Project structure

- `app/src/main/java/com/beatbox/player/` — activities, song model, and playback service.
- `app/src/main/res/layout/` — runtime XML layouts, including the preserved splash layout.
- `app/src/main/res/drawable/` and `values/` — gradient, fallback artwork, vector note, and theme.
- `docs/original-layouts/` — exact copies of the eight XML layouts recovered from the original `master` branch.
- `.github/workflows/android-build.yml` — hosted Android build, unit-test task, and debug APK artifact workflow.

## Requirements and build

- JDK 17 or newer (Android Gradle Plugin 8.13 requires JDK 17; CI uses Temurin 17).
- Android SDK Platform 36 and Build Tools 36.0.0.
- Gradle 8.13, downloaded by the included Gradle wrapper.
- Internet access for the initial Gradle and AndroidX dependency downloads.

Open this repository folder in Android Studio and allow Gradle sync. From the repository root, run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The GitHub Actions workflow runs the equivalent tasks on pushes to `codex/beatbox-android-reconstruction` and on pull requests targeting `main`, `master`, or that development branch. It uploads `app/build/outputs/apk/debug/app-debug.apk` when the build succeeds.

There are currently no project-authored unit tests. The `testDebugUnitTest` task is included so tests can be added to the build without changing CI.

## Limitations

- Favorites, Room playlists, listening history, categories for albums/artists/folders/genres, shuffle/repeat UI, artwork extraction, and queue persistence are not implemented.
- Search uses the in-memory scan held by Home.
- Background playback and media-session notification support are implemented through Media3's service architecture but still require emulator and physical-device verification.
- No successful Android build or device test has yet been recorded. A workflow passing must not be inferred from its presence in this repository.
