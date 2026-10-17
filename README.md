# Kitra

Translate any text on your phone screen with **a native screenshot + one tap**. Take a screenshot with the usual buttons, tap "Translate" on the notification and read a natural translation in a lightweight sheet. Copy, close and keep reading.

- No accounts, no servers of its own, no ads, no analytics.
- **With the user's own free Gemini API key**, Kitra translates with Gemini (context, idioms and tone). This is always the main engine when a key is set and the phone is online.
- **Without a key, offline, out of quota or if Gemini fails**, Kitra falls back to a clearly labelled *basic mode* on the device (Gemini Nano when the phone has it, otherwise ML Kit). In basic mode the screenshot and its text never leave the phone.
- Screenshots are never deleted, moved or modified.

Application id: `com.kettenki.kitra` · Version 1.0.0 · UI in English, Spanish and German.

## Project structure

```
core/      Platform-independent logic (portable to iOS with KMP): translation prompt, TranslationProvider,
           Gemini REST client with streaming, engine selection and basic-mode fallback, OCR screen-text
           filter, screenshot filter and image sizing. Unit tests.
app/       Android app (Kotlin + Jetpack Compose): welcome, key tutorial, settings, diagnostics,
           screenshot detection (JobScheduler), heads-up notification, share target, photo picker,
           image preprocessing, on-device engines and result sheet.
tools/     strings_source.py (single source of the UI strings in en/es/de), play_listing.py,
           play_graphics.py (store graphics).
play/      Google Play material: listing texts, permission declaration, privacy policy, graphics.
           Signed AABs go to play/release/ (ignored by git).
testdata/  Fictional test screenshots (de, fr, it, en, zh) and the script that generates them.
```

The translation prompts (cloud and on-device) live in a single file: `core/src/main/kotlin/com/kettenki/kitra/core/TranslationPrompt.kt`.

## Distributions (product flavors)

| Flavor | Photo permission | Screenshot flow | versionCode |
|---|---|---|---|
| `full` | `READ_MEDIA_IMAGES` | Every new screenshot shows a "Translate" notification: **one tap** | 1 |
| `play` | none | A new image shows a notification that opens the system photo picker: **two taps** | 2 |

Both include the share target. `play` has a higher versionCode so it can replace a rejected `full` upload.

## Translation engines

| Engine | When | Privacy |
|---|---|---|
| Gemini (cloud, user's own key) | Key set and online | The screenshot is sent directly to Google when the user taps "Translate" |
| Gemini Nano (on device) | Basic mode on supported phones | Stays on the phone |
| ML Kit (on device) | Basic mode on any phone with Google Play services | Stays on the phone (ML Kit sends technical diagnostics to Google, never content) |

## Get a free Gemini API key

1. Open <https://aistudio.google.com/apikey> and sign in with a Google account.
2. Tap **Create API key** and copy it.
3. Back in Kitra, the key is detected from the clipboard: confirm it with one tap.

Free-tier limits are shown in Google AI Studio and may change. **Privacy:** on the free tier, Google may use submitted content to improve its products and human reviewers may read it; with billing enabled it does not. The Gemini API Terms require users to be 18+. See <https://ai.google.dev/gemini-api/terms>.

The key is stored encrypted with the Android Keystore. Never put real keys in code or in the repository.

## Build

Requirements: JDK 17+ and the Android SDK with platform 37 (`sdkmanager "platforms;android-37.0"`).

```bash
./gradlew test                                  # unit tests
./gradlew assembleFullDebug assemblePlayDebug   # one APK per ABI, e.g. app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk
./gradlew bundleFullRelease bundlePlayRelease   # signed Android App Bundles for Google Play
```

### Release signing

The upload key is never committed. Gradle reads it from environment variables
(`KITRA_STORE_FILE`, `KITRA_STORE_PASSWORD`, `KITRA_KEY_ALIAS`, `KITRA_KEY_PASSWORD`) or from a properties file
outside the repository: `~/.kitra/keystore.properties` by default (override with `KITRA_KEYSTORE_PROPERTIES`):

```properties
storeFile=/absolute/path/to/kitra-upload.jks
storePassword=...
keyAlias=kitra-upload
keyPassword=...
```

Without it, release builds are produced unsigned.

## Install on a phone

1. Enable **Developer options → USB debugging** and connect the phone.
2. `adb install -r app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk` (almost every current phone is arm64).

## Strings and store material

```bash
python tools/strings_source.py   # regenerates app/src/main/res/values*/strings.xml
python tools/play_listing.py     # regenerates play/listing/<locale>/*.txt and checks Play's limits
python tools/play_graphics.py <Roboto.ttf> <captures_dir>   # regenerates play/graphics/
```

## Debugging

```bash
adb logcat -s Kitra   # timings, sizes and engine only, never content
```

Advanced → Diagnostics shows which engines are available and which one was used for each translation.
