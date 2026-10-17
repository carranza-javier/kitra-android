# CLAUDE.md

Proyecto: **Kitra**, aplicación móvil que traduce capturas de pantalla con Gemini. Usa "Kitra" como nombre visible de la app, del proyecto y en la documentación. La especificación completa está en `SPEC.md`. Léela entera antes de hacer nada.

## Prioridad del producto
Esfuerzo cero para el usuario: captura nativa + una pulsación = traducción. Ante cualquier duda, elige la opción que reduzca acciones del usuario sin comprometer privacidad ni estabilidad.

## Reglas permanentes
- Nunca incluyas claves de API reales en código, tests, ejemplos ni commits. Usa marcadores como `YOUR_GEMINI_API_KEY`.
- La clave del usuario solo se guarda en almacenamiento seguro (Android Keystore / iOS Keychain) y nunca se escribe en logs.
- No registres en logs el contenido de capturas ni de traducciones.
- La aplicación nunca borra, mueve ni modifica las capturas originales del usuario.
- Sin backend propio, sin analíticas, sin bases de datos, sin cuentas.
- No inventes APIs del sistema. Si algo no es viable, implementa la alternativa con menos esfuerzo para el usuario y anótalo.
- Verifica en la documentación oficial actual cualquier dato que pueda haber cambiado (modelos de Gemini, cuotas, condiciones, APIs de Android).
- El prompt de traducción vive en un único lugar del código.
- Trabaja por fases (sección 11 de SPEC.md). No pases a la siguiente sin que la actual funcione de extremo a extremo.

## Modo de trabajo: autónomo
- No pidas confirmación ni hagas preguntas. Trabaja de principio a fin.
- Lo que no esté especificado, decídelo tú y anótalo en `docs/DECISIONES.md`.
- Si algo se bloquea, intenta resolverlo; si no puedes, aíslalo, sigue con el resto y explícalo al final.
- Después de cada paso, compila y ejecuta los tests antes de continuar.
- Hay un emulador de Android instalado: úsalo sin preguntar para probar la app de verdad (instrucciones en "Pruebas en el emulador", sección 13 de SPEC.md).
- Para pruebas reales con Gemini, la clave está en la variable de entorno `GEMINI_API_KEY`. Si no existe, usa un proveedor falso. Nunca la escribas en archivos, logs ni commits.
- Al terminar, escribe `docs/INFORME.md` y muéstrame el resumen (formato en la sección 13 de SPEC.md).
