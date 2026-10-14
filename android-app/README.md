# Malayalam News Android APK

This is a lightweight Android WebView wrapper for the Malayalam News PWA hosted at:

https://rianmercer.github.io/malayalam-news/

The app keeps navigation to the selected official live-player page inside the app. It requires an internet connection and loads the website hosted on GitHub Pages.

## Build locally

Install Android Studio (including Android SDK Platform 35) and JDK 17. From this folder, run:

```bash
gradle assembleDebug
```

The debug APK is created at:

`app/build/outputs/apk/debug/app-debug.apk`

## Automated build

The repository workflow `.github/workflows/build-android-apk.yml` builds a debug APK after relevant changes and uploads it as a downloadable GitHub Actions artifact. Open the latest successful run, then download the `Malayalam-News-APK` artifact.

## Important limitations

- This is an app wrapper; it does not download or rebroadcast TV channels.
- It does not block YouTube advertisements. YouTube's player and broadcaster websites control their advertising and playback.
- A broadcaster can change or restrict its live stream at any time.
- The APK is debug-signed for personal installation, not a Play Store release.
