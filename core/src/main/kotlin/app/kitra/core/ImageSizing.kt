package app.kitra.core

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Cálculo puro del tamaño de la imagen reducida (SPEC §4.1).
 * Una captura típica (1080×2400) se reduce a 1600 px de lado largo: el texto sigue siendo legible
 * y la petición pesa y tarda bastante menos.
 */
object ImageSizing {
    const val MAX_LONG_SIDE = 1600
    const val JPEG_QUALITY = 85

    /** Límite de seguridad para la imagen codificada (las peticiones con datos inline admiten unos 20 MB). */
    const val MAX_ENCODED_BYTES = 7 * 1024 * 1024

    data class Size(val width: Int, val height: Int)

    fun targetSize(width: Int, height: Int, maxLongSide: Int = MAX_LONG_SIDE): Size {
        require(width > 0 && height > 0) { "Invalid image size" }
        val longSide = max(width, height)
        if (longSide <= maxLongSide) return Size(width, height)
        val scale = maxLongSide.toDouble() / longSide
        return Size(
            max(1, (width * scale).roundToInt()),
            max(1, (height * scale).roundToInt()),
        )
    }

    /** Mayor potencia de dos para submuestrear al decodificar sin quedar por debajo del objetivo. */
    fun sampleSize(width: Int, height: Int, maxLongSide: Int = MAX_LONG_SIDE): Int {
        var sample = 1
        val longSide = max(width, height)
        while (longSide / (sample * 2) >= maxLongSide) sample *= 2
        return sample
    }
}
