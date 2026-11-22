# BeatBox development status

Status updated 22 November 2026 on `codex/beatbox-android-reconstruction`.

## Project

- Repository: <https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer>
- Application ID `com.beatbox.player`; version `1.0` (versionCode 1).
- Native Java/XML, min SDK 23, compile SDK 36, target SDK 35, Media3 1.11.1, Room 2.8.5.
- A single ExoPlayer and MediaSession are owned by `PlaybackService`; screens connect through MediaController.
- The original eight XML layouts remain byte-for-byte under `docs/original-layouts/`. The splash screen and gradient theme remain in runtime resources. The main runtime screens are functional reconstructions and are not yet a visual restoration of every original composition.

## Feature and verification matrix

| Feature | Source / functional status | Automated test | Emulator | Physical device |
|---|---|---|---|---|
| MediaStore permission, discovery, refresh, change observer | Implemented for local audio; permission denial has guidance | Partial UI coverage | Pending; UI test currently fails before assertions because its activity window has no focus | Not tested |
| Songs, albums, artists, folders, genres, recently added | Implemented through MediaStore and local grouping | Library category flow exists | Pending | Not tested |
| Recently played, favourites, playlists, playlist ordering, settings and queue persistence | Room-backed implementation | DAO persistence tests and limited UI tests | Pending | Not tested |
| Search | Debounced local title/artist/album matching; category tabs, playlists and genre search not implemented | Unmatched query UI test exists | Pending; currently blocked by emulator window focus | Not tested |
| Playback, MediaSession, notification, background service, mini-player, Now Playing | Implemented with one Media3 player/session and local MediaStore items | Session connection and generated WAV tests | Run #36 failed before the fixture could be read; corrections are in the current working tree and need CI rerun | Not tested |
| Sleep timer and playback speed | Persisted deadline, shared-player pause, timer cancellation and 0.75×–2× speed implemented | Timer arithmetic and UI flow exist | UI test currently blocked by emulator window focus | Not tested |
| Local lyrics | LRC/text import, parsing, and active-line highlighting | Parser tests; empty-state UI test exists | Pending; currently blocked by emulator window focus | Not tested |
| Local radio | Song, artist, album, and MediaStore-genre radio actions are wired from library categories; new wiring is awaiting hosted build/test validation | Song-seed tests plus new category-order tests in the current working tree | Pending | Not tested |
| Recommendations | Deterministic local scoring and a “Recommended for You” screen use history, favourites, artist and album affinity; current-track exclusion and fallback are handled | New scoring tests in the current working tree; not yet CI-verified | Pending | Not tested |
| Equalizer, bass boost, virtualizer/spatial effect | Not implemented | None | Not tested; device effect support would need hardware validation | Not tested |
| Crossfade | Not implemented. Media3 does not expose a built-in queue crossfade in the configured player; upstream tracks this as an open feature request: <https://github.com/androidx/media/issues/2>. A reliable two-player/session design still needs implementation and tests. | None | Not tested | Not tested |
| Video library | Not exposed in runtime navigation; no local video browser/player currently implemented | None | Not tested | Not tested |
| Account, cloud sync, online catalogue/streaming | Not implemented; requires authorised providers and service configuration | None | Not applicable | Not applicable |

## Latest validation

- Run [#36](https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer/actions/runs/45037996424), commit `039b36e62af16893be266c8795fefa6aee8035de`: **Build, lint and unit tests passed.** The emulator job failed after running on the API 35 Intel emulator.
- Five instrumentation failures were reported: four Espresso tests blocked by `RootViewWithoutFocusException` (`customSleepTimerShowsRemainingTimeAndCanBeCancelled`, `lyricsScreenExplainsWhenNoTrackIsPlaying`, `searchFiltersOutAnUnmatchedLocalQuery`, and `libraryCanOpenAlbumCategoryAndReturnToCategories`) and `PlaybackEngineTest#decodesLocalAudioAndSupportsSeekPauseShuffleAndRepeat`, whose WAV fixture was written into the test APK cache instead of the app cache. The workflow uploaded reports and now emits test names/assertions. Current uncommitted changes move the WAV fixture to the target app cache and wake/dismiss the emulator keyguard before Espresso tests; these changes still need a hosted rerun.
- The total passed/skipped test counts are not stated here because the anonymous Actions page exposes failure annotations but not the uploaded XML artifact.
- Earlier successful build validation is run [#32](https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer/actions/runs/45033694656), which uploaded a 6.75 MB `beatbox-debug-apk` artifact. The release APK was built but not uploaded.
- Local `gradlew testDebugUnitTest` is blocked because this workstation lacks SDK Platform 36 (`local.properties` is absent; the installed SDK has Platform 37 only). No local Android build is claimed. Pure Java domain sources compile with `javac --release 17`.
- The API 35 emulator is hosted CI. No screenshot-based visual review has been completed. No Samsung Galaxy A35 has been tested.

## Remaining work

1. Push and rerun the current instrumentation fixes; inspect the resulting reports and correct any remaining focus or fixture failures.
2. Finish effect controls, crossfade/session design, categorized search, fully seeded radio UI, and recommendations for genre/recency/continue-listening signals.
3. Restore the original runtime Home/Dashboard/Now Playing compositions and capture/review the requested emulator screenshots.
4. Decide whether to add a native local video library; keep the current navigation free of a dead Video Library control.
5. Run the complete CI pipeline, then perform the manual Galaxy A35 checklist in `DEVICE_TESTING.md`.
