# AvideTravel Android

Native Android application for AvideTravel.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin 9.4.1
- compileSdk / targetSdk 37
- Coil 3 for native image loading
- Direct HTTPS access to AvideTravel public APIs
- No WebView

## Live AvideTravel integration

The app consumes:

- `https://avide.travel/api/services`
- `https://avide.travel/api/agents`

The native screens are:

- Home
- Deals
- Explore
- Contact

Deal and agent content is loaded directly from the existing AvideTravel backend. Booking pages that do not yet have a native checkout API open in the user's normal browser; no in-app WebView is used.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK 37 installed.

Command line:

```bash
gradle :app:assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

A GitHub Actions workflow also builds and publishes the debug APK as an Actions artifact on every push to `main`.

## Package

`com.avidetravel.app`

## Next native modules

The project is structured so native authentication, traveler profiles, saved trips, push notifications, booking status, payments, and agent chat can be added without converting the app to a WebView.
