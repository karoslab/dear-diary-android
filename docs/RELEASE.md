# Play Console checklist

1. Create the app **Dear Diary: Voice Journal**, package `com.karoslabs.deardiary`.
2. Host [`PRIVACY.md`](../PRIVACY.md) and add the URL.
3. Data safety: **Data not collected** — see [`play/data-safety.md`](../play/data-safety.md).
4. Content rating — see [`play/content-rating.md`](../play/content-rating.md).
5. Paste listing copy from [`play/listing.md`](../play/listing.md) or `fastlane/metadata/android/en-US/`.
6. Upload screenshots per [`play/screenshots.md`](../play/screenshots.md).
7. Create an upload keystore. Put secrets in env or `keystore.properties` (never git).
8. `./gradlew :app:bundleRelease` → upload `app/build/outputs/bundle/release/app-release.aab`.
9. Target API must meet Play’s current floor (this project targets 36).
10. App content: no ads, no IAP in this build, no login.
11. Declare that the app does not use advertising ID.
12. Test install from an internal testing track on a physical phone. Confirm airplane mode: record, transcribe (if the on-device model is present), search, export.

## Signing reminder

Play App Signing: upload the AAB signed with your **upload** key. Google holds the app signing key. Keep the upload `.jks` offline and backed up.
