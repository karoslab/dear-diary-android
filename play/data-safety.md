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
- Speech runs through the system on-device recognizer; this app does not upload audio

## Optional notes for reviewers

If Play asks about the speech model: the one-time model download is performed by the Android / Google system speech service, not by dear diary. The app cannot initiate that download itself without internet permission. Users who already have an on-device model work fully offline.

## Privacy policy URL

Host [`PRIVACY.md`](../PRIVACY.md) (GitHub Pages, the product site, or any static host) and paste that https URL into Play Console.
