package app.kitra.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Proveedor falso para probar el flujo sin clave: emite un texto fijo en trozos, simulando streaming.
 * La app solo permite activarlo en compilaciones de depuración.
 */
class FakeTranslationProvider(
    private val chunkDelayMs: Long = 80,
) : TranslationProvider {

    override val engine: Engine get() = Engine.FAKE

    override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
        val text = "[Proveedor falso · ${target.nativeName}]\n" +
            "Imagen recibida: ${image.bytes.size / 1024} KB (${image.mimeType}).\n\n" +
            "Este texto simula una traducción en streaming para probar el flujo completo sin clave de Gemini.\n\n" +
            "Copia y cierra para volver a lo que estabas leyendo."
        text.split(" ").forEachIndexed { i, word ->
            delay(chunkDelayMs)
            emit(TranslationEvent.Text(if (i == 0) word else " $word"))
        }
    }
}
