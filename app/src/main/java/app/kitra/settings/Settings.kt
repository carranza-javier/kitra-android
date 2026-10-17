package app.kitra.settings

import android.content.Context
import androidx.core.content.edit
import app.kitra.BuildConfig
import app.kitra.core.Engine
import app.kitra.core.Language
import app.kitra.core.gemini.GeminiProvider
import java.util.Locale

/** Configuración mínima del usuario (SPEC §6). Sin base de datos: SharedPreferences privadas. */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kitra_settings", Context.MODE_PRIVATE)

    val apiKey = SecureKeyStore(prefs)

    var targetLanguage: Language
        get() = Language.fromCode(prefs.getString(KEY_LANGUAGE, null))
            ?: Language.forSystemLocale(Locale.getDefault().language)
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value.code) }

    /** Preferencia del usuario; la detección solo funciona si además hay permisos. */
    var detectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_DETECTION, true)
        set(value) = prefs.edit { putBoolean(KEY_DETECTION, value) }

    var model: String
        get() = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: GeminiProvider.DEFAULT_MODEL
        set(value) = prefs.edit { putString(KEY_MODEL, value.trim().ifEmpty { null }) }

    /** «Máxima calidad»: usar Gemini en la nube con la clave del usuario (opcional, en «Avanzado»). */
    var cloudEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD, false)
        set(value) = prefs.edit { putBoolean(KEY_CLOUD, value) }

    /** Solo depuración: fuerza un motor para comparar (null = elección automática). */
    var engineOverride: Engine?
        get() = if (BuildConfig.DEBUG) prefs.getString(KEY_ENGINE_OVERRIDE, null)?.let { runCatching { Engine.valueOf(it) }.getOrNull() } else null
        set(value) = prefs.edit { putString(KEY_ENGINE_OVERRIDE, value?.name) }

    /** Si ya se pidió el paquete del idioma de destino durante la configuración inicial. */
    var targetPackRequested: String?
        get() = prefs.getString(KEY_TARGET_PACK, null)
        set(value) = prefs.edit { putString(KEY_TARGET_PACK, value) }

    /** Proveedor falso para probar sin clave. Ignorado en compilaciones de release. */
    var useFakeProvider: Boolean
        get() = BuildConfig.DEBUG && prefs.getBoolean(KEY_FAKE, false)
        set(value) = prefs.edit { putBoolean(KEY_FAKE, value) }

    /** Última captura notificada, para no avisar dos veces por la misma. */
    var lastNotifiedMediaId: Long
        get() = prefs.getLong(KEY_LAST_ID, -1)
        set(value) = prefs.edit { putLong(KEY_LAST_ID, value) }

    /** Si ya se pidieron los permisos automáticamente en la configuración inicial. */
    var permissionsPrompted: Boolean
        get() = prefs.getBoolean(KEY_PERMS_PROMPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_PERMS_PROMPTED, value) }

    private companion object {
        const val KEY_LANGUAGE = "target_language"
        const val KEY_DETECTION = "detection_enabled"
        const val KEY_MODEL = "model"
        const val KEY_FAKE = "debug_fake_provider"
        const val KEY_LAST_ID = "last_notified_media_id"
        const val KEY_PERMS_PROMPTED = "permissions_prompted"
        const val KEY_CLOUD = "cloud_enabled"
        const val KEY_ENGINE_OVERRIDE = "debug_engine_override"
        const val KEY_TARGET_PACK = "target_pack_requested"
    }
}
