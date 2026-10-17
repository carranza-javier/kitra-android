package com.kettenki.kitra.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.kettenki.kitra.core.BasicModeReason
import com.kettenki.kitra.core.Engine

/** Una traducción en la pantalla de diagnóstico. No contiene texto ni imágenes. */
data class TranslationLogEntry(
    val timeMillis: Long,
    val engine: Engine?,
    val basicReason: BasicModeReason?,
    val firstTextMs: Long?,
    val totalMs: Long?,
    /** Nombre del error si falló (p. ej. "NoTextFound"), o null si fue bien. */
    val error: String?,
)

/** Historial corto (20 entradas) en SharedPreferences: sin base de datos, solo para diagnóstico. */
class TranslationLog(private val prefs: SharedPreferences) {

    fun add(entry: TranslationLogEntry) {
        val line = listOf(
            entry.timeMillis, entry.engine?.name.orEmpty(), entry.basicReason?.name.orEmpty(),
            entry.firstTextMs ?: "", entry.totalMs ?: "", entry.error.orEmpty(),
        ).joinToString("|")
        val lines = (listOf(line) + raw()).take(MAX)
        prefs.edit { putString(KEY, lines.joinToString("\n")) }
    }

    fun entries(): List<TranslationLogEntry> = raw().mapNotNull { line ->
        val p = line.split("|")
        if (p.size < 6) return@mapNotNull null
        TranslationLogEntry(
            timeMillis = p[0].toLongOrNull() ?: return@mapNotNull null,
            engine = p[1].takeIf { it.isNotEmpty() }?.let { runCatching { Engine.valueOf(it) }.getOrNull() },
            basicReason = p[2].takeIf { it.isNotEmpty() }?.let { runCatching { BasicModeReason.valueOf(it) }.getOrNull() },
            firstTextMs = p[3].toLongOrNull(),
            totalMs = p[4].toLongOrNull(),
            error = p[5].takeIf { it.isNotEmpty() },
        )
    }

    fun clear() = prefs.edit { remove(KEY) }

    private fun raw(): List<String> = prefs.getString(KEY, null)?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val KEY = "translation_log"
        const val MAX = 20
    }
}
