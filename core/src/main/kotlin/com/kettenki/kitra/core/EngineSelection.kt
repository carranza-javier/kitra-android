package com.kettenki.kitra.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/** Estado de Gemini Nano en el dispositivo (equivale a FeatureStatus de ML Kit GenAI). */
enum class NanoState { AVAILABLE, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE }

/**
 * Elige los motores en orden. El usuario no tiene que elegir nada:
 * - Gemini en la nube con la clave del usuario es siempre el motor principal, si hay clave y conexión.
 * - Si no, el «modo básico» en el teléfono: Gemini Nano si el móvil lo tiene listo; si no, ML Kit.
 * - ML Kit va siempre al final, porque funciona en cualquier móvil.
 */
object EngineSelection {

    fun chain(hasKey: Boolean, online: Boolean, nano: NanoState, override: Engine? = null): List<Engine> {
        if (override != null) return listOf(override)
        return buildList {
            if (hasKey && online) add(Engine.GEMINI_CLOUD)
            if (nano == NanoState.AVAILABLE) add(Engine.GEMINI_NANO)
            add(Engine.ML_KIT)
        }
    }

    /** Motivo del modo básico cuando ni siquiera se intenta la nube. */
    fun initialBasicReason(hasKey: Boolean, online: Boolean): BasicModeReason = when {
        !hasKey -> BasicModeReason.NO_KEY
        !online -> BasicModeReason.OFFLINE
        else -> BasicModeReason.CLOUD_FAILED
    }

    /** Motivo del modo básico cuando Gemini en la nube ha fallado con este error. */
    fun basicReasonFor(error: Throwable): BasicModeReason = when (error) {
        is TranslationException.NoConnection -> BasicModeReason.OFFLINE
        is TranslationException.QuotaExceeded -> BasicModeReason.QUOTA_EXCEEDED
        is TranslationException.InvalidKey, is TranslationException.KeyTypeUnsupported -> BasicModeReason.KEY_INVALID
        else -> BasicModeReason.CLOUD_FAILED
    }

    /** Si ante este error se puede recurrir al siguiente motor. */
    fun canFallBack(engine: Engine, @Suppress("UNUSED_PARAMETER") error: Throwable): Boolean = when (engine) {
        // Cualquier fallo de la nube (red, cuota, clave, servicio) se resuelve en el teléfono, con su motivo.
        Engine.GEMINI_CLOUD -> true
        // Cualquier fallo de Nano (cuota, batería, no disponible, imagen no válida…) se resuelve con ML Kit.
        Engine.GEMINI_NANO -> true
        else -> false
    }
}

/**
 * Prueba los proveedores en orden. Si uno falla antes de producir texto y el error lo permite,
 * pasa al siguiente. Cada vez que traduce un motor local emite [TranslationEvent.BasicMode] con el motivo.
 * Una vez que hay texto en pantalla, los errores se muestran tal cual.
 */
class FallbackTranslationProvider(
    private val providers: List<TranslationProvider>,
    /** Motivo del modo básico si la cadena empieza directamente por un motor local. */
    private val initialBasicReason: BasicModeReason = BasicModeReason.NO_KEY,
) : TranslationProvider {

    init {
        require(providers.isNotEmpty())
    }

    override val engine: Engine get() = providers.first().engine

    override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
        var basicReason = initialBasicReason
        for ((index, provider) in providers.withIndex()) {
            val isLast = index == providers.lastIndex
            var emittedText = false
            var failure: Throwable? = null
            emit(TranslationEvent.EngineStarted(provider.engine))
            if (provider.engine.isLocal) emit(TranslationEvent.BasicMode(basicReason))
            provider.translate(image, target)
                .catch { failure = it } // solo errores del proveedor, nunca del colector
                .collect { event ->
                    if (event is TranslationEvent.Text) emittedText = true
                    emit(event)
                }
            val error = failure ?: return@flow
            if (isLast || emittedText || !EngineSelection.canFallBack(provider.engine, error)) throw error
            if (provider.engine == Engine.GEMINI_CLOUD) basicReason = EngineSelection.basicReasonFor(error)
        }
    }
}
