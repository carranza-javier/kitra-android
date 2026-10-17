package app.kitra.core

import kotlinx.coroutines.flow.Flow

/** Imagen ya preprocesada (reducida y comprimida) lista para enviar. */
class EncodedImage(val bytes: ByteArray, val mimeType: String)

/** Fragmento de texto traducido recibido en streaming. */
data class TranslationChunk(val text: String)

/** Proveedor de traducción intercambiable (Gemini, falso para pruebas, futuros proveedores). */
interface TranslationProvider {
    /**
     * Traduce la imagen y emite el texto a medida que llega.
     * Lanza [TranslationException] ante errores conocidos. Cancelar el colector cancela la petición.
     */
    fun translate(image: EncodedImage, target: Language): Flow<TranslationChunk>

    /** Comprueba que la clave y el modelo funcionan. Lanza [TranslationException] si no. */
    suspend fun verify()
}

/** Errores comprensibles para el usuario (SPEC §5 y §9). Los mensajes no contienen datos del usuario. */
sealed class TranslationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidKey : TranslationException("invalid_key")
    class QuotaExceeded : TranslationException("quota_exceeded")
    class Unavailable(val httpCode: Int?) : TranslationException("unavailable")
    class NoConnection(cause: Throwable? = null) : TranslationException("no_connection", cause)
    class ModelNotFound : TranslationException("model_not_found")
    class Blocked(val reason: String?) : TranslationException("blocked")
    class EmptyResponse : TranslationException("empty_response")
    class ImageTooLarge : TranslationException("image_too_large")
    class Unknown(val httpCode: Int?) : TranslationException("unknown")
}
