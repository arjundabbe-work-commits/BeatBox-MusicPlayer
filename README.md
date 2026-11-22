# BeatBox

BeatBox is a native Android Java music player for audio stored on the device. The project keeps the recovered BeatBox splash screen, gradient theme, and original XML files while rebuilding the runtime around Media3 and Room.

## Implemented

- MediaStore audio discovery with runtime permission handling and manual refresh.
- Debounced MediaStore change observation while Home is visible, plus rescan when returning from another section.
- Device-backed library browsing for songs, albums, artists, folders, genres, recently added, recently played, favourites, and playlists.
- Local title, artist, and album search.
- Media3 ExoPlayer playback in a `MediaSessionService`, shared through `MediaController`.
- Now Playing with seek, shuffle, repeat, favourite toggle, queue entry point, and album art loading off the UI thread.
- Mini-player, OS media-session notification, and handling for audio becoming noisy.
- Room persistence for favourites, playlists and ordered playlist tracks, listening history, queue snapshots, and settings.
- Favorites, playlist management/detail, queue, recent history, settings, and about screens.
- Configurable sleep timer with a persisted deadline, remaining-time display, cancel action, and pause through the shared MediaController.
- Playback speed choices from 0.75× to 2×, persisted in Room and app preferences.
- User-imported local `.lrc` or plain-text lyrics, with timestamp parsing and active-line highlighting for synchronized files.
- Song-based local radio queues related tracks using actual artist/album metadata, with no online catalogue.
- JVM tests for local search/sort/time algorithms; Room and local-fixture playback instrumentation tests.
- Espresso instrumentation flows for library navigation, local search, sleep timer, and the lyrics empty state.
- GitHub Actions for JVM tests, lint, debug/release APK builds, and API 35 emulator instrumentation.

Online streaming, sign-in/cloud sync, lyrics, equalizer/audio effects, sleep timer, crossfade, video browsing, and a full albums/artists/folders/genres library are not implemented. The app does not claim access to an online catalogue.

## Build

Requirements: JDK 17, Android SDK Platform 36, Android Build Tools 36.0.0, and the included Gradle 8.13 wrapper.

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease
```

For instrumentation tests, start an API 35 Google APIs emulator and run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Open this repository in Android Studio and allow Gradle sync to download project dependencies. The app ID is `com.beatbox.player` (version `1.0`, code `1`). The hosted workflow uploads the debug APK and test reports on successful builds.

## Source map

- `app/src/main/java/com/beatbox/player/` — screens, app process, and playback service.
- `app/src/main/java/com/beatbox/player/data/` — Room entities and DAOs.
- `app/src/main/java/com/beatbox/player/domain/` — library algorithms and track metadata.
- `app/src/main/res/` — native XML layouts and BeatBox visual resources.
- `docs/original-layouts/` — recovered original layouts preserved for reference.
- `docs/DEVELOPMENT_STATUS.md` — verified feature and validation status.
- `docs/DEVICE_TESTING.md` — physical-device checklist and setup.

## Current validation status

See [the development status report](docs/DEVELOPMENT_STATUS.md) for the latest CI run, known gaps, and verification boundaries. A successful hosted build is not evidence of Samsung Galaxy A35 testing; physical-device verification remains separate.
