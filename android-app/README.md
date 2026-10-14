# Malayalam News Android APK

This Android app loads the Malayalam News channel-selection interface at:

https://rianmercer.github.io/malayalam-news/

It uses Mozilla GeckoView and packages the unmodified uBlock Origin WebExtension in the APK.

## Build locally

Install Android Studio, Android SDK Platform 35, and JDK 17. Run the build from this directory after downloading and extracting the official uBlock Origin Firefox XPI into:

`app/src/main/assets/ublock/`

The folder must contain the extension's `manifest.json`.

Then run:

```bash
gradle assembleDebug
```

The APK is created at:

`app/build/outputs/apk/debug/app-debug.apk`

## Automated build

The GitHub Actions workflow downloads a pinned release of the official uBlock Origin Firefox extension, extracts it into the APK assets, compiles the app, and uploads a debug APK as an artifact.

## Important limitations

- This is a personal-use APK; it is debug-signed, not Play Store release-signed.
- The content blocker filters web requests using its standard lists. It cannot guarantee that every YouTube ad will be blocked, and YouTube may detect ad blocking or interrupt playback.
- Blocking behaviour in an embedded GeckoView app is not as extensively validated as uBlock Origin in the normal Firefox Android browser. Test the target live channels before relying on it.
- Broadcaster sites may still show server-delivered ads, sponsorships, or video ads that a browser blocker cannot reliably distinguish.
- The app does not download or rebroadcast channel streams.
- See `THIRD_PARTY_NOTICES.md` for the bundled extension's source and license.
