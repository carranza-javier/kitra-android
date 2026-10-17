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

---

# Fase 2: traducción en el dispositivo, sin clave (rama `feature/on-device-translation`)

Fecha: 17-10-2026. Objetivo: que Kitra traduzca sin clave de API desde la primera vez que se abre. El flujo de captura → aviso → ventana de resultado no cambia.

## Motores y selección automática

| Orden | Motor | Cuándo se usa |
|---|---|---|
| 1 | **Gemini en la nube** (clave del usuario) | Solo si el usuario activa «Máxima calidad» en Avanzado y hay una clave válida. |
| 2 | **Gemini Nano** (ML Kit GenAI Prompt API, con la captura como imagen) | Si `checkStatus()` devuelve `AVAILABLE`. |
| 3 | **ML Kit**: OCR (Text Recognition v2) + identificación de idioma + traducción local | Siempre, en cualquier móvil. Es el último recurso. |

- La lógica de selección y de recurso al siguiente motor vive en `:core` (`EngineSelection`, `FallbackTranslationProvider`) y tiene tests.
- **Recurso automático, solo si aún no hay texto en pantalla:** cualquier fallo de Nano (cuota, batería, no disponible, imagen no válida, respuesta vacía) pasa a ML Kit sin molestar. En la nube solo se recurre al dispositivo si el fallo es de red, de cuota o del servicio, con el aviso «traducido en el dispositivo». Una clave inválida se muestra como error, porque es un problema de configuración que el usuario debe conocer.
- `TranslationProvider` ahora emite eventos (`EngineStarted`, `Text`, `Progress`, `Notice`) en vez de solo texto, para poder mostrar descargas, avisos y la atribución del motor que realmente ha traducido.

## Gemini Nano

- `com.google.mlkit:genai-prompt:1.0.0-beta4` (beta, sin SLA). La API se ha comprobado con `javap` sobre las clases reales: `Generation.getClient(generationConfig { modelConfig { STABLE / FULL } })`, `checkStatus()`, `download()`, `generateContentStream(generateContentRequest(SystemInstruction, ImagePart, TextPart))`.
- **Modelo STABLE + FULL:** se prioriza la calidad. `FAST` queda como ajuste para medir en el móvil.
- `temperature 0.2`, `topK 16`, un solo candidato. Si el móvil no admite instrucciones de sistema (`isSystemPromptAvailable()`), las reglas van en el mismo turno que la imagen.
- **Prompt:** versión corta del mismo prompt en `TranslationPrompt.onDeviceSystemInstruction()` (mismo archivo único). Un modelo pequeño sigue peor las instrucciones largas, y la entrada está limitada a unos 4000 tokens.
- **Restricciones oficiales:** la inferencia solo se permite con la app en primer plano (la ventana de resultado lo está), hay una cuota por app y una cuota diaria de batería, y no funciona con el bootloader desbloqueado. Google solo valida oficialmente el **inglés y el coreano** para la Prompt API: la calidad al español hay que medirla en un móvil real.
- **Descarga de Nano:** si el estado es `DOWNLOADABLE`, se descarga sola al abrir la configuración con wifi. Con datos móviles, solo desde el botón de Avanzado. Lo gestiona AICore y es compartido por todo el sistema.
- **No probado en el emulador:** el emulador no tiene AICore (`checkStatus` → `UNAVAILABLE`, comprobado en el log).

## ML Kit (motor universal)

- **OCR sin agrupar** (`play-services-mlkit-text-recognition` + `-chinese`) e **identificación de idioma sin agrupar** (`play-services-mlkit-language-id`): los modelos los gestiona Google Play Services, no engordan el APK y Play los instala junto con la app (`com.google.mlkit.vision.DEPENDENCIES = ocr,ocr_chinese,langid`). En instalaciones sueltas (APK) se instalan con `ModuleInstallClient` al abrir la configuración, o en la primera traducción, mostrando «Preparando el reconocimiento de texto…».
- **Dos reconocedores en paralelo** (latino y chino): el latino es el más preciso con acentos y ß, y el chino también lee CJK. Se usa el chino si al menos el 30 % de las letras son CJK. Así no hay que saber el idioma antes del OCR.
- **Filtro de interfaz por posición y tamaño** (`ScreenText`, en `:core`, con tests): fuera la franja superior del 4,5 % (barra de estado) y la inferior del 4,5 % (navegación); los textos de 3 palabras o menos y una línea en el 12 % superior o el 25 % inferior (barras de herramientas, pestañas, botones); las etiquetas cortas pegadas a un lateral; los restos de iconos (2 caracteres o menos), y los **textos cortos (8 palabras o menos) en la zona inferior separados del contenido por un hueco de más de 3 líneas** (banners superpuestos, como el de cookies). No se usan listas de palabras clave, como pedía la tarea.
- **Párrafos:** bloques ordenados de arriba abajo. Un bloque se une al anterior si está a menos de 0,6 líneas, alineado (±5 % del ancho) y con la misma altura de letra (±25 %). Se deshacen los guiones de corte de palabra y el chino se une sin espacios. Cada párrafo se traduce por separado y se muestra en cuanto está listo, como un streaming por párrafos.
- **Idioma:** se identifica el idioma dominante de todo el texto. Si coincide con el de destino, se muestra el texto con el aviso «El texto ya está en español». Las páginas con varios idiomas se traducen según el dominante (limitación conocida).
- **Paquetes de idioma (unos 30 MB cada uno):** el del idioma de destino se pide en la configuración inicial con `requireWifi()`. Si no hay wifi, la pantalla lo dice y ofrece «Descargar ahora con datos». Los de origen se descargan sin esperar al wifi la primera vez que se detectan, porque el usuario acaba de pedir la traducción, mostrando «Descargando el idioma alemán (unos 30 MB, solo la primera vez)…» en la ventana de resultado. Se pueden borrar en Avanzado → «Idiomas descargados». El inglés viene incluido y ni se descarga ni se borra.
- **Atribución obligatoria:** se muestra la insignia oficial «powered by Google Translate» (descargada del zip oficial de Google Cloud, en color con tema claro y en blanco con tema oscuro) **junto al resultado, solo cuando ha traducido ML Kit**. El aviso legal de Google (*THIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE…*) está en Avanzado → «Aviso legal de la traducción». Con Nano y con Gemini en la nube no se muestra nada, porque la SPEC pide no mostrar información de Gemini en cada traducción.

## Tamaño de la app

- La traducción local de ML Kit añade unos **16 MB de código nativo por arquitectura**. El APK universal de release pasaba de 2,3 a 67 MB.
- **Solución:** Play (AAB) ya entrega a cada móvil solo su arquitectura. Para los APK sueltos se generan **APK por arquitectura** (arm64-v8a 20,5 MB, armeabi-v7a 15,7 MB, x86_64 21,5 MB), y se deja fuera x86 de 32 bits (solo emuladores antiguos). Como AGP no permite APK por arquitectura y AAB en la misma ejecución (issuetracker 402800800), la división se desactiva automáticamente al ejecutar `bundle*`.

## Configuración inicial sin clave

- Primera apertura: se piden los permisos automáticamente, se pide el paquete del idioma de destino (con wifi), se instalan los modelos de OCR y, si el móvil tiene Nano descargable y hay wifi, se descarga Nano. El usuario solo elige el idioma (por defecto, el del sistema) y acepta los permisos.
- La clave de Gemini pasa a Avanzado → «Máxima calidad: Gemini en la nube», junto al campo del modelo. La clave se sigue guardando cifrada con Keystore y solo si la prueba de conexión pasa.
- **Privacidad:** el texto dice que, con los motores locales, la captura no sale del móvil; que solo se descargan paquetes de idioma y modelos, y que el modo en la nube envía la imagen a Google (con el aviso del nivel gratuito).
- Solo en depuración: `adb shell am start -n app.kitra/.ui.setup.SetupActivity --es debug_engine mlkit|nano|cloud|auto` fuerza un motor para las comparativas.
