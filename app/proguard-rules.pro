# ML Kit (OCR, language ID, on-device translation) and the GenAI Prompt API (beta) rely on reflection and
# dynamically loaded Google Play services modules. Shrinking them broke model downloads and the Gemini Nano
# status check in the release build, so they are kept as-is.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.common.moduleinstall.** { *; }
-dontwarn com.google.mlkit.**

# Readable error names in the diagnostics screen and logs (no user content is ever logged).
-keepnames class com.kettenki.kitra.core.TranslationException
-keepnames class com.kettenki.kitra.core.TranslationException$*

# ML Kit's on-device translation discovers its components (and Firebase's) by class name and uses Google's
# data transport for its diagnostics. Without these, language pack downloads never completed in release.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.datatransport.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.firebase.**
