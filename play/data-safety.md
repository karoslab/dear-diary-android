# Play Console — Data safety

Answer the questionnaire so the storefront matches the iOS label **Data Not Collected**.

## Overview

- Does your app collect or share any of the required user data types? **No**
- Data not collected

## Why this is true

- No account, analytics, ads, crash upload, or cloud sync
- No `INTERNET` permission in the manifest
- Room database and audio files stay in app-private storage
- Auto Backup and device-transfer extraction are excluded
- Speech runs through Vosk on this device from the same microphone tap that writes the audio file; this app does not upload audio

## Optional notes for reviewers

If Play asks about speech: the English Vosk model is packaged in the app (Gradle fetches it at build time). There is no runtime download and no Google speech service. The app has no `INTERNET` permission.

## Privacy policy URL

Host [`PRIVACY.md`](../PRIVACY.md) (GitHub Pages, the product site, or any static host) and paste that https URL into Play Console.
