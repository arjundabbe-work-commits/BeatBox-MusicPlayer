# Open and run BeatBox in Android Studio

## Install Android Studio and SDK

1. Download and install the current Android Studio release from [developer.android.com/studio](https://developer.android.com/studio).
2. In Android Studio, open **Tools → SDK Manager** (or the SDK Manager button on the welcome screen).
3. Install **Android SDK Platform 36** and the latest Android SDK Build-Tools offered for it. Install **Android SDK Platform-Tools** for `adb`.
4. In **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**, select Android Studio's bundled JDK (JDK 17 or newer; AGP 8.13 requires JDK 17).

## Open and build

1. Choose **Open** and select the `BeatBox-MusicPlayer` repository folder containing `settings.gradle`.
2. Allow Gradle sync to finish. The first sync downloads Gradle 8.13 and AndroidX dependencies, so internet access is needed once.
3. If Android Studio prompts to install missing SDK components, accept the Android SDK 36 components.
4. Choose **Build → Make Project**. To build from a terminal on Windows, run `gradlew.bat assembleDebug` from the repository root. The debug APK, once a successful build completes, will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Run on an emulator

1. Open **Tools → Device Manager → Create Device**.
2. Choose a Pixel device profile, download a system image (API 35 or newer), finish setup and start the emulator.
3. Select that emulator in the Android Studio device selector and press **Run**.
4. The app asks for music access. The emulator starts with no music; copy audio files into it (for example, drag supported audio files into the emulator) and relaunch or refresh after adding them.

## Run on an Android phone

1. On the phone, open **Settings → About phone** and tap **Build number** seven times to enable Developer options (menu names vary by manufacturer).
2. Open **Developer options**, enable **USB debugging**, and connect the phone with a data-capable USB cable.
3. Accept the phone's **Allow USB debugging** prompt. On Windows, install the phone manufacturer's USB driver if Android Studio does not detect it.
4. Select the phone in Android Studio and press **Run**. Grant BeatBox music access when prompted, then select a song from the device library.

Background playback, lock-screen controls and notification behavior still need device verification; they have not been tested in this workspace.
