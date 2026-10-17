package com.kettenki.kitra.core

import kotlinx.coroutines.flow.Flow

/** Imagen ya preprocesada (reducida y comprimida) lista para traducir. */
class EncodedImage(val bytes: ByteArray, val mimeType: String)

/** Motores de traducción disponibles. */
enum class Engine {
    /** Gemini Nano en el dispositivo (ML Kit GenAI Prompt API). */
    GEMINI_NANO,

    /** OCR + identificación de idioma + traducción local de ML Kit. */
    ML_KIT,

    /** Gemini en la nube con la clave del usuario. */
    GEMINI_CLOUD,

    /** Proveedor falso de las compilaciones de depuración. */
    FAKE,
}

/** Lo que va ocurriendo durante una traducción. */
sealed interface TranslationEvent {
    /** Motor que empieza a trabajar (cambia si hay que recurrir al siguiente). */
    data class EngineStarted(val engine: Engine) : TranslationEvent

    /** Fragmento de texto traducido que se añade al anterior. */
    data class Text(val text: String) : TranslationEvent

    /** Paso intermedio que conviene mostrar mientras aún no hay texto. */
    data class Progress(val step: ProgressStep) : TranslationEvent

    /** Aviso breve que acompaña al resultado. */
    data class Notice(val kind: NoticeKind) : TranslationEvent
}

sealed interface ProgressStep {
    data object PreparingTextRecognition : ProgressStep
    data object RecognizingText : ProgressStep
    data class DownloadingLanguage(val languageCode: String) : ProgressStep
    data object Translating : ProgressStep
}

enum class NoticeKind {
    /** El texto ya estaba en el idioma de destino: se muestra tal cual. */
    ALREADY_IN_TARGET,

    /** No se pudo usar Gemini en la nube y se ha traducido en el dispositivo. */
    CLOUD_FELL_BACK_TO_DEVICE,
}

/** Proveedor de traducción intercambiable. */
interface TranslationProvider {
    val engine: Engine

    /**
     * Traduce la imagen y emite eventos a medida que avanza.
     * Lanza [TranslationException] ante errores conocidos. Cancelar el colector cancela el trabajo.
     */
    fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent>

    /** Comprueba que el proveedor puede trabajar (por ejemplo, que la clave es válida). */
    suspend fun verify() = Unit
}

/** Errores comprensibles para el usuario. Los mensajes no contienen datos del usuario. */
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

    /** No se ha encontrado texto legible en la imagen. */
    class NoTextFound : TranslationException("no_text_found")

    /** No se ha podido identificar el idioma del texto. */
    class UnknownLanguage : TranslationException("unknown_language")

    /** El idioma detectado no tiene traducción local. */
    class LanguageNotSupported(val languageTag: String) : TranslationException("language_not_supported")

    /** No se ha podido descargar un paquete de idioma o un modelo. */
    class DownloadFailed(cause: Throwable? = null) : TranslationException("download_failed", cause)

    /** El motor en el dispositivo ha fallado o no está disponible ahora mismo. */
    class EngineFailed(cause: Throwable? = null) : TranslationException("engine_failed", cause)
}
