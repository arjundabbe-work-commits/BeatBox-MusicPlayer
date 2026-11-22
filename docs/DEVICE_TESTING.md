# Android device testing

## Prepare a Samsung Galaxy A35

1. Install Android Studio and open the BeatBox repository folder.
2. Allow Android Studio to install JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and platform tools if prompted.
3. On the phone, open **Settings → About phone → Software information** and tap **Build number** seven times to enable Developer options.
4. Open **Settings → Developer options**, enable **USB debugging**, then connect the phone to the computer with a data-capable USB cable.
5. Accept the phone's USB debugging authorization prompt.
6. In Android Studio, select the Galaxy A35 as the run target and launch the `app` configuration. Alternatively, build `app/build/outputs/apk/debug/app-debug.apk`, then install it from Android Studio or with `adb install -r app-debug.apk`.

## Checklist

- [ ] Fresh installation and launch; splash transitions to Home.
- [ ] Grant audio access; verify songs, titles, artists, albums, durations, and artwork.
- [ ] Deny access and verify the explanation and settings path.
- [ ] Refresh after adding/removing audio; verify missing media is handled.
- [ ] Start, pause, resume, seek, skip, shuffle, and change repeat mode.
- [ ] Open Now Playing and the mini-player from Home; verify state follows the service.
- [ ] Background the app, lock the screen, and test notification/lock-screen controls.
- [ ] Connect and disconnect Bluetooth/headphones; verify controls and audio interruption behavior.
- [ ] Add/remove favourites; restart the app and verify saved state.
- [ ] Create, rename, delete, reorder, and play a playlist; restart and verify persistence.
- [ ] Reorder/remove/clear the queue; restart and verify the saved queue behavior.
- [ ] Check listening history, clear history, and reopen the app.
- [ ] Check battery use during extended background playback and verify playback stops cleanly when appropriate.
- [ ] Update the app over an existing install and confirm user data remains available.

Record the Android version, app version, observed result, and any failure for each item. Emulator results do not substitute for this physical-device checklist.
