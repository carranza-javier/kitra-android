package com.kettenki.kitra.core

import kotlin.math.abs

/** Rectángulo en píxeles de la imagen. */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class OcrLine(val text: String, val box: Box)

/** Bloque de texto tal como lo devuelve el OCR (normalmente, un párrafo o parte de él). */
data class OcrBlock(val lines: List<OcrLine>, val box: Box) {
    val text: String get() = lines.joinToString(" ") { it.text }
    val wordCount: Int get() = text.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
    val lineHeight: Int get() = lines.map { it.box.height }.sorted().let { if (it.isEmpty()) box.height else it[it.size / 2] }
}

/**
 * Convierte los bloques del OCR de una captura en los párrafos del contenido principal (SPEC §4.2:
 * «ignorar los elementos de interfaz»). Usa solo la posición y el tamaño de cada bloque:
 * - Fuera: barra de estado (franja superior) y barra de navegación (franja inferior).
 * - Fuera: textos muy cortos en las zonas de barras de herramientas, pestañas y botones (bordes de la pantalla).
 * - Fuera: restos de iconos (1-2 caracteres sueltos).
 * - Dentro: el resto, ordenado de arriba abajo, uniendo bloques que continúan el mismo párrafo.
 */
object ScreenText {

    /** Franja de la barra de estado (≈24 dp en una pantalla de 2400 px de alto). */
    const val STATUS_BAR_FRACTION = 0.045

    /** Franja de la barra de navegación. */
    const val NAV_BAR_FRACTION = 0.045

    /** Zonas de barras de herramientas, pestañas y botones, arriba y abajo. */
    const val TOP_CHROME_FRACTION = 0.12
    const val BOTTOM_CHROME_FRACTION = 0.25

    /** Un texto con estas palabras o menos, en esas zonas, se considera interfaz. */
    const val MAX_UI_WORDS = 3

    /** Texto corto aislado en la zona inferior: banners superpuestos (cookies, avisos), no contenido. */
    const val SAME_SIZE_RATIO = 0.7
    const val MAX_BANNER_WORDS = 8
    const val BANNER_GAP_LINES = 3

    fun paragraphs(blocks: List<OcrBlock>, imageWidth: Int, imageHeight: Int): List<String> {
        val content = blocks.filter { isContent(it, imageWidth, imageHeight) }
            .sortedWith(compareBy({ it.box.top }, { it.box.left }))
            .let { dropIsolatedBottomBanners(it, imageHeight) }
        val paragraphs = mutableListOf<MutableList<OcrBlock>>()
        for (block in content) {
            val current = paragraphs.lastOrNull()
            if (current != null && continuesParagraph(current.last(), block, imageWidth)) current += block
            else paragraphs += mutableListOf(block)
        }
        return paragraphs.map { group -> joinLines(group.flatMap { it.lines }.map { it.text }) }
            .filter { it.isNotBlank() }
    }

    internal fun isContent(block: OcrBlock, imageWidth: Int, imageHeight: Int): Boolean {
        val text = block.text.trim()
        if (text.count(Char::isLetterOrDigit) <= 2) return false // iconos, "OK", "☰", flechas
        val box = block.box
        if (box.bottom <= imageHeight * STATUS_BAR_FRACTION) return false
        if (box.top >= imageHeight * (1 - NAV_BAR_FRACTION)) return false
        val short = block.wordCount <= MAX_UI_WORDS && block.lines.size == 1
        if (short && box.bottom <= imageHeight * TOP_CHROME_FRACTION) return false
        if (short && box.top >= imageHeight * (1 - BOTTOM_CHROME_FRACTION)) return false
        // Etiquetas cortas pegadas a un lateral (iconos con texto, contadores de «me gusta», etc.).
        val nearSide = box.left <= imageWidth * 0.02 || box.right >= imageWidth * 0.98
        if (short && nearSide && box.width < imageWidth * 0.25) return false
        return true
    }

    /**
     * En un artículo, el texto de la parte baja sigue al párrafo anterior con el espaciado normal.
     * Un texto corto en la zona inferior separado por un hueco grande es un elemento superpuesto.
     */
    internal fun dropIsolatedBottomBanners(sorted: List<OcrBlock>, imageHeight: Int): List<OcrBlock> {
        val kept = mutableListOf<OcrBlock>()
        for (block in sorted) {
            val inBottomZone = block.box.top >= imageHeight * (1 - BOTTOM_CHROME_FRACTION)
            val previous = kept.lastOrNull()
            val gap = if (previous == null) block.box.top else block.box.top - previous.box.bottom
            val isolated = gap > BANNER_GAP_LINES * block.lineHeight.coerceAtLeast(1)
            if (inBottomZone && block.wordCount <= MAX_BANNER_WORDS && isolated) continue
            kept += block
        }
        return kept
    }

    /** El bloque siguiente continúa el párrafo si está muy cerca, alineado y con el mismo tamaño de letra. */
    internal fun continuesParagraph(previous: OcrBlock, next: OcrBlock, imageWidth: Int): Boolean {
        val lineHeight = previous.lineHeight.coerceAtLeast(1)
        val gap = next.box.top - previous.box.bottom
        // Una línea corta sin letras con trazos hacia abajo (p, g…) mide algo menos que las demás (≈0,72),
        // mientras que un titular y el cuerpo difieren bastante más (40 px frente a 64 px ≈ 0,63).
        val nextHeight = next.lineHeight.coerceAtLeast(1)
        val sameSize = minOf(nextHeight, lineHeight).toDouble() / maxOf(nextHeight, lineHeight) >= SAME_SIZE_RATIO
        val aligned = abs(next.box.left - previous.box.left) <= imageWidth * 0.05
        return gap in -lineHeight / 2..(lineHeight * 0.8).toInt() && sameSize && aligned
    }

    /** Une las líneas de un párrafo deshaciendo los guiones de corte de palabra. */
    internal fun joinLines(lines: List<String>): String {
        val sb = StringBuilder()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (sb.isEmpty()) {
                sb.append(line)
            } else if (sb.endsWith("-") && sb.length > 1 && sb[sb.length - 2].isLetter() && line.first().isLowerCase()) {
                sb.setLength(sb.length - 1)
                sb.append(line)
            } else if (isCjk(sb.last()) && isCjk(line.first())) {
                sb.append(line) // el chino y el japonés no separan con espacios
            } else {
                sb.append(' ').append(line)
            }
        }
        return sb.toString()
    }

    fun containsCjk(text: String): Boolean = text.any(::isCjk)

    fun cjkRatio(text: String): Double {
        val letters = text.count { it.isLetter() }
        return if (letters == 0) 0.0 else text.count(::isCjk).toDouble() / letters
    }

    private fun isCjk(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES
    }
}
