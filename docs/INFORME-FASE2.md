# Informe de la fase 2: traducción en el dispositivo, sin clave

Fecha: 17-10-2026 · Rama: `feature/on-device-translation` (**sin fusionar** con `main`) · Emulador: Pixel, Android 16 (API 36), Google Play

## 1. Qué se ha hecho

Kitra **traduce sin clave de API desde la primera vez que se abre**. El flujo de captura → aviso «Traducir» → ventana de resultado no cambia.

| Requisito | Estado | Cómo se ha comprobado |
|---|---|---|
| Motor 1: **Gemini Nano** (Prompt API de ML Kit GenAI, con la captura como imagen y el prompt adaptado) | ✅ Implementado, ⚠️ **sin probar** | El emulador no tiene AICore: `checkStatus()` → `UNAVAILABLE` y Kitra pasa a ML Kit (visto en el log). La prueba queda preparada para tu móvil (sección 5). |
| Motor 2: **ML Kit**: OCR v2 + idioma + traducción local, con filtro de interfaz por posición y tamaño, y por párrafos | ✅ | Probado en el emulador con de, fr, it, en y zh. |
| Motor 3: **Gemini en la nube**, opcional, en Avanzado como «Máxima calidad» | ✅ | Probado con tu clave y las mismas imágenes. |
| Selección automática (Nano → ML Kit), sin que el usuario elija nada | ✅ | Tests de `EngineSelection` y `FallbackTranslationProvider`, y el log del emulador. |
| Paquete del idioma de destino en la configuración inicial, con wifi | ✅ | Instalación limpia: «Descargando el idioma…» → «Listo para traducir sin conexión» en unos 12 s. |
| Paquetes de origen bajo demanda, con el progreso en la ventana de resultado | ✅ | «Descargando el idioma alemán (unos 30 MB, solo la primera vez)…» y después la traducción. |
| Borrar paquetes desde ajustes | ✅ | Avanzado → «Idiomas descargados» → «Borrar». |
| Configuración inicial sin clave (solo idioma y permisos) | ✅ | Instalación limpia: los permisos se piden solos y no aparece ninguna pantalla de clave. |
| Atribución de la traducción local de ML Kit | ✅ | Insignia oficial «powered by Google Translate» junto al resultado (solo con ML Kit) + aviso legal de Google en Avanzado. |
| Textos de privacidad: con los motores locales, la captura no sale del móvil | ✅ | Actualizados en la configuración. |

### Prueba completa en el emulador desde una instalación limpia, sin clave

1. `adb uninstall` → instalar → abrir Kitra: se piden los permisos solos (notificaciones y fotos → «Permitir todo»).
2. Elegir **Español** → el paquete se descarga solo (unos 12 s) → «Listo para traducir sin conexión».
3. Abrir un artículo en alemán en Google Photos → **captura real del sistema** (`KEYCODE_SYSRQ`) → aviso «Traducir» → un toque.
4. La ventana muestra «Leyendo la captura…» y «Descargando el idioma alemán…» y luego la traducción con «powered by Google Translate». **Primera vez: 35 s** (incluye descargar el alemán). **Siguientes: 3,5 s hasta el primer texto y 4,3 s en total**, en el emulador antes de que se cayera (ver la nota de tiempos).
5. «Cerrar» → vuelve a Google Photos.

## 2. Comparativa de motores (mismas imágenes → español)

Imágenes: `testdata/screenshot_{de,fr,it,en,zh}.png` (artículos ficticios con barra de estado, cabecera de la web, banner de cookies y botón).

> ⚠️ **Los tiempos del emulador no son representativos de un móvil real.** El emulador no tiene aceleración de ML ni NPU, se cayó a mitad de las pruebas (fallo gráfico del PC) y, tras un arranque en frío, iba bastante más lento: el mismo caso de ML Kit pasó de 3,5 s a 7,8 s. En la nube, el tiempo depende sobre todo de la red y del servidor.

### Tiempo hasta el primer texto (ms) y total, con los paquetes ya descargados

| Imagen | Gemini en la nube (2 ejecuciones) | ML Kit, emulador después de la caída | ML Kit, emulador antes de la caída | Gemini Nano |
|---|---|---|---|---|
| de | 3270 / 3067 (total 3,2–3,5 s) | 7752 (9,9 s) | **3484 (4,3 s)** | no disponible |
| fr | 2391 / 2700 (2,7–2,9 s) | 6332 (8,2 s) | — | no disponible |
| it | 2075 / 1880 (2,1–2,2 s) | 6678 (8,9 s) | — | no disponible |
| en | 2953* (3,0 s) | 5768 (6,9 s) | — | no disponible |
| zh | 5056 / 2496 (2,8–5,7 s) | 5504 (9,2 s) | — | no disponible |

\* La primera ejecución en inglés falló por un corte de red del emulador. Como el motor en la nube estaba forzado para la prueba, se mostró el error «Sin conexión». En modo automático, Kitra habría pasado a ML Kit con el aviso «traducido en el dispositivo». El reintento dio 6,5 s.

**Primera vez de cada idioma con ML Kit** (incluye descargar el paquete, depende de la red): alemán 35 s, francés 30 s, italiano 82 s. Solo ocurre una vez por idioma.

### Calidad, con ejemplos concretos

| Frase original | Gemini en la nube | ML Kit (en el teléfono) |
|---|---|---|
| de: *Stadtrat will **Velowege** bis 2030 verdoppeln* | «El Consejo Municipal quiere duplicar los **carriles bici** hasta 2030» | «Ayuntamiento quiere doblarse **velocegia** hasta 2030» |
| de: *Kritiker sprechen von einem **Schildbürgerstreich*** | «hablan de una **ocurrencia absurda**» | «hablan de una **bragas de tortugas**» |
| de: *…von einem längst überfälligen Schritt* | «un paso que hace tiempo que debería haberse dado» | «defienden de una larga ciencia atrasada» |
| fr: *un pêcheur qui a l'habitude de **tirer le diable par la queue*** | «un pescador **habituado a las penurias**» | «a un pescador que está acostumbrado a **dibujar al diablo por la cola**» |
| it: *«Abbiamo preso **due piccioni con una fava**»* | «Hemos **matado dos pájaros de un tiro**» | «Tomamos **dos palomas con una FAVA**» |
| it: *I lavori si sono conclusi…* | «Las obras han concluido…» | «**T** Trabajo finalizó…» (error de OCR) |
| en: *the owners are **over the moon*** | «afirman estar **entusiasmadísimos**» | «dicen que están **sobre la luna**» |
| en: *after years of **burning the midnight oil*** | «tras años de trabajar hasta altas horas de la noche» | «después de años de **quemar el aceite de medianoche**» |
| zh: *上海地铁新线提前三个月开通* | (titular correcto) | «**Shanghai Metro New Line** abrió tres meses de anticipación» (partes en inglés: ML Kit traduce pasando por el inglés) |
| zh: *这下真是**一举两得*** | «Esto sí que es **matar dos pájaros de un tiro**» | «Esto es realmente **dos**» |
| zh: *首班车* | «el primer **tren**» | «el primer **autobús**» |
| Interfaz (barra de estado, cabecera, cookies, «OK») | Ignorada siempre | Barra de estado, botones y **banner de cookies** ignorados gracias a las reglas de posición. **Se cuela la cabecera** cuando tiene más de 3 palabras («Gazette Lausanne»). |

**Resumen de calidad:** ML Kit sirve para entender de qué va un texto sencillo. Las frases literales salen bien («La inauguración está programada para el sábado», «Los expertos piden precaución»), pero **falla en lo que Kitra quiere hacer mejor**: expresiones, contexto, palabras regionales (*Velowege*) y chino. Gemini en la nube mantiene la calidad de la fase 1. **La calidad de Gemini Nano no se ha podido medir**: Google solo valida oficialmente el inglés y el coreano en la Prompt API, así que hay que probarla en tu móvil.

### Descarga y tamaño

| | Gemini en la nube | ML Kit | Gemini Nano |
|---|---|---|---|
| Tamaño de la app | Incluido en la base (la app de la fase 1 pesaba 2,3 MB) | **+16 MB de código nativo** de traducción. APK arm64: **20,5 MB** (antes 2,3 MB). AAB para Play: 32,5 MB, con unos 20 MB por móvil. | Unos KB (biblioteca cliente) |
| Modelos | Ninguno | **Paquete de idioma: unos 30 MB de descarga y 43–46 MB en disco** (medido: alemán 45,7, español 44,5, francés 44,4, chino 44,3, italiano 43,0 MB). OCR e identificación de idioma los gestiona Google Play Services (compartidos, sin coste para la app). | Lo gestiona AICore, compartido por todo el sistema. En muchos móviles compatibles ya viene instalado. No medible en el emulador. |
| Red al traducir | Siempre (envía la captura) | Solo la primera vez de cada idioma | Ninguna |

### Compatibilidad

| | Gemini en la nube | ML Kit | Gemini Nano |
|---|---|---|---|
| Móviles | Cualquiera con internet y clave | **Cualquier Android 10+ con Google Play Services** (los móviles sin servicios de Google, como los Huawei recientes, no tienen OCR) | Solo los de la lista oficial: Pixel 9/10/11, Galaxy S25/S26, Z Fold7/8, Z Flip8, OnePlus 13/15, Xiaomi 15/17, OPPO Find X8/X9, vivo X200/X300… |
| Idiomas | Todos | Más de 50 para traducir, pero el OCR solo lee los alfabetos latino y chino tal como está configurado | Validados oficialmente: inglés y coreano. El resto, sin garantía. |
| Restricciones | Cuota de tu clave | Ninguna | Solo con la app en primer plano, cuota por app, cuota diaria de batería, no funciona con el bootloader desbloqueado. Es una API en beta. |
| Privacidad | La captura va a Google (en el nivel gratuito, Google puede usarla) | La captura no sale del teléfono | La captura no sale del teléfono |

## 3. Problemas y riesgos

1. **La calidad de ML Kit es claramente inferior** a la de la fase 1 (ver los ejemplos). Por defecto, sin clave y sin Nano, Kitra traduce peor que antes, a cambio de no necesitar nada.
2. **Gemini Nano sin probar:** es una API en beta, solo valida oficialmente inglés y coreano, y no se sabe cómo traduce al español a partir de una imagen. Si falla en tu móvil, Kitra pasa a ML Kit sin que lo notes.
3. **Tamaño:** de 2,3 a unos 20 MB de app, más unos 45 MB en disco por idioma.
4. **OCR sin servicios de Google:** en móviles sin Google Play Services, ML Kit no puede leer la captura. Con Gemini en la nube activado sí funciona.
5. **El filtro de interfaz es heurístico:** quita la barra de estado, la navegación, los botones y los banners aislados, pero se cuelan cabeceras de más de 3 palabras y textos de interfaz en mitad de la pantalla. Los textos con varias columnas se leen de arriba abajo, mezclando las columnas.
6. **Páginas con varios idiomas:** ML Kit traduce según el idioma dominante.
7. **Emulador:** se cayó durante las pruebas por un fallo gráfico del PC. Se recuperó con un arranque en frío. No afecta a la app.

## 4. Compilación y tests

- `./gradlew test assembleDebug assembleRelease lintDebug` + `./gradlew bundleRelease` → **BUILD SUCCESSFUL**.
- Lint: **0 errores** y 3 avisos aceptados: `targetSdk 36` y `fullBackupContent` (heredados de la fase 1) y la falta de la versión hdpi de la insignia de Google, que no viene en el zip oficial y Android escala desde la xhdpi.
- **48 tests unitarios en `:core`** (antes 27): selección de motor y recurso al siguiente (10), filtro de pantalla para el OCR (10), cliente de Gemini (15), prompt (4), filtro de capturas (5) y tamaño de imagen (4).
- APK por arquitectura: `app-arm64-v8a-release` 20,5 MB, `app-armeabi-v7a-release` 15,7 MB, `app-x86_64-release` 21,5 MB. AAB: 32,5 MB.

## 5. Qué hay que probar en tu móvil (sobre todo, Gemini Nano)

**Primero:** ¿qué modelo de móvil tienes? Si está en la lista de la sección 2 (Pixel 9 o posterior, Galaxy S25/S26, etc.), tiene Gemini Nano.

1. Compila la rama e instala el APK de tu arquitectura (casi seguro arm64):
   ```
   git switch feature/on-device-translation
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
   ```
2. Abre Kitra → **Avanzado → Motor de traducción**. Debe aparecer uno de estos:
   - «Gemini Nano: disponible» → Kitra ya lo usa.
   - «Gemini Nano: se puede descargar» → pulsa «Descargar Gemini Nano» con wifi y espera.
   - «Gemini Nano: no disponible en este teléfono» → tu móvil usará ML Kit.
3. Copia las imágenes de prueba al móvil (`adb push testdata/screenshot_*.png /sdcard/Download/`), ábrelas en la galería, haz una captura y pulsa «Traducir». Mientras tanto, mira los tiempos con `adb logcat -s Kitra` (solo tiempos y motor, nunca el texto).
4. **Compara los tres motores** en tu móvil (solo en la versión debug):
   ```
   adb shell am start -S -n app.kitra/.ui.setup.SetupActivity --es debug_engine nano
   adb shell am start -S -n app.kitra/.ui.setup.SetupActivity --es debug_engine mlkit
   adb shell am start -S -n app.kitra/.ui.setup.SetupActivity --es debug_engine cloud   # con clave en Avanzado
   adb shell am start -S -n app.kitra/.ui.setup.SetupActivity --es debug_engine auto    # volver a lo normal
   ```
   Fíjate en las mismas frases de la tabla de calidad (*Schildbürgerstreich*, *tirer le diable par la queue*, *due piccioni con una fava*, 一举两得) y en si Nano ignora la barra de estado y el banner de cookies.
5. **Modo avión:** con ML Kit (y con Nano si lo tienes), traduce una captura sin conexión. Debe funcionar si el idioma ya está descargado.
6. Prueba con capturas reales: tu periódico suizo, WhatsApp, un menú…
7. Mira el tamaño en Ajustes → Apps → Kitra (app más datos).

## 6. Recomendación

1. **Fusionar la rama, pero con Gemini en la nube muy a mano.** Sin clave, Kitra ya sirve a tus amigos desde el primer minuto y la captura no sale del móvil. Eso es imprescindible para Play y para que la usen sin configurar nada. Pero ML Kit traduce claramente peor que Gemini, justo en lo que diferencia a Kitra.
2. **Decide según tu móvil:** si tiene Gemini Nano y su calidad al español es buena, es el mejor motor por defecto (calidad cercana a la nube, privado y gratis). Si no la tiene o traduce mal, el motor por defecto para la mayoría será ML Kit.
3. **Mejora que propongo, sin implementar:** un enlace discreto «Mejorar con Gemini» en la ventana de resultado cuando ha traducido ML Kit, que lleve directamente a activar «Máxima calidad». Así, quien quiera la calidad de la fase 1 la tiene a dos toques, sin que nadie tenga que ir a buscarla.
4. Para ti mismo: deja «Máxima calidad» activada con tu clave. Kitra pasará al dispositivo si no hay red o se agota la cuota.
