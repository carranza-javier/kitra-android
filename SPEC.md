# Kitra: traducción instantánea de capturas de pantalla

## 1. Visión del producto

La aplicación se llama **Kitra**. Este nombre debe usarse en el nombre visible de la app, en el proyecto, en los avisos y en la documentación.

Quiero crear una aplicación móvil que permita traducir prácticamente cualquier texto visible en la pantalla del teléfono, con el mínimo esfuerzo posible, utilizando un LLM como Gemini para conseguir traducciones naturales y contextuales.

**La prioridad absoluta del producto es que traducir no suponga ningún esfuerzo.** Cualquier decisión de diseño, arquitectura o alcance se subordina a esto.

No quiero crear otro traductor convencional. Ya existen traductores y herramientas del sistema (Google Lens, Circle to Search, Live Text en iOS) que traducen la pantalla. La diferencia de esta aplicación está en dos cosas:

1. **Calidad:** traducciones naturales y contextuales hechas por un LLM, que entienden el tono, las expresiones coloquiales y la intención del texto, no traducciones literales.
2. **Esfuerzo cero:** traducir algo que estás leyendo debe ser tan fácil como hacer una captura de pantalla y pulsar una sola vez.

El usuario no debería tener que:
- Abrir manualmente una aplicación de traducción.
- Copiar y pegar texto.
- Seleccionar el texto que quiere traducir.
- Buscar una imagen en la galería.
- Pasar por el menú de compartir y elegir una aplicación.
- Elegir el idioma en cada operación.
- Configurar opciones en cada traducción.

### Ejemplo de uso principal

Estoy leyendo un periódico suizo en alemán o francés desde el móvil. Encuentro un titular interesante.

1. Hago una captura de pantalla normal con los botones físicos del teléfono (encendido + bajar volumen), como hago siempre.
2. Aparece inmediatamente un aviso de Kitra con la opción "Traducir".
3. Lo pulso una vez.
4. Veo la traducción al español en una vista ligera.
5. Copio, cierro y sigo leyendo.

La aplicación debe integrarse en el flujo natural de uso del móvil. No debe obligar al usuario a cambiar sus hábitos. La captura nativa con botones físicos es un hábito ya existente, preciso y fiable, y es el punto de partida de la experiencia.

## 2. Principio fundamental: esfuerzo cero

**Objetivo: captura nativa + una sola pulsación = traducción.**

Preferencias del usuario respecto a la interacción:
- Se prefieren **botones físicos y pulsaciones claras** frente a gestos (deslizar, dibujar, tocar la parte trasera del teléfono), porque los gestos pueden ser imprecisos.
- La traducción debe iniciarse automáticamente tras esa única pulsación. Nunca debe pedirse una confirmación adicional.
- La vista de resultado debe cerrarse con una sola acción y devolver al usuario a lo que estaba haciendo.

Si una función no está permitida por el sistema operativo, hay que implementar la alternativa con menos esfuerzo posible, no intentar eludir sus restricciones.

El indicador principal de éxito es el número de acciones que necesita el usuario desde que ve un texto interesante hasta que puede leerlo en su idioma, seguido del tiempo que tarda en aparecer la traducción.

## 3. Plataformas

El proyecto debe contemplar Android e iOS. Mi dispositivo es Android y el de mi pareja es un iPhone. Primero Android, después iOS.

### 3.1 Android: flujo principal (detección de capturas)

Restricción real del sistema: una aplicación normal **no puede interceptar** la combinación de botones de captura ni los botones de volumen de forma global. Tampoco puede añadir botones a la miniatura de captura que muestra el sistema. Por tanto, el "tercer botón" se implementa así:

1. El usuario hace una captura nativa.
2. La aplicación **detecta que se ha creado una nueva captura** en el almacenamiento del dispositivo.
3. La aplicación muestra inmediatamente un **aviso emergente (notificación heads-up) con una acción "Traducir"**.
4. Al pulsarlo, se abre la vista de resultado y la traducción empieza sola.

Detalles técnicos a investigar e implementar:
- Detección mediante `JobScheduler` con `addTriggerContentUri` sobre `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (no requiere un servicio permanente). Comparar con un `ContentObserver` dentro de un foreground service si la latencia del job no es aceptable. Medir ambas opciones en dispositivo real y elegir con datos.
- Identificar capturas filtrando por ruta relativa (carpetas tipo `Screenshots` en `Pictures` o `DCIM`, que varían según fabricante) y por fecha de creación reciente. Ignorar cualquier otra imagen.
- Permisos: `READ_MEDIA_IMAGES` (Android 13+) y `POST_NOTIFICATIONS`. Gestionar el caso de acceso parcial a fotos de Android 14+ ("fotos seleccionadas"), en el que la aplicación no verá las capturas nuevas, y explicárselo al usuario con un mensaje claro.
- Canal de notificación de alta importancia para que el aviso aparezca como heads-up. El aviso debe desaparecer solo al cabo de unos segundos si no se usa, sin acumularse en la bandeja.
- Pulsar el aviso no debe borrar ni mover la captura original del usuario, que es suya y está en su galería.
- Riesgo a verificar: la política de Google Play sobre permisos de fotos y vídeos restringe el acceso amplio a imágenes. Comprobar si este caso de uso es aceptable para publicar en Play. Para uso personal (APK instalado directamente) no es un problema.
- Alternativa a evaluar frente a la notificación: un botón flotante temporal ("Traducir") mediante permiso de superposición (`SYSTEM_ALERT_WINDOW`) que aparece unos segundos tras la captura. Solo si es claramente más cómodo y rápido que la notificación.

### 3.2 Android: entrada por compartir (respaldo)

La aplicación también debe registrarse como destino para compartir imágenes (`ACTION_SEND` con `image/*`). Es el flujo más sencillo de implementar y sirve para:
- Probar de extremo a extremo el pipeline de traducción antes de tener la detección de capturas.
- Traducir imágenes que no son capturas.
- Funcionar cuando el usuario no ha concedido el permiso de fotos.

Si es posible, ofrecer la aplicación como destino de Direct Share para que aparezca en primera fila.

### 3.3 Android: vías avanzadas (fase 2, evaluar)

- **Aplicación de asistente digital (`VoiceInteractionService`):** si el usuario la elige como asistente predeterminado, al **mantener pulsado el botón de encendido** (o inicio, según el teléfono) el sistema le entrega una captura de la pantalla actual, si está activada la opción de usar captura de pantalla. Es una sola pulsación de un botón físico, sin captura previa. Inconveniente: sustituye al asistente actual del usuario (Gemini/Google Assistant). Evaluar viabilidad y ofrecerlo como opción, nunca forzarlo.
- **Servicio de accesibilidad:** permitiría capturar la pantalla (`takeScreenshot`, API 30+) y escuchar botones de volumen (por ejemplo, pulsación larga de bajar volumen). Google Play restringe el uso de accesibilidad para fines que no son de accesibilidad, así que solo se contempla como variante experimental para instalación directa, en un módulo o *flavor* separado que no afecte a la versión publicable.
- **Traducción automática de toda captura (opcional, desactivada por defecto):** elimina incluso la pulsación del aviso, pero envía a Google cada captura que haga el usuario. Solo con aviso de privacidad explícito y consentimiento claro.

No asumir que una aplicación puede capturar silenciosamente otras aplicaciones ni interceptar combinaciones de teclas arbitrarias sin estas vías.

### 3.4 iOS

Restricciones reales: iOS no permite que una aplicación detecte capturas hechas en otras aplicaciones ni intercepte botones físicos.

Vías previstas, por orden de prioridad:
1. **Atajo de Atajos (Shortcuts) + App Intent:** la acción del sistema "Hacer captura de pantalla" seguida de un App Intent de la aplicación que recibe la imagen, traduce y muestra el resultado (idealmente en un snippet sin abrir la app completa). El usuario puede lanzarlo desde el botón de Acción (iPhone 15 Pro y posteriores, botón físico, preferente), desde Toque posterior (gesto, alternativa) o desde Siri.
2. **Extensión de compartir:** captura nativa, pulsar la miniatura, compartir, elegir la aplicación.

Verificar qué modelo de iPhone tiene mi pareja para decidir la vía principal. La experiencia debe ser lo más parecida posible a Android.

## 4. Motor de traducción: Gemini API

Gemini será el proveedor inicial. No se utilizará un traductor convencional como motor principal.

La captura se envía directamente a un modelo Gemini multimodal. Evaluar también el OCR local previo (por ejemplo, ML Kit) y elegir la estrategia con mejor equilibrio entre velocidad, calidad y coste, basándose en mediciones reales.

### 4.1 Optimización de la petición

- **Reducir y comprimir la imagen** antes de enviarla (resolución y calidad suficientes para que el texto siga siendo legible). Menos tokens y menos latencia.
- **Streaming:** usar `streamGenerateContent` y mostrar el texto a medida que llega. Reduce mucho la latencia percibida y forma parte del MVP.
- No realizar llamadas duplicadas para una misma captura.

### 4.2 Instrucciones de traducción (prompt de sistema)

El modelo debe:
- Detectar automáticamente el idioma de origen.
- Traducir al idioma de destino configurado.
- **Ignorar los elementos de interfaz** que no forman parte del contenido: barra de estado, hora, batería, notificaciones, barras de navegación, menús, botones, anuncios, banners de cookies. Traducir el contenido principal que el usuario está leyendo.
- Producir una traducción natural, no literal.
- Conservar significado, contexto, tono y expresiones idiomáticas.
- Mantener correctamente nombres propios, cifras, fechas y referencias.
- Respetar la estructura del contenido (titulares, párrafos, listas) siempre que sea posible.
- No añadir explicaciones, resúmenes ni comentarios.
- No inventar texto ilegible ni completar partes que no aparezcan en la captura. Si hay fragmentos ilegibles, indicarlo de forma breve.
- Si el texto ya está en el idioma de destino, indicarlo brevemente en lugar de "traducirlo".
- En artículos periodísticos, traducir el texto visible, nunca resumir la noticia.

La salida predeterminada contiene únicamente el texto traducido.

El prompt debe vivir en un único lugar del código, compartible entre plataformas, y debe poder iterarse sin tocar el resto de la aplicación.

### 4.3 Elección del modelo

La rapidez es esencial, pero no se debe sacrificar la calidad usando un modelo excesivamente limitado. Comprobar en la documentación oficial actual los modelos disponibles con entrada de imagen, sus cuotas gratuitas, latencia y condiciones antes de fijar el predeterminado.

El modelo debe poder cambiarse por configuración sin reescribir la aplicación.

## 5. Coste: cada usuario usa su propia clave (BYOK)

Cada usuario configura su propia clave de Gemini API. No habrá una API centralizada pagada por mí.

El objetivo es que desarrollador y usuarios puedan usar la aplicación sin coste de inferencia mientras las peticiones estén dentro de la cuota gratuita. No se debe prometer que será gratuito para siempre: límites, modelos y condiciones de Google pueden cambiar.

### Configuración inicial

- El usuario introduce su clave una sola vez.
- La aplicación comprueba que la clave funciona.
- La clave se guarda en el almacenamiento seguro del sistema (Android Keystore; Keychain en iOS). Nunca en texto plano.
- El usuario elige su idioma de destino.
- Se piden los permisos necesarios (fotos, notificaciones) explicando para qué sirven.
- A partir de ahí, la traducción funciona sin volver a mostrar pantallas de configuración.

La clave nunca debe aparecer en logs, analíticas, informes de errores ni mensajes de depuración. No se envía a ningún backend propio: la aplicación llama directamente a la API con la clave del usuario.

Errores comprensibles para: clave inválida, cuota agotada, proveedor no disponible, sin conexión.

## 6. Configuración del usuario

Mínima:
- Idioma de destino (persistente).
- Clave de Gemini API y botón para probar la conexión.
- Activar o desactivar la detección automática de capturas.
- Aviso de privacidad (ver sección 10).

Nada más en el MVP. Sin cuentas, sin registro, sin suscripciones, sin servidor.

## 7. Resultado de la traducción

Una vista ligera (preferiblemente una hoja inferior o una actividad con aspecto de diálogo sobre la app anterior), con el texto apareciendo en streaming.

### Acciones del MVP
- **Copiar:** coloca el texto traducido en el portapapeles con una pulsación.
- **Cerrar:** cierra la vista, vuelve a la aplicación anterior y elimina los datos temporales de la aplicación.

Acciones posteriores (fase 4): guardar traducciones en una biblioteca local sencilla.

### Gestión de archivos
- La experiencia es temporal por defecto.
- La aplicación no crea copias permanentes ni historial no solicitado.
- Cualquier copia temporal (por ejemplo, la imagen reducida) se borra al cerrar o al terminar.
- La captura original del usuario es suya: la aplicación nunca la borra, mueve ni modifica.
- No se almacena contenido personal en servidores propios.

## 8. Diseño y experiencia de usuario

Minimalista, rápido y centrado en la lectura. Debe sentirse como una herramienta del sistema.

Prioridades:
1. Esfuerzo cero para iniciar la traducción.
2. Calidad de traducción.
3. Rapidez percibida (streaming).
4. Claridad del resultado.
5. Facilidad para copiar y cerrar.
6. Consumo mínimo de batería y recursos.
7. Privacidad.

No mostrar información sobre Gemini en cada traducción. Indicador de carga discreto. Mensajes de error útiles. Sin animaciones innecesarias.

## 9. Arquitectura técnica

Stack propuesto (validarlo antes de empezar):
- **Android:** Kotlin + Jetpack Compose.
- **Lógica compartida:** módulo Kotlin Multiplatform con lo independiente de la plataforma (interfaz de proveedor, cliente REST de Gemini, prompt, modelos de datos), para reutilizarlo después en iOS.
- **iOS:** SwiftUI + App Intents + extensión de compartir, consumiendo el módulo compartido.
- **Gemini:** llamadas REST directas con la clave del usuario. No usar Firebase ni servicios que impliquen un proyecto o facturación del desarrollador.

Si se justifica que KMP añade demasiada complejidad para el MVP, empezar con Android nativo puro manteniendo la lógica de traducción aislada en un módulo sin dependencias de Android, para portarla más adelante.

Separación de responsabilidades:
- Entrada de imágenes (detección de capturas, compartir, futuras vías).
- Preprocesado de imagen (reducción, compresión).
- Proveedor de traducción (interfaz `TranslationProvider`, implementación Gemini).
- Configuración y almacenamiento seguro.
- Presentación del resultado.
- Gestión de contenido temporal.

Sin backend propio, sin bases de datos, sin autenticación, sin analíticas, sin infraestructura innecesaria.

Gestionar correctamente: permisos denegados o parciales, cancelaciones, errores de red, respuestas vacías, cuotas agotadas, imágenes demasiado grandes y que el usuario cierre la vista mientras la traducción está en curso (cancelar la petición).

## 10. Privacidad

Las capturas pueden contener mensajes privados, datos bancarios, nombres y notificaciones.

- Procesar localmente todo lo que no requiera la API.
- Enviar al proveedor únicamente la imagen necesaria y solo cuando el usuario pulsa "Traducir" (salvo que haya activado expresamente la traducción automática).
- No guardar capturas de forma permanente.
- No registrar contenido de capturas ni traducciones en logs.
- Explicar claramente que la imagen se envía a Google al traducir.
- **Comprobar las condiciones actuales de Gemini API:** en el nivel gratuito, Google ha permitido históricamente usar el contenido enviado para mejorar sus productos. Si sigue siendo así, mostrarlo de forma clara y visible en la configuración inicial, no escondido, e indicar que el nivel de pago tiene condiciones distintas.

No afirmar que los datos nunca se usan para mejorar modelos sin haberlo comprobado.

## 11. Alcance y fases

La primera versión debe hacer una cosa excepcionalmente bien: captura nativa, una pulsación, traducción de calidad.

### Fase 1: flujo funcional en Android
1. Crear el proyecto (y el módulo compartido si se valida KMP).
2. Configuración inicial: clave API (almacenamiento seguro y prueba de conexión), idioma de destino, permisos.
3. Pipeline de traducción: preprocesado de imagen, llamada a Gemini en streaming, prompt de la sección 4.2.
4. Entrada por compartir imágenes, para probar el pipeline de extremo a extremo.
5. Detección de capturas nuevas y aviso heads-up con acción "Traducir".
6. Vista de resultado con streaming, copiar y cerrar.
7. Gestión de errores y limpieza de temporales.

### Fase 2: optimización
1. Medir en dispositivo real: tiempo entre captura y aparición del aviso, y entre la pulsación y el primer texto traducido.
2. Ajustar detección (JobScheduler frente a ContentObserver), tamaño de imagen y modelo con esos datos.
3. Evaluar el botón flotante frente a la notificación.
4. Evaluar la vía de asistente digital (pulsación larga del botón de encendido).
5. Evaluar la traducción automática opcional.
6. Variante experimental con servicio de accesibilidad, solo para instalación directa.

### Fase 3: iOS
1. Atajo con "Hacer captura de pantalla" + App Intent de traducción, con resultado en snippet si es viable.
2. Extensión de compartir.
3. Clave en Keychain.
4. Misma experiencia temporal de resultado.

### Fase 4: mejoras opcionales
- Biblioteca local de traducciones guardadas.
- Traducción superpuesta sobre la imagen original.
- Proveedores alternativos.

No implementar fases posteriores antes de que el flujo básico sea rápido, estable y útil.

## 12. Criterios de éxito

Evaluar con pruebas reales, no solo por si compila. La aplicación debe:
- Detectar una captura nativa y ofrecer "Traducir" en el momento.
- Recibir imágenes compartidas desde otras aplicaciones.
- Traducir texto en alemán, francés, italiano e inglés al idioma configurado, ignorando la interfaz.
- Producir traducciones naturales y contextuales.
- Mostrar el texto en streaming sin confirmaciones innecesarias.
- Copiar y cerrar correctamente.
- No dejar archivos temporales ni tocar la captura original.
- Funcionar con la clave personal del usuario.
- Gestionar errores y límites de la API.

No inventar objetivos de latencia: medir y optimizar con datos reales.

## 13. Instrucciones para Claude Code

Actúa como ingeniero de software sénior y responsable técnico del proyecto.

1. Inspecciona el repositorio y respeta cualquier estructura existente.
2. Comprueba la documentación oficial actual de Android (detección de cambios en MediaStore, JobScheduler con content URI, permisos de fotos en Android 13/14+, notificaciones, política de Google Play sobre permisos de fotos y accesibilidad) y de Gemini API (entrada de imágenes, streaming, modelos disponibles, cuotas y condiciones de uso del nivel gratuito).
3. Distingue claramente entre lo viable, lo que necesita permisos especiales y lo que no se puede hacer tal como se imagina. Si algo de esta especificación no es viable, no te detengas: implementa la alternativa con menos esfuerzo para el usuario y anótalo para el informe final.
4. Decide la arquitectura mínima y valida el stack de la sección 9 por tu cuenta. Documenta las decisiones en `docs/DECISIONES.md`.
5. Implementa la fase 1 completa en el orden indicado, con un flujo que funcione de extremo a extremo lo antes posible.
6. Escribe pruebas unitarias para la lógica (preprocesado, cliente de Gemini, gestión de errores, filtrado de capturas) y ejecútalas. Corrige los fallos hasta que pasen.
7. Documenta en el README cómo obtener y configurar una clave de Gemini, compilar, instalar en un dispositivo y probar el flujo.
8. Al terminar, entrega el informe final descrito más abajo.

### Modo de trabajo: autónomo

- Trabaja de principio a fin sin pedir confirmación ni hacer preguntas.
- Cuando algo no esté especificado (identificador del paquete, versión mínima de Android, librerías, nombres internos), decide tú la opción más razonable y anótala en `docs/DECISIONES.md`.
- Si encuentras un bloqueo, intenta resolverlo. Si no es posible, aíslalo, sigue con el resto y explícalo en el informe final.
- Hay un emulador de Android instalado en el ordenador. Úsalo sin preguntar para probar la app de verdad, no solo para compilar (ver sección "Pruebas en el emulador").
- No puedes probar en mi móvil físico: deja esas comprobaciones preparadas y listadas para mí.

### Pruebas en el emulador

- Localiza el SDK de Android y las herramientas (`adb`, `emulator`). Lista los AVD disponibles y arranca uno con una versión reciente de Android. Si no hay ninguno adecuado, créalo con `avdmanager`.
- Instala la app con Gradle y comprueba que arranca sin errores revisando `logcat`.
- Concede los permisos necesarios con `adb shell pm grant` cuando haga falta para automatizar la prueba.
- Prueba la entrada por compartir lanzando un intent `ACTION_SEND` con una imagen mediante `adb`.
- Prueba la detección de capturas con una captura real del sistema (`adb shell input keyevent KEYCODE_SYSRQ`) y, como alternativa, copiando una imagen a la carpeta de capturas y forzando el escaneo de medios. Comprueba que aparece el aviso "Traducir" (`adb shell dumpsys notification`).
- Usa capturas de pantalla del emulador (`adb exec-out screencap -p`) para revisar visualmente la configuración y la vista de resultado.
- Para probar la llamada real a Gemini, lee la clave de la variable de entorno `GEMINI_API_KEY` solo en tiempo de prueba local. Si no existe, prueba el flujo con un proveedor falso y anótalo en el informe. La clave nunca se escribe en archivos del repositorio, logs ni commits.
- Usa imágenes de prueba con texto en alemán, francés, italiano e inglés. Créalas tú mismo si hace falta, guárdalas en una carpeta de pruebas y sin datos personales.
- Incluye en el informe final lo que has probado en el emulador y el resultado.

### Informe final

Al terminar, escribe `docs/INFORME.md` y muéstrame un resumen con:
1. Qué has implementado y qué funciona.
2. Decisiones importantes y por qué.
3. Lo que no era viable tal como estaba especificado y qué alternativa has usado.
4. Resultado de compilación y tests.
5. Problemas pendientes o riesgos (por ejemplo, política de Google Play).
6. Pasos exactos para que yo lo pruebe en mi móvil.

Reglas:
- No implementes fases posteriores a la 1.
- No inventes APIs del sistema ni simules capturas globales imposibles.
- No incluyas claves reales en código, repositorio ni ejemplos.
- No añadas infraestructura innecesaria.
- Ante una disyuntiva, elige lo que reduzca el esfuerzo del usuario sin comprometer privacidad, estabilidad ni restricciones del sistema.

**Objetivo final: captura nativa + una pulsación = traducción natural de lo que estás leyendo. El esfuerzo cero es la principal ventaja competitiva del producto.**
