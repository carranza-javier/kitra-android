# Informe final: fase 1 de Kitra (Android)

Fecha: 17-10-2026 · Emulador: Pixel, Android 16 (API 36), Google Play · Modelo: `gemini-3.5-flash-lite`

## 1. Qué está implementado y funciona

Toda la fase 1 de la SPEC (§11), probada de verdad en el emulador, también con la clave real de Gemini:

| Paso | Estado | Comprobado en el emulador |
|---|---|---|
| 1. Proyecto (`:app` Android + `:core` Kotlin puro, portable a iOS) | ✅ | Compila en debug y release (R8). |
| 2. Configuración inicial: clave con prueba de conexión y cifrada con Keystore, idioma y permisos | ✅ | Clave real validada y guardada cifrada. Clave inválida → «no válida». Modo avión → «sin conexión». Permisos pedidos solos tras validar la clave. Acceso parcial a fotos explicado. |
| 3. Pipeline: reducción a 1600 px + JPEG, `streamGenerateContent` (SSE) y prompt de la §4.2 | ✅ | Captura de ~1,2 MB en PNG → unos 60 KB. El texto llega en streaming. |
| 4. Entrada por compartir (`ACTION_SEND image/*`) + Direct Share | ✅ | `am start -a SEND` con imágenes de, fr, it y en. |
| 5. Detección de capturas + aviso heads-up «Traducir» | ✅ | **Captura real del sistema (`KEYCODE_SYSRQ`)** → heads-up visible → tocar «Traducir» → traducción. También copiando una imagen a `Pictures/Screenshots` y forzando el escaneo. Una imagen en `Download` **no** genera aviso. |
| 6. Vista de resultado: streaming, Copiar y Cerrar | ✅ | Hoja inferior sobre la app anterior. «Copiar» → «Copiado». «Cerrar», tocar fuera o Atrás devuelven a la app anterior. |
| 7. Errores y temporales | ✅ | Cerrar a mitad de traducción cancela la petición. Caché vacía tras cerrar. Las capturas originales no cambian (mismo tamaño y fecha). Sin clave → mensaje + «Abrir configuración». |

### Mediciones reales (emulador, red doméstica)

| Medida | Resultado |
|---|---|
| Botón de captura → el sistema termina de guardar la captura | ~1,5 s (lo hace Android, no Kitra) |
| Captura guardada → aviso de Kitra publicado | **0,15–0,5 s** |
| Botón de captura → banner heads-up visible | ~2 s |
| Pulsar «Traducir» → imagen preparada | 0,25–0,6 s |
| Pulsar «Traducir» → **primer texto traducido** (Gemini real) | **1,5–3,7 s** (mediana ≈ 2 s) |
| Pulsar «Traducir» → traducción completa (artículo corto) | 1,8–4,4 s |
| Acciones del usuario | **Captura + 1 toque** (+ 1 toque para copiar y otro para cerrar) |

### Calidad (Gemini real, al español)

- **Interfaz ignorada:** hora, batería, nombre del periódico, menú, banner de cookies, botón «OK» y controles de Google Photos (Share/Edit/Lens/Trash). Nada de eso aparece en la traducción.
- **Expresiones adaptadas, no literales:** *Schildbürgerstreich* → «una estupidez», *tirer le diable par la queue* → «habituado a las estrecheces», *prendere due piccioni con una fava* → «matar dos pájaros de un tiro», *over the moon* → «más que encantados».
- Respeta titular y párrafos, sin añadir comentarios ni resúmenes.

## 2. Decisiones importantes

Todas están detalladas en [`DECISIONES.md`](DECISIONES.md). Las principales:

- **Sin KMP por ahora:** módulo `:core` en Kotlin puro, sin dependencias de Android (coroutines + serialization), con un `HttpTransport` abstracto. Se podrá pasar a KMP en la fase 3 con pocos cambios.
- **JobScheduler con content URI, no foreground service:** el aviso sale menos de 0,5 s después de que Android guarda la captura. No compensa un servicio permanente que gasta batería y muestra una notificación fija.
- **Modelo `gemini-3.5-flash-lite`:** Google lo recomienda para proyectos nuevos, es el más rápido, admite imágenes, tiene nivel gratuito y la calidad comprobada es buena. Se puede cambiar en «Avanzado» (por ejemplo, a `gemini-3.8-flash`). Las dos opciones están disponibles para tu clave (comprobado).
- **Todo en memoria:** la imagen reducida nunca se escribe en disco, así que no hay temporales que limpiar (aun así se vacía la caché).
- **Aviso silencioso, con un id fijo y que desaparece a los 8 s:** no suena, no vibra y no se acumula, pero sigue apareciendo como heads-up.
- **Privacidad:** se avisa de forma destacada de que, en el nivel gratuito, Google usa el contenido y puede revisarlo una persona (verificado en las condiciones actuales). Los logs solo contienen tiempos y tamaños. Se revisó el `logcat` tras las pruebas reales: 0 apariciones de la clave y 0 de contenido traducido. La clave tampoco aparece en ningún archivo del repositorio.
- **Prompt iterado con pruebas reales** (2 problemas encontrados y corregidos; ver DECISIONES.md).

## 3. Lo que no era viable tal como estaba especificado

| Especificado | Realidad | Alternativa |
|---|---|---|
| Añadir un «tercer botón» a la combinación de captura o a su miniatura | Android no lo permite | Detectar la captura nueva en MediaStore y mostrar un aviso heads-up «Traducir» (SPEC §3.1). |
| Detectar capturas con acceso parcial a fotos (Android 14+) | La app no ve las fotos nuevas | Mensaje claro en la configuración + botón a los ajustes + «Compartir» como respaldo. |
| Botón flotante (`SYSTEM_ALERT_WINDOW`) | Viable, pero necesita un permiso especial que se activa en los ajustes y Play lo restringe | No implementado en la fase 1. La notificación aparece en el mismo sitio y momento. Se evaluará en la fase 2. |
| OCR local previo (ML Kit) | Viable | No implementado: el modelo multimodal ya recibe una imagen de unos 60 KB y responde en unos 2 s. Se medirá en la fase 2. |
| Prueba en el móvil físico | No tengo acceso | Pasos preparados en la sección 6. |

## 4. Compilación y tests

- `./gradlew test assembleDebug assembleRelease lintDebug` → **BUILD SUCCESSFUL**.
- **27 tests unitarios, 0 fallos:** cliente de Gemini (streaming, endpoint, cabecera de la clave, cuerpo de la petición, partes de razonamiento ignoradas, clave inválida, cuota, 5xx, modelo inexistente, sin red, corte a mitad de stream, bloqueo, error dentro del stream, cancelación, verificación, nombre de modelo malicioso), tamaño de imagen, filtro de capturas, prompt y proveedor falso.
- Lint: 0 errores y 2 avisos aceptados (`targetSdk 36` y `fullBackupContent`, que no aplica con `allowBackup=false`).
- Versiones: Kotlin 2.4.21, AGP 9.4.1, Gradle 9.8.1, compileSdk 37, minSdk 29.

## 5. Problemas pendientes y riesgos

1. **Política de Google Play sobre fotos (riesgo alto para publicar):** Play solo permite `READ_MEDIA_IMAGES` a apps cuya función principal requiera acceso amplio a fotos (galerías, etc.). Detectar capturas para traducirlas no encaja claramente y habría que declararlo y justificarlo en Play Console, con riesgo de rechazo. **Para uso personal con el APK instalado directamente no hay problema.** Si se quiere publicar, «Compartir» funciona sin ese permiso.
2. **Nivel gratuito de Gemini:** Google puede usar las capturas enviadas y revisarlas una persona. Está avisado en la app. Para contenido sensible conviene activar la facturación.
3. **Cuotas:** los límites del nivel gratuito ya no aparecen en la documentación, solo en AI Studio, y pueden cambiar.
4. **Fabricantes con ahorro de batería agresivo** (Xiaomi, Huawei…) pueden retrasar el job. Se medirá en tu móvil (fase 2).
5. Si revocas y vuelves a conceder el permiso de fotos desde los ajustes del sistema, la detección se reactiva al abrir Kitra una vez o al reiniciar el teléfono.
6. La miniatura de captura del sistema queda unos segundos encima de la hoja de Kitra. Es parte de la interfaz de Android y no se puede controlar.

## 6. Pasos para probarlo en tu móvil

1. Compila: `./gradlew assembleDebug` (o usa `app/build/outputs/apk/debug/app-debug.apk`).
2. En el móvil: **Ajustes → Opciones de desarrollador → Depuración USB** y conéctalo. Luego: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Abre **Kitra**, pega tu clave y pulsa **Guardar y probar** → «Clave guardada y funcionando».
4. Acepta los permisos: **Notificaciones** y **Fotos y vídeos → «Permitir todo»** (no «acceso limitado»).
5. Comprueba que el idioma de destino es **Español**.
6. Abre un periódico suizo en el navegador, haz una captura con **encendido + bajar volumen** y pulsa **«Traducir»** en el aviso.
7. Comprueba: la traducción aparece en streaming, **Copiar** funciona, **Cerrar** te devuelve al navegador y la captura sigue en tu galería.
8. Prueba también **Compartir → Kitra** desde la galería.
9. Para la fase 2, mide los tiempos con `adb logcat -s Kitra` (solo muestra tiempos y tamaños).
10. Si el aviso no aparece: revisa que «Aviso al hacer una captura» esté activado en Kitra, que las notificaciones de Kitra estén permitidas y, en marcas como Xiaomi o Huawei, desactiva la optimización de batería para Kitra.
