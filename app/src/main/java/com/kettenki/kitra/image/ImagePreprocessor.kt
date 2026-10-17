package com.kettenki.kitra.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.scale
import com.kettenki.kitra.core.EncodedImage
import com.kettenki.kitra.core.ImageSizing
import com.kettenki.kitra.core.TranslationException
import java.io.ByteArrayOutputStream

/**
 * Reduce y comprime la imagen en memoria antes de enviarla (SPEC §4.1).
 * No escribe archivos: no hay temporales que limpiar y la captura original no se toca (solo lectura).
 */
class ImagePreprocessor(private val resolver: ContentResolver) {

    /** Lanza [ImageUnreadableException] si no se puede abrir y [TranslationException.ImageTooLarge] si no cabe. */
    fun encode(uri: Uri): EncodedImage {
        val bitmap = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val target = ImageSizing.targetSize(info.size.width, info.size.height)
                decoder.setTargetSize(target.width, target.height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (e: Exception) {
            throw ImageUnreadableException(e)
        }
        try {
            for ((quality, maxSide) in listOf(ImageSizing.JPEG_QUALITY to 0, 70 to 1200)) {
                val source = if (maxSide == 0) bitmap else scaled(bitmap, maxSide)
                val out = ByteArrayOutputStream()
                source.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (source !== bitmap) source.recycle()
                if (out.size() <= ImageSizing.MAX_ENCODED_BYTES) return EncodedImage(out.toByteArray(), "image/jpeg")
            }
            throw TranslationException.ImageTooLarge()
        } finally {
            bitmap.recycle()
        }
    }

    private fun scaled(bitmap: Bitmap, maxSide: Int): Bitmap {
        val size = ImageSizing.targetSize(bitmap.width, bitmap.height, maxSide)
        return bitmap.scale(size.width, size.height)
    }
}

class ImageUnreadableException(cause: Throwable) : Exception("image_unreadable", cause)
