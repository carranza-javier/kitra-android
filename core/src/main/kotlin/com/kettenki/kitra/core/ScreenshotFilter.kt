package com.kettenki.kitra.core

/**
 * Decide si una imagen nueva de MediaStore es una captura de pantalla (SPEC §3.1).
 * La carpeta varía según fabricante: Pictures/Screenshots, DCIM/Screenshots, Pictures/Screenshot, etc.
 */
object ScreenshotFilter {

    /** Antigüedad máxima para considerar "nueva" una captura. */
    const val MAX_AGE_SECONDS = 60L

    private val folderKeywords = listOf(
        "screenshot", "screen_shot", "screen shot", "screencap", "screen capture",
        "bildschirmfoto", "captures d", "capturas", "schermata",
    )

    fun isScreenshot(
        relativePath: String?,
        displayName: String?,
        dateAddedSeconds: Long,
        nowSeconds: Long,
        maxAgeSeconds: Long = MAX_AGE_SECONDS,
    ): Boolean {
        if (!isRecent(dateAddedSeconds, nowSeconds, maxAgeSeconds)) return false
        val path = relativePath?.lowercase().orEmpty()
        // La carpeta es el criterio principal; el nombre solo se usa si la carpeta no está disponible.
        if (path.isNotEmpty()) return folderKeywords.any { path.contains(it) }
        return displayName?.lowercase()?.startsWith("screenshot") == true
    }

    fun isRecent(dateAddedSeconds: Long, nowSeconds: Long, maxAgeSeconds: Long = MAX_AGE_SECONDS): Boolean {
        val age = nowSeconds - dateAddedSeconds
        // Margen de unos segundos por relojes ligeramente desfasados.
        return age in -10..maxAgeSeconds
    }
}
