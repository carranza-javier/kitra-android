# Kitra

Translate any text on your phone screen with **a native screenshot + one tap**. Take a screenshot with the usual buttons, tap "Translate" on the notification and read a natural translation in a lightweight sheet. Copy, close and keep reading.

- No accounts, no servers, no analytics.
- **With your own free Gemini API key**, Kitra translates with Gemini (context, idioms and tone).
- **Without a key** (or offline), Kitra falls back to a basic on-device mode (Gemini Nano when the phone has it, otherwise ML Kit). The screenshot never leaves the phone in basic mode.
- Screenshots are never deleted, moved or modified. In Gemini mode, the image is sent to Google only when you tap "Translate".

## Project structure

```
core/      Platform-independent logic (portable to iOS with KMP): translation prompt, TranslationProvider,
           Gemini REST client with streaming, engine selection and fallback, OCR screen-text filter,
           screenshot filter and image sizing. Unit tests.
app/       Android app (Kotlin + Jetpack Compose): setup, screenshot detection (JobScheduler),
           heads-up notification, share target, image preprocessing, on-device engines and result sheet.
testdata/  Fictional test screenshots (de, fr, it, en, zh) and the script that generates them.
```

The translation prompt lives in a single file: `core/src/main/kotlin/.../core/TranslationPrompt.kt`.

## Translation engines

| Engine | When | Quality | Privacy |
|---|---|---|---|
| Gemini (cloud, user's own key) | Main engine whenever a key is set and the phone is online | Best | The screenshot is sent to Google when you tap "Translate" |
| Gemini Nano (on device) | Basic mode on supported phones (Pixel 9+, Galaxy S25+…) | Basic | Stays on the phone |
| ML Kit (on device) | Basic mode on any other phone with Google Play services | Basic, literal | Stays on the phone |

## 1. Get a free Gemini API key (optional, recommended)

1. Open <https://aistudio.google.com/apikey> and sign in with your Google account.
2. Tap **Create API key** and copy it.
3. Back in Kitra, the key is detected from the clipboard: confirm it with one tap.

Free-tier limits are shown in Google AI Studio and may change. **Privacy:** on the free tier, Google may use submitted content to improve its products and human reviewers may read it; with billing enabled it does not. See <https://ai.google.dev/gemini-api/terms>.

The key is stored encrypted with the Android Keystore. Never put real keys in code or in the repository.

## 2. Build

Requirements: JDK 17+ and the Android SDK with platform 37 (`sdkmanager "platforms;android-37.0"`). Create `local.properties` with `sdk.dir=...` if Android Studio does not.

```bash
./gradlew test            # unit tests
./gradlew assembleDebug   # one APK per ABI, e.g. app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
./gradlew bundleRelease   # Android App Bundle for Google Play
```

## 3. Install on a phone

1. Enable **Developer options → USB debugging** and connect the phone.
2. `adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` (almost every current phone is arm64).

## 4. Use

- **Screenshot:** take a screenshot → a "Translate" notification appears → tap it → the translation streams in → **Copy** / **Close**.
- **Share:** share any image with **Kitra** from any app or the gallery.

## Debugging

```bash
adb logcat -s Kitra   # timings, sizes and engine only, never content
```
