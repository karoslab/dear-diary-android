# dear diary for Android

A free voice journal. Record, get on-device speech-to-text, and keep the audio plus the words on this phone. No account. No cloud. No analytics. No journal network traffic.

This repository is the Android replica of [Dear Diary: Voice Journal](https://apps.apple.com/us/app/dear-diary-voice-journal/id6794079693) by KarOS Labs. Product site: https://dear-diary-13p.pages.dev

Package: `com.karoslabs.deardiary`

## Privacy, in one line

The app does **not** declare the `INTERNET` permission. Journal audio and transcripts cannot leave the process over the network. The header always shows **0 bytes sent** while you journal. The only speech-model download is a system download (Google / Android), once, so recognition can keep working offline. Your voice never rides along.

## Open in Android Studio

1. Install Android Studio (latest stable) with SDK 36 and JDK 17+.
2. **File → Open** this folder (`settings.gradle.kts` is at the root).
3. Let Gradle sync. The wrapper uses Gradle 8.13.
4. Choose a device or emulator (API 26+).
5. Run the `app` configuration (debug). Debug signing works out of the box.

Command line:

```bash
export ANDROID_HOME=/path/to/Android/sdk
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## What is implemented

- **Record** — press to speak, pause / resume, live transcript, save audio + text + duration + auto title. After save, a fixed reflection prompt (Android has no Apple Intelligence; the app says so).
- **Journal** — streak kept, entry count, GitHub-style heatmap, TODAY / YESTERDAY / date sections, entry cards.
- **Entry** — play and scrub audio, edit title and transcript, add / remove tags, delete, export one entry.
- **Search** — local Room FTS, “ran over local entries, 0 bytes sent”, gold underline on matches.
- **Settings** — export everything, import backup, storage bar + stats, System / Dark / Light, reduced-motion note, speech engine row, wipe everything.
- **Themes** — true-black dark gold, and a warm cream light theme that is designed, not inverted gray.

On-device speech prefers `SpeechRecognizer.createOnDeviceSpeechRecognizer()` (API 31+) and always sets `EXTRA_PREFER_OFFLINE`. Settings names the real engine.

## Build a release AAB (Play)

Debug builds do not need a store keystore.

For Play Console you need an upload keystore. Copy the example and fill it, **or** set environment variables.

```bash
cp keystore.properties.example keystore.properties
# then edit storeFile, storePassword, keyAlias, keyPassword
```

Environment variables (used if present; they win over the properties file):

| Variable | Meaning |
| --- | --- |
| `DEAR_DIARY_STORE_FILE` | Absolute path to the `.jks` / `.keystore` |
| `DEAR_DIARY_STORE_PASSWORD` | Keystore password |
| `DEAR_DIARY_KEY_ALIAS` | Key alias |
| `DEAR_DIARY_KEY_PASSWORD` | Key password |

Create a keystore (once):

```bash
keytool -genkeypair -keystore dear-diary-upload.jks -alias deardiary \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -dname "CN=KarOS Labs, OU=dear diary, O=KarOS Labs, L=Unknown, ST=Unknown, C=US"
```

Build the Play bundle:

```bash
export ANDROID_HOME=/path/to/Android/sdk
./gradlew :app:bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```

If no release keystore is configured, `bundleRelease` still builds and is signed with the debug key so CI can compile. **Do not upload a debug-signed AAB to production.**

Version: `1.0` / `versionCode 1`. Display name: **Dear Diary: Voice Journal**.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## Play Console

See:

- [`play/listing.md`](play/listing.md) — short + full description, mirroring the iOS privacy pitch
- [`play/data-safety.md`](play/data-safety.md) — Data safety: Data not collected
- [`play/screenshots.md`](play/screenshots.md) — screenshot guidance
- [`play/content-rating.md`](play/content-rating.md) — content rating notes
- [`PRIVACY.md`](PRIVACY.md) — privacy policy you can host
- [`fastlane/`](fastlane/) — store listing metadata + a Fastfile sketch
- [`docs/RELEASE.md`](docs/RELEASE.md) — release checklist

## Fastlane (optional)

```bash
cd fastlane
# metadata lives in fastlane/metadata/android/en-US/
# Supply / Play upload is documented in the Fastfile. You still need a Play service account.
```

## Architecture (short)

Kotlin, Jetpack Compose, Material 3 colors restyled to the dark-gold chrome. Single activity. Room (entries + tags + FTS). Audio in `filesDir/audio`. MediaRecorder + Media3 ExoPlayer. DataStore for theme. No accounts, ads, analytics, or cloud sync.
