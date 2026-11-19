# Friday setup for Android

This ZIP contains the Android source and an APK build workflow.

## Build with GitHub Actions

1. Extract the ZIP and upload or push its contents to the root of your repository. Keep .github/workflows/android-apk.yml and android-companion in their original locations.
2. Open the repository's Actions tab and run Build FRIDAY Android App APK.
3. Open the successful run and download FRIDAY-Android-Companion-debug from Artifacts. Extract that download to get the APK and open it on the phone.

## Build on your laptop

Open android-companion in Android Studio with JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. The project uses Gradle 8.10.2. Build the debug variant. The output is in android-companion/app/build/outputs/apk/debug.

## Configure Android

Launch Friday and open WhatsApp replies to enable Notification access. Use Read an open WhatsApp chat for the optional accessibility shortcut. See WHATSAPP_WITHOUT_API.md for the local reply flow. Local WhatsApp features require no app token, API key or QR code. The dotted-face dashboard is part of the Windows application.
