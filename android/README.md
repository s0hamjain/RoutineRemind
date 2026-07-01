# RoutineRemind — Android app

Kotlin + Jetpack Compose client with a C++ (NDK/JNI) native module for
audio/video/speech processing. Auth via Identity Platform (Firebase Auth SDK);
talks to the Spring Boot backend over REST.

## One-time setup (required before it will build)

1. **Install the Android toolchain** (if not already): Android Studio, plus the
   **NDK (Side by side)** and **CMake** via *Settings → SDK Manager → SDK Tools*.

2. **Add `google-services.json`.** Download it from the Firebase/Identity Platform
   console for the Android app (package `com.routineremind.app`) and place it at
   `android/app/google-services.json`. Use `google-services.json.template` as a
   reference. (The real file is git-ignored.)

3. **Generate the Gradle wrapper jar.** This repo ships the wrapper config but not
   the binary jar. Either open the `android/` folder in Android Studio (it will
   sync and create it), or run once with a local Gradle:
   ```bash
   cd android && gradle wrapper --gradle-version 8.9
   ```

## Run

- Open `android/` in Android Studio → let it sync → Run on an emulator/device.
- The app talks to the backend at `http://10.0.2.2:8080/api/v1/` (the emulator's
  alias for your machine's `localhost`). Start the backend first:
  ```bash
  cd ../backend && ./mvnw spring-boot:run
  ```

## Structure

```
app/src/main/
  java/com/routineremind/app/
    MainActivity.kt            Compose entry point
    auth/AuthManager.kt        Firebase Auth wrapper
    data/                      Retrofit API, models, client
    nativebridge/NativeAudio.kt  JNI bindings
    ui/                        ViewModel + Compose screens
    ui/theme/Theme.kt          Design system (palette, type)
  cpp/                         C++ NDK module (CMake)
    native-lib.cpp             JNI entry points
    audio_processor.{h,cpp}    RMS stub → real DSP in M5
```

## Native module

`NativeAudio` loads `libroutineremind_media.so` and currently exposes
`nativeVersion()` and `nativeComputeRms()`. In M5 this becomes the real
audio/video encoder + speech pre-processing pipeline.
