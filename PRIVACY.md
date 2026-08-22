# Privacy policy — dear diary (Android)

**Last updated:** 22 August 2026  
**Developer:** KarOS Labs  
**App:** Dear Diary: Voice Journal (`com.karoslabs.deardiary`)

dear diary is a voice journal that runs on your phone. It is built so that journal audio and transcripts do not leave the device through this app.

## What we collect

**Nothing.**

There is no account. There is no cloud. There is no analytics SDK. There is no crash reporter that uploads your journal. There is no advertising identifier. The Android package does not request the `INTERNET` permission, so this app cannot open a journal network connection.

## What stays on your device

- Voice recordings you make in the app
- Transcripts produced on the device
- Titles, tags, and the dates you wrote
- Theme preference (system / dark / light)

These live in app-private storage and a local Room database. Android Auto Backup and cloud device-transfer are turned off for this app.

## Speech to text

Transcription uses the Android / Google on-device speech recognizer when the device provides one (`createOnDeviceSpeechRecognizer` on Android 12+, otherwise the system recognizer with `EXTRA_PREFER_OFFLINE`).

Android or Google may download a speech model **once** so recognition can work later with no internet. That download is a system service, not a dear diary upload. Your journal audio and the words you speak are never sent by this app. If an on-device model is missing, recognition may fail locally rather than being sent to a server by dear diary.

## Export and import

You can export a backup (words + audio in one file) to this device and share it yourself with the Android share sheet. You can import a backup that is already on the device. You choose where that file goes. dear diary does not upload it.

## Wipe

Settings includes **Wipe everything**. That erases every entry, tag, and audio file stored by dear diary on that phone. There is no server copy to restore from.

## Children

The app is a personal journal. It is rated for a general audience. We do not collect data from anyone, including children.

## Contact

Questions about this policy: use the Play Store storefront contact for KarOS Labs, or the site at https://dear-diary-13p.pages.dev

## Changes

If this policy ever changes, the date at the top will change. Because the app collects nothing, a change would only describe how the on-device product works.
