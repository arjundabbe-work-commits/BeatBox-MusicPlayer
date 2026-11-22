# BeatBox development status

Status recorded 22 November 2026 for `codex/beatbox-android-reconstruction`.

## Project

- Repository: <https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer>
- Application ID: `com.beatbox.player`
- Java, XML, AndroidX, Media3/ExoPlayer, Room, Gradle, GitHub Actions.
- Playback lives in `PlaybackService`; screens create MediaControllers rather than players.
- The recovered eight XML layouts remain in `docs/original-layouts/`. The splash screen and gradient styling remain in the runtime app.

## Feature status

| Area | Status |
|---|---|
| Local song discovery, permission flow, manual refresh, change observation | Implemented (observer on Home and rescan on return); device validation pending |
| Local search (title, artist, album) | Implemented; unified categorized search results are absent |
| Albums, artists, folders, genres, recently added library browsing | Implemented from MediaStore cache/genre membership; verification pending |
| Media3 player, background service, media session, mini-player | Implemented; hosted emulator verification is in progress; physical device pending |
| Queue snapshot persistence and queue UI | Implemented; verification pending |
| Favourites, playlists, playlist ordering, history, settings persistence | Implemented; verification pending |
| Sleep timer | Implemented with saved deadline and shared-controller pause; JVM calculation tests added; Android background behavior pending verification |
| Playback speed | 0.75×–2× through the shared MediaController with persisted preference; device verification pending |
| Lyrics | Local LRC/text import and playback-synchronized highlighting implemented; parser tests added; Android flow pending verification |
| Song-based local radio | Seed, artist, and album metadata drive a bounded local queue; JVM tests added; playback validation pending |
| History-based recommendations and artist/genre-seeded radio | Not implemented; radio currently uses song artist/album metadata |
| Equalizer/audio effects, sleep timer, crossfade | Not implemented |
| Video library, account/cloud sync, online music catalogue | Not implemented; no provider credentials or licensing configured |

## Automated validation

- JVM test sources cover music search/sorting, library grouping, radio ordering, LRC parsing, sleep timer arithmetic, database persistence, and playback controls.
- Android instrumentation sources cover Room behavior, generated local WAV playback, MediaController connection, album-category navigation, sleep timer set/cancel, unmatched search, and the empty lyrics state.
- GitHub Actions run [#33](https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer/actions/runs/45033834801), commit `107f842`, completed its **Build, lint and unit tests** job successfully. It ran clean, JVM unit tests, Android lint, and `assembleDebug assembleRelease`. Run [#32](https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer/actions/runs/45033694656), commit `c1a4c89`, uploaded the 6.75 MB debug APK artifact.
- Run #33's API 35 emulator job is still in progress on `macos-15-intel`. Earlier Linux attempts timed out waiting for boot, and the first macOS attempt used a retired ARM label; the workflow now selects the supported Intel runner. The latest documentation-only commit is queued for CI.
- No physical Galaxy A35 test has been performed. The local workstation has no configured Android SDK, so build validation runs on GitHub Actions.
- GitHub Actions reports a non-failing notice that Gradle 8.13 is out of date; no upgrade has been made during this stabilization pass.

## Remaining work

1. Confirm the API 35 emulator job on the current `macos-15-intel` runner and fix any instrumentation failures.
2. Install the generated debug APK on a device and complete the checklist in `DEVICE_TESTING.md`.
3. Implement history-based recommendations, artist/genre-seeded radio, and the missing audio effects (crossfade, equalizer, and related playback controls), each with focused tests.
4. Improve search into categorized results and restore the original runtime visual identity beyond the preserved XML references.
5. Publish a final completion matrix only after each applicable item is implemented and verified.
