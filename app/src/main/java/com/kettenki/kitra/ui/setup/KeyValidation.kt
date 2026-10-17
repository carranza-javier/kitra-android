package com.kettenki.kitra.ui.setup

import androidx.annotation.StringRes
import com.kettenki.kitra.R
import com.kettenki.kitra.core.TranslationException
import com.kettenki.kitra.engine.Engines
import com.kettenki.kitra.settings.Settings

/** Resultado de validar una clave de Gemini contra Google. */
sealed interface KeyCheck {
    data object Valid : KeyCheck

    /** La clave es válida pero no tiene cuota ahora mismo: se guarda igualmente. */
    data object ValidNoQuota : KeyCheck
    data class Invalid(@StringRes val message: Int) : KeyCheck
}

object KeyValidation {

    /**
     * Texto del portapapeles que parece una clave: una sola palabra sin espacios, de 30 a 256 caracteres
     * alfanuméricos (o . _ -). No se exige el prefijo «AIza»: desde mayo de 2026 AI Studio crea claves
     * de otro tipo («auth keys») y Google no documenta su formato. La validación real la hace Google.
     */
    private val KEY_SHAPE = Regex("[A-Za-z0-9._-]{30,256}")

    fun looksLikeKey(text: String?): Boolean = text?.trim()?.let { KEY_SHAPE.matches(it) } == true

    /** «AIza…a1B2»: suficiente para reconocerla sin mostrarla entera. */
    fun mask(key: String): String = if (key.length <= 10) "…" else key.take(4) + "…" + key.takeLast(4)

    /** Comprueba la clave con Google y, si sirve, la guarda cifrada. */
    suspend fun checkAndSave(settings: Settings, key: String): KeyCheck {
        val trimmed = key.trim()
        return try {
            Engines.gemini(trimmed, settings.model).verify()
            settings.apiKey.save(trimmed)
            KeyCheck.Valid
        } catch (e: TranslationException.QuotaExceeded) {
            settings.apiKey.save(trimmed)
            KeyCheck.ValidNoQuota
        } catch (e: TranslationException) {
            KeyCheck.Invalid(
                when (e) {
                    is TranslationException.InvalidKey -> R.string.key_error_invalid
                    is TranslationException.KeyTypeUnsupported -> R.string.key_error_type_unsupported
                    is TranslationException.NoConnection -> R.string.key_error_offline
                    is TranslationException.ModelNotFound -> R.string.key_error_model
                    else -> R.string.key_error_google
                },
            )
        }
    }
}
