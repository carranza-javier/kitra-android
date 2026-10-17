# Kitra

Traduce cualquier texto de la pantalla del móvil con **captura nativa + una pulsación**. Haz una captura con los botones de siempre, pulsa «Traducir» en el aviso y lee la traducción, hecha con Gemini, en una hoja ligera. Copia, cierra y sigue leyendo.

- Sin cuentas, sin servidor y sin analíticas. Cada usuario usa su propia clave de Gemini API.
- Las capturas nunca se borran, mueven ni modifican. La imagen solo se envía a Google cuando pulsas «Traducir».

Estado: **fase 1 (Android)**. La especificación está en [`SPEC.md`](SPEC.md) y las decisiones en [`docs/DECISIONES.md`](docs/DECISIONES.md).

## Estructura

```
core/   Lógica sin Android (portable a iOS con KMP): prompt, TranslationProvider,
        cliente REST de Gemini con streaming, filtro de capturas y tamaño de imagen. Tests unitarios.
app/    App Android (Kotlin + Jetpack Compose): configuración, detección de capturas (JobScheduler),
        aviso heads-up, entrada por compartir, preprocesado y vista de resultado.
testdata/  Capturas de prueba ficticias (de, fr, it, en) y el script que las genera.
```

El prompt de traducción está en un único archivo: `core/src/main/kotlin/app/kitra/core/TranslationPrompt.kt`.

## 1. Obtener una clave de Gemini API

1. Entra en <https://aistudio.google.com/apikey> con tu cuenta de Google.
2. Pulsa **Create API key** y cópiala.
3. El nivel gratuito tiene límites diarios, que puedes consultar en <https://aistudio.google.com/rate-limit>. Google puede cambiarlos.
4. **Privacidad:** en el nivel gratuito, Google puede usar lo que envías para mejorar sus productos, y revisores humanos pueden leerlo. Con facturación activada no se usa. Detalles: <https://ai.google.dev/gemini-api/terms>.

La clave se introduce **dentro de la app** y se guarda cifrada con Android Keystore. Nunca la pongas en el código ni en archivos del repositorio.

## 2. Compilar

Requisitos: JDK 17 o posterior y el Android SDK con la plataforma 37 (`sdkmanager "platforms;android-37.0"`). Crea `local.properties` con `sdk.dir=...` si Android Studio no lo hace por ti.

```bash
./gradlew test            # tests unitarios
./gradlew assembleDebug   # APK de depuración: app/build/outputs/apk/debug/app-debug.apk
```

## 3. Instalar en el móvil

1. En el móvil, activa **Opciones de desarrollador → Depuración USB** y conéctalo por USB.
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`

También puedes copiar el APK al móvil y abrirlo (habrá que permitir instalar apps desconocidas).

## 4. Configurar (una sola vez)

1. Abre **Kitra**.
2. Pega tu clave y pulsa **Guardar y probar**.
3. Acepta los permisos que se piden a continuación: **Fotos y vídeos → «Permitir todo»** (solo para detectar capturas nuevas) y **Notificaciones**.
4. Elige el idioma de destino (por defecto, el del sistema).

## 5. Usar

- **Captura:** haz una captura con encendido + bajar volumen → aparece «Captura detectada · Traducir» → púlsalo → la traducción aparece en streaming → **Copiar** / **Cerrar**.
- **Compartir:** desde cualquier app o la galería, comparte una imagen con **Kitra**.

## Probar en el emulador (desarrollo)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Proveedor falso (solo debug), para probar sin clave:
adb shell am start -n app.kitra/.ui.setup.SetupActivity --ez debug_fake_provider true
adb shell pm grant app.kitra android.permission.READ_MEDIA_IMAGES
adb shell pm grant app.kitra android.permission.POST_NOTIFICATIONS
adb shell am start -n app.kitra/.ui.setup.SetupActivity   # programa la detección
adb shell input keyevent KEYCODE_HOME
adb shell input keyevent KEYCODE_SYSRQ                     # captura real del sistema
adb shell dumpsys notification --noredact | grep -A20 app.kitra
# Compartir una imagen:
adb push testdata/screenshot_de.png /sdcard/Download/
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/screenshot_de.png
adb shell am start -a android.intent.action.SEND -t image/png \
  --eu android.intent.extra.STREAM content://media/external/images/media/<ID> -n app.kitra/.ui.translate.TranslateActivity
adb logcat -s Kitra   # solo tiempos y tamaños, nunca contenido
```
