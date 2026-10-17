# Decisiones de Kitra

Registro de decisiones tomadas durante la fase 1 (Android). Fecha: 17-10-2026.

## Stack y arquitectura

| Decisión | Motivo |
|---|---|
| **Android nativo + módulo `:core` en Kotlin/JVM puro** (no KMP todavía) | La SPEC permite empezar sin KMP si añade complejidad. `:core` no depende de Android (solo `kotlinx-coroutines` y `kotlinx-serialization`, ambos multiplataforma, y `kotlin.io.encoding.Base64`). Pasarlo a KMP en la fase 3 consiste en cambiar el plugin y mover `HttpTransport`/`IOException` a `expect/actual`. |
| **Sin cliente HTTP externo**: interfaz `HttpTransport` en `:core` + `HttpURLConnection` en la app | Cero dependencias de red. Los tests usan un transporte falso. En iOS se implementará con `URLSession`. |
| Kotlin 2.4.21, AGP 9.4.1 (Kotlin integrado), Gradle 9.8.1, Compose BOM 2026.09.00 | Últimas versiones estables a 17-10-2026. |
| `compileSdk 37`, `targetSdk 36` (la versión del emulador con que se ha probado; lint avisa de que existe la 37 y se subirá tras probarla), **`minSdk 29`** (Android 10) | Compose 1.12 obliga a compilar contra la API 37. `minSdk 29` porque `MediaStore.RELATIVE_PATH` (filtro de capturas) existe desde Android 10, y `ImageDecoder` desde Android 9. Cubre la práctica totalidad de móviles en uso. |
| Paquete `app.kitra` | Corto y neutro. Cambiable antes de publicar. |
| Interfaz de usuario en español | Usuario principal hispanohablante. Los textos están en `strings.xml` para traducirlos más adelante. |

## Detección de capturas

- **JobScheduler con `addTriggerContentUri`** sobre `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (con descendientes), sin servicio permanente. `updateDelay` de 200 ms y `maxDelay` de 1 s para agrupar la inserción y la actualización que hace el sistema al guardar la captura.
- **Medido en el emulador (Android 16):** el sistema tarda unos 1,5 s en terminar de guardar la captura (estado *pending*). Kitra publica el aviso unos **0,5 s después** de que la captura queda guardada, y el banner aparece en pantalla **unos 2 s después de pulsar** el botón. Con estos datos **no hace falta un foreground service con ContentObserver** (consume batería y obliga a mostrar una notificación permanente). La medición definitiva en un móvil real queda para la fase 2.
- Los jobs con content URI son de un solo uso: se reprograman al terminar cada ejecución, al arrancar el teléfono (`BOOT_COMPLETED`), al actualizar la app (`MY_PACKAGE_REPLACED`) y al abrir Kitra.
- **Filtro** (`ScreenshotFilter`, en `:core`, con tests): la carpeta (`RELATIVE_PATH`) contiene `screenshot`, `screen_shot`, `screencap`, etc., y la imagen tiene menos de 60 s. El nombre del archivo solo se usa si no hay carpeta. MediaStore excluye por defecto las imágenes *pending*. Se guarda el último `_ID` notificado para no avisar dos veces por la misma captura.
- **Aviso:** canal `IMPORTANCE_HIGH` (necesario para el heads-up), **sin sonido ni vibración** (para no molestar en cada captura; comprobado que el heads-up sigue apareciendo), un único id de notificación (cada aviso nuevo sustituye al anterior) y `setTimeoutAfter(8 s)`: desaparece solo, sin acumularse. Pulsar el aviso entero o el botón «Traducir» hace lo mismo, así que cualquier toque traduce. Visibilidad `PRIVATE` en la pantalla de bloqueo y sin miniatura de la captura en el aviso.
- **Botón flotante (`SYSTEM_ALERT_WINDOW`) descartado en la fase 1:** necesita un permiso especial que se concede desde los ajustes del sistema y Play lo restringe. La notificación ya aparece en el mismo sitio y en el mismo momento. Se evaluará en la fase 2.

## Permisos

- `READ_MEDIA_IMAGES` (13+) / `READ_EXTERNAL_STORAGE` (≤12) y `POST_NOTIFICATIONS` (13+). Se piden juntos al activar el aviso y, la primera vez, **automáticamente justo después de validar la clave**, para que el alta no requiera buscar el botón.
- Se declara `READ_MEDIA_VISUAL_USER_SELECTED` para detectar de forma explícita el **acceso parcial de Android 14+**. En ese caso Kitra explica en la configuración que no puede ver las capturas nuevas y ofrece abrir los ajustes, o usar «Compartir».
- Si falta el permiso, la detección se desactiva (se cancela el job) y «Compartir» sigue funcionando.

## Traducción

- **Modelo predeterminado `gemini-3.5-flash-lite`** (comprobado en ai.google.dev el 17-10-2026): admite imágenes, tiene nivel gratuito, Google lo describe como el modelo 3.5 más rápido y económico, y lo recomienda para proyectos nuevos junto a `gemini-3.8-flash`. Su razonamiento predeterminado es «minimal», adecuado para baja latencia. Se puede cambiar en «Avanzado» sin recompilar (por ejemplo, a `gemini-3.8-flash` si la calidad no convence). La elección final se hará midiendo con la clave real (fase 2).
- **REST `models/{model}:streamGenerateContent?alt=sse`** con la clave en la cabecera `x-goog-api-key` (nunca en la URL). La documentación nueva promociona también la Interactions API, pero `streamGenerateContent` sigue documentado y sin aviso de obsolescencia, y es más simple.
- **No se envía `thinkingConfig`**: el nombre y los valores admitidos cambian entre familias de modelos, y el valor predeterminado de flash-lite (minimal) ya es el deseado. Así, cambiar de modelo no rompe la petición.
- **Prueba de la clave:** `GET models/{model}`, que no consume tokens y valida a la vez la clave y el modelo. La clave solo se guarda si la prueba pasa.
- **Prompt:** un único archivo, `core/src/main/kotlin/app/kitra/core/TranslationPrompt.kt`. Está en inglés porque los modelos siguen mejor las instrucciones en inglés, y pide responder en el idioma de destino. Se pide texto sin Markdown porque la vista muestra texto plano.
- **Iteración del prompt con Gemini real (17-10-2026):**
  1. El ejemplo literal `"[illegible]"` hacía que el modelo lo copiara en inglés dentro de la traducción al español. Se eliminó (hay un test que lo comprueba).
  2. Una regla sobre «texto ilegible» demasiado protagonista provocó una vez que el cuerpo de un artículo italiano saliera **sin traducir**. Se añadió «Translate every sentence… Never leave sentences in the original language» y se distinguió «texto cortado en el borde» (traducir lo visible y terminar en «…») de «texto realmente ilegible». En las ejecuciones posteriores (de, fr, it, en y repeticiones) todo salió traducido.
- **Preprocesado:** decodificación con `ImageDecoder` redimensionando directamente a un **lado largo de 1600 px** y JPEG al 85 % (una captura de 1080×2400 pasa de unos 1,2 MB en PNG a unos 64 KB). Si superase 7 MB, se reintenta a 1200 px y calidad 70; si aun así no cabe, se muestra el error «imagen demasiado grande».
- **Todo en memoria**: no se escriben archivos temporales. Aun así, la caché se vacía al cerrar la vista y al iniciar la app.
- **Sin llamadas duplicadas:** el `ViewModel` ignora un segundo `start` con la misma URI (por ejemplo, al girar la pantalla). Cerrar la vista cancela la corrutina y cierra la conexión HTTP.
- **OCR local (ML Kit) no implementado en la fase 1:** añadiría una dependencia pesada y un paso más, y el modelo multimodal ya recibe la imagen pequeña. Se medirá en la fase 2 si aporta velocidad.

## Clave y privacidad

- La clave se cifra con **AES-256-GCM usando una clave no exportable de Android Keystore** y en `SharedPreferences` solo se guarda el texto cifrado. No se usa `androidx.security:security-crypto` porque está obsoleta. `allowBackup=false`.
- **Condiciones de Gemini API (comprobadas el 17-10-2026):** en el nivel gratuito, Google usa el contenido y las respuestas para mejorar sus productos, y revisores humanos pueden leerlos; en el nivel de pago, no. Esto se muestra **destacado en la pantalla de configuración**, no escondido.
- Logs: solo tiempos, tamaños en KB y el tipo de excepción. Nunca la clave, el texto, ni rutas o nombres de archivo. Se revisó el `logcat` completo del emulador después de las pruebas.

## Vista de resultado

- Actividad translúcida (`Theme.Kitra.Overlay`) con una hoja inferior en Compose sobre la app anterior. `singleTask` + `taskAffinity` propia + `excludeFromRecents`: no aparece en recientes y al cerrar se vuelve a lo que se estaba haciendo.
- Se cierra con «Cerrar», tocando fuera de la hoja o con el gesto Atrás. «Copiar» confirma con «Copiado» durante 1,5 s.
- El texto es seleccionable, por si solo se quiere copiar una parte.

## Proveedor falso

- `FakeTranslationProvider` (en `:core`) emite un texto fijo en streaming. **Solo se puede activar en compilaciones *debug*** (en «Avanzado» o con `adb shell am start -n app.kitra/.ui.setup.SetupActivity --ez debug_fake_provider true`). En *release*, `Settings.useFakeProvider` siempre devuelve `false`.

## Direct Share

- `shortcuts.xml` con `<share-target>` + un acceso directo dinámico de larga duración (`pushDynamicShortcut`). El sistema decide si lo muestra en la fila superior del menú de compartir según el uso.
