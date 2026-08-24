# fastlane (optional)

Store listing text lives in `metadata/android/en-US/`.

```bash
# from repo root, after installing fastlane
bundle exec fastlane android bundle
```

`supply` / Play upload needs a Play Console service account JSON. Leave `Appfile` `json_key_file` empty until you have one. You can also upload the AAB by hand from `app/build/outputs/bundle/release/`.
