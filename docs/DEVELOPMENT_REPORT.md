# BeatBox reconstruction and development report

## Repository recovery

- The working repository is <https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer>, branch `codex/beatbox-android-reconstruction`.
- The recovered original project contained eight XML layouts but no application source, manifest, Gradle setup, theme resources, artwork files, or backend configuration. Original `main` and `master` were preserved; this work remains on the development branch.
- The eight recovered layouts are preserved in `docs/original-layouts/`. The splash layout and gradient treatment have runtime equivalents. Other screens were reconstructed with real device audio because the original referenced image assets, sample catalogue entries, and a custom image-slider widget that were not present. Imaginary tracks and remote catalogues were not added.
- The original app ID could not be recovered. `com.beatbox.player` remains provisional.

## Architecture

- Native Java/XML Android app, min SDK 23, compile SDK 36, target SDK 35.
- MediaStore is the device-library source. Room stores favourites, playlists and ordered entries, history, queue snapshots, and key/value settings.
- One Media3 1.11.1 ExoPlayer is owned by `PlaybackService` and exposed via MediaSession. Activities use MediaController; they do not create additional playback engines.
- No online music, authentication, cloud service, or copyrighted demo content is configured.

## Implemented in source

- Timed launch splash, local audio permission flow, MediaStore scan/refresh, library change observation, and local browsing for songs, albums, artists, folders, genres, Recently Added, and Recently Played.
- Debounced local song search by title, artist, and album.
- Shared Media3 playback with queue, notification/session, seek, shuffle/repeat, mini-player, playback speed, and persistent queue snapshot.
- Room-backed favourites, playlists, listening history, queue, and settings; local LRC/plain-text lyrics import and synchronized display; persistent sleep timer.
- Song-seeded local radio and library radio actions seeded by artist, album, and MediaStore genre metadata. The new actions and tests await hosted build/test validation.
- A deterministic history/favourite/artist/album-scored recommendation algorithm and a “Recommended for You” screen. Both were added after run #36 and are awaiting hosted build/test validation.
- GitHub Actions for clean, JVM unit tests, lint, debug/release assembly, API 35 emulator instrumentation, failure reporting, and artifact upload.

## Incomplete or unverified

- Equalizer, bass boost, virtualizer/spatial controls, crossfade, categorized search, and a local video browser/player are not implemented. Media3's upstream crossfade feature request remains open: <https://github.com/androidx/media/issues/2>.
- Recommendations currently do not use genre, skip, or continue-listening signals.
- The runtime Home and Now Playing screens preserve the dark gradient and BeatBox name but do not yet restore all original screen compositions. No screenshot review has been completed.
- No physical Galaxy A35 test has been performed.

## Validation

- GitHub Actions run [#36](https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer/actions/runs/45037996424), commit `039b36e62af16893be266c8795fefa6aee8035de`: clean/build, Android lint, JVM unit tests, and debug/release APK assembly passed. API 35 emulator tests failed: four Espresso tests could not get window focus and the generated WAV test fixture was written under the instrumentation package's cache, which the app player could not open. The current working tree contains fixes for those diagnosed problems, but they have not yet been run in CI.
- A prior successful hosted build uploaded debug APK artifact `beatbox-debug-apk`, 6.75 MB, from [run #32](https://github.com/arjundabbe-work-commits/BeatBox-MusicPlayer/actions/runs/45033694656). The exact APK version is 1.0 (versionCode 1). The artifact is available from the run's Artifacts section; the release APK was not uploaded.
- Local Gradle cannot resolve Android build tasks because this machine lacks SDK Platform 36. Installed Platform 37 alone does not satisfy the project's configured compile SDK. Pure Java domain production sources passed `javac --release 17`; the complete Android build and tests are not claimed as local passes.

## Next work

1. Push and rerun the instrumentation repairs, then fix the actual remaining failures from the report.
2. Complete the missing playback and library features, test them, and restore the principal runtime screen layouts.
3. Run full CI and capture/review emulator screenshots; then complete `DEVICE_TESTING.md` on the Galaxy A35.
