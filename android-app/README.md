# Malayalam News Android APK

This Android app loads the Malayalam News channel selection interface hosted at:

https://rianmercer.github.io/malayalam-news/

The experimental app uses Mozilla GeckoView and packages the unmodified uBlock Origin Firefox WebExtension.

## Automated build

The GitHub Actions workflow downloads a pinned release of the official uBlock Origin Firefox extension, extracts it into the APK assets, compiles the app, and uploads a debug APK as an artifact.

Open the latest successful run in the Actions tab and download the `Malayalam-News-APK` artifact.

## Important limitations

- This is a personal-use APK; it is debug-signed, not signed for a Play Store release.
- The content blocker filters web requests using its standard lists. It cannot guarantee that every YouTube ad will be blocked; YouTube may detect ad blocking or interrupt playback.
- Blocking behaviour in an embedded GeckoView app has not been tested as extensively as uBlock Origin in normal Firefox for Android. Test the target live channels before relying on it.
- Broadcaster sites may still show server-delivered ads, sponsorships, or video ads that a browser blocker cannot reliably distinguish.
- The app does not download or rebroadcast channel streams.
- See `THIRD_PARTY_NOTICES.md` for the bundled extension source and license.
