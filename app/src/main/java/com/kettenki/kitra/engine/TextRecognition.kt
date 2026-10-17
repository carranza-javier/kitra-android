package com.kettenki.kitra.engine

import android.content.Context
import android.graphics.Bitmap
import com.kettenki.kitra.core.Box
import com.kettenki.kitra.core.OcrBlock
import com.kettenki.kitra.core.OcrLine
import com.kettenki.kitra.core.ScreenText
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * OCR (Text Recognition v2) e identificación de idioma de ML Kit, en sus versiones "sin agrupar":
 * los modelos los gestiona Google Play Services, así que el APK apenas crece.
 */
object TextRecognitionEngine {
    private val latin by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val chinese by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    private val languageId by lazy { LanguageIdentification.getClient() }

    /** Si los modelos de OCR y de idioma ya están en el teléfono. */
    suspend fun modulesReady(context: Context): Boolean =
        ModuleInstall.getClient(context).areModulesAvailable(latin, chinese, languageId).await().areModulesAvailable()

    /** Pide a Google Play Services que instale los modelos y espera a que termine. */
    suspend fun installModules(context: Context) {
        val request = ModuleInstallRequest.newBuilder().addApi(latin).addApi(chinese).addApi(languageId).build()
        ModuleInstall.getClient(context).installModules(request).await()
    }

    /**
     * Reconoce el texto. Lanza los dos reconocedores a la vez: el latino es el más preciso para
     * alfabetos latinos (acentos, ß…) y el chino también lee caracteres CJK. Se queda con el chino
     * solo si encuentra una proporción relevante de caracteres CJK.
     */
    suspend fun recognize(bitmap: Bitmap): List<OcrBlock> = coroutineScope {
        val image = InputImage.fromBitmap(bitmap, 0)
        val latinResult = async { latin.process(image).await() }
        val chineseResult = async { chinese.process(image).await() }
        val cjk = chineseResult.await()
        val chosen = if (ScreenText.cjkRatio(cjk.text) >= 0.3) cjk.also { latinResult.cancel() } else latinResult.await()
        chosen.textBlocks.mapNotNull(::toBlock)
    }

    /** Etiqueta BCP-47 del idioma dominante, o "und" si no se reconoce. */
    suspend fun identifyLanguage(text: String): String = languageId.identifyLanguage(text.take(2000)).await()

    private fun toBlock(block: Text.TextBlock): OcrBlock? {
        val box = block.boundingBox ?: return null
        val lines = block.lines.mapNotNull { line ->
            val r = line.boundingBox ?: return@mapNotNull null
            OcrLine(line.text, Box(r.left, r.top, r.right, r.bottom))
        }
        if (lines.isEmpty()) return null
        return OcrBlock(lines, Box(box.left, box.top, box.right, box.bottom))
    }
}
