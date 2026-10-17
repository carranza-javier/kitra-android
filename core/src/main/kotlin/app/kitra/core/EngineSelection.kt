package app.kitra.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/** Estado de Gemini Nano en el dispositivo (equivale a FeatureStatus de ML Kit GenAI). */
enum class NanoState { AVAILABLE, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE }

/**
 * Elige automáticamente los motores, en orden de preferencia. El usuario no tiene que elegir nada:
 * - Gemini en la nube solo si lo ha activado en «Avanzado» y hay clave.
 * - Gemini Nano si el móvil lo tiene listo.
 * - ML Kit siempre, como último recurso, porque funciona en cualquier móvil.
 */
object EngineSelection {

    fun chain(cloudEnabled: Boolean, hasKey: Boolean, nano: NanoState, override: Engine? = null): List<Engine> {
        if (override != null) return listOf(override)
        return buildList {
            if (cloudEnabled && hasKey) add(Engine.GEMINI_CLOUD)
            if (nano == NanoState.AVAILABLE) add(Engine.GEMINI_NANO)
            add(Engine.ML_KIT)
        }
    }

    /** Si ante este error se puede recurrir al siguiente motor sin molestar al usuario. */
    fun canFallBack(engine: Engine, error: Throwable): Boolean = when (engine) {
        // Cualquier fallo de Nano (cuota, batería, no disponible, imagen no válida…) se resuelve con ML Kit.
        Engine.GEMINI_NANO -> true
        // En la nube solo se recurre al dispositivo si el problema es de red o de cuota, no de configuración.
        Engine.GEMINI_CLOUD -> error is TranslationException.NoConnection ||
            error is TranslationException.QuotaExceeded ||
            error is TranslationException.Unavailable
        else -> false
    }
}

/**
 * Prueba los proveedores en orden. Si uno falla antes de producir texto y el error lo permite,
 * pasa al siguiente. Una vez que hay texto en pantalla, los errores se muestran tal cual.
 */
class FallbackTranslationProvider(private val providers: List<TranslationProvider>) : TranslationProvider {

    init {
        require(providers.isNotEmpty())
    }

    override val engine: Engine get() = providers.first().engine

    override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
        var fellBackFromCloud = false
        for ((index, provider) in providers.withIndex()) {
            val isLast = index == providers.lastIndex
            var emittedText = false
            var failure: Throwable? = null
            emit(TranslationEvent.EngineStarted(provider.engine))
            if (fellBackFromCloud) emit(TranslationEvent.Notice(NoticeKind.CLOUD_FELL_BACK_TO_DEVICE))
            provider.translate(image, target)
                .catch { failure = it } // solo errores del proveedor, nunca del colector
                .collect { event ->
                    if (event is TranslationEvent.Text) emittedText = true
                    emit(event)
                }
            val error = failure ?: return@flow
            if (isLast || emittedText || !EngineSelection.canFallBack(provider.engine, error)) throw error
            if (provider.engine == Engine.GEMINI_CLOUD) fellBackFromCloud = true
        }
    }
}
