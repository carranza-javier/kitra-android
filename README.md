# Kitra

Traduce cualquier texto de la pantalla del móvil con **captura nativa + una pulsación**. Haz una captura con los botones de siempre, pulsa «Traducir» en el aviso y lee la traducción, hecha con Gemini, en una hoja ligera. Copia, cierra y sigue leyendo.

- Sin cuentas, sin servidor y sin analíticas. **Sin clave:** traduce en el propio teléfono (Gemini Nano si el móvil lo tiene; si no, ML Kit). Opcionalmente, «Máxima calidad» con Gemini en la nube y tu propia clave.
- Las capturas nunca se borran, mueven ni modifican. La imagen solo se envía a Google cuando pulsas «Traducir».

Estado: **fase 1 (Android)** en `main`; **fase 2 (traducción en el dispositivo)** en la rama `feature/on-device-translation` (ver [`docs/INFORME-FASE2.md`](docs/INFORME-FASE2.md)). La especificación está en [`SPEC.md`](SPEC.md) y las decisiones en [`docs/DECISIONES.md`](docs/DECISIONES.md).

## Estructura

```
core/   Lógica sin Android (portable a iOS con KMP): prompt, TranslationProvider,
        cliente REST de Gemini con streaming, filtro de capturas y tamaño de imagen. Tests unitarios.
app/    App Android (Kotlin + Jetpack Compose): configuración, detección de capturas (JobScheduler),
        aviso heads-up, entrada por compartir, preprocesado y vista de resultado.
testdata/  Capturas de prueba ficticias (de, fr, it, en) y el script que las genera.
```

El prompt de traducción está en un único archivo: `core/src/main/kotlin/app/kitra/core/TranslationPrompt.kt`.

## Motores de traducción (fase 2)

| Motor | Cuándo | Calidad | Privacidad |
|---|---|---|---|
| Gemini Nano (en el teléfono) | Automático si el móvil lo tiene (Pixel 9+, Galaxy S25+…) | Por medir en un móvil real | La captura no sale del móvil |
| ML Kit (en el teléfono) | Automático en cualquier otro móvil con Google Play Services | Literal: falla en expresiones y contexto | La captura no sale del móvil |
| Gemini en la nube | Solo si activas «Máxima calidad» en Avanzado y pones tu clave | La mejor | La captura se envía a Google |

## 1. (Opcional) Obtener una clave de Gemini API para «Máxima calidad»

1. Entra en <https://aistudio.google.com/apikey> con tu cuenta de Google.
2. Pulsa **Create API key** y cópiala.
3. El nivel gratuito tiene límites diarios, que puedes consultar en <https://aistudio.google.com/rate-limit>. Google puede cambiarlos.
4. **Privacidad:** en el nivel gratuito, Google puede usar lo que envías para mejorar sus productos, y revisores humanos pueden leerlo. Con facturación activada no se usa. Detalles: <https://ai.google.dev/gemini-api/terms>.

La clave se introduce **dentro de la app** y se guarda cifrada con Android Keystore. Nunca la pongas en el código ni en archivos del repositorio.

## 2. Compilar

Requisitos: JDK 17 o posterior y el Android SDK con la plataforma 37 (`sdkmanager "platforms;android-37.0"`). Crea `local.properties` con `sdk.dir=...` si Android Studio no lo hace por ti.

```bash
./gradlew test            # tests unitarios
./gradlew assembleDebug   # un APK por arquitectura: app/build/outputs/apk/debug/app-arm64-v8a-debug.apk, …
./gradlew bundleRelease   # AAB para Google Play
```

## 3. Instalar en el móvil

1. En el móvil, activa **Opciones de desarrollador → Depuración USB** y conéctalo por USB.
2. `adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` (casi todos los móviles actuales son arm64)

También puedes copiar el APK al móvil y abrirlo (habrá que permitir instalar apps desconocidas).

## 4. Configurar (una sola vez)

1. Abre **Kitra**: se piden los permisos solos. Acepta **Notificaciones** y **Fotos y vídeos → «Permitir todo»** (solo para detectar capturas nuevas).
2. Elige el idioma de destino (por defecto, el del sistema). Su paquete (unos 30 MB) se descarga solo, preferiblemente con wifi.
3. Ya está: no hace falta clave. Para usar Gemini en la nube: **Avanzado → Máxima calidad**, pega tu clave y pulsa **Guardar y probar**.

## 5. Usar

- **Captura:** haz una captura con encendido + bajar volumen → aparece «Captura detectada · Traducir» → púlsalo → la traducción aparece en streaming → **Copiar** / **Cerrar**.
- **Compartir:** desde cualquier app o la galería, comparte una imagen con **Kitra**.

## Probar en el emulador (desarrollo)

```bash
adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk
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
adb logcat -s Kitra   # solo tiempos, tamaños y motor, nunca contenido
# Forzar un motor para comparar (solo debug): nano | mlkit | cloud | auto
adb shell am start -S -n app.kitra/.ui.setup.SetupActivity --es debug_engine mlkit
```
