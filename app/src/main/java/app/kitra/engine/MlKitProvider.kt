package app.kitra.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import app.kitra.core.EncodedImage
import app.kitra.core.Engine
import app.kitra.core.Language
import app.kitra.core.NoticeKind
import app.kitra.core.ProgressStep
import app.kitra.core.ScreenText
import app.kitra.core.TranslationEvent
import app.kitra.core.TranslationException
import app.kitra.core.TranslationProvider
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

/**
 * Traducción local en cualquier móvil: OCR → filtro de interfaz → idioma → traducción de ML Kit,
 * párrafo a párrafo para que el texto vaya apareciendo. La captura no sale del teléfono.
 */
class MlKitProvider(private val context: Context) : TranslationProvider {
    override val engine = Engine.ML_KIT

    override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
        val started = SystemClock.elapsedRealtime()
        if (!runCatching { TextRecognitionEngine.modulesReady(context) }.getOrDefault(false)) {
            emit(TranslationEvent.Progress(ProgressStep.PreparingTextRecognition))
            try {
                TextRecognitionEngine.installModules(context)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                throw TranslationException.DownloadFailed(e)
            }
        }

        emit(TranslationEvent.Progress(ProgressStep.RecognizingText))
        val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
            ?: throw TranslationException.NoTextFound()
        val paragraphs = try {
            ScreenText.paragraphs(TextRecognitionEngine.recognize(bitmap), bitmap.width, bitmap.height)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw TranslationException.EngineFailed(e)
        } finally {
            bitmap.recycle()
        }
        // Solo métricas: número de párrafos y tiempo, nunca el texto.
        Log.i(TAG, "OCR: ${paragraphs.size} paragraphs in ${SystemClock.elapsedRealtime() - started} ms")
        if (paragraphs.isEmpty()) throw TranslationException.NoTextFound()

        val tag = TextRecognitionEngine.identifyLanguage(paragraphs.joinToString("\n"))
        if (tag == "und") throw TranslationException.UnknownLanguage()
        val source = TranslateLanguage.fromLanguageTag(tag) ?: throw TranslationException.LanguageNotSupported(tag)
        Log.i(TAG, "Language identified: $source")

        if (source == target.code) {
            emit(TranslationEvent.Notice(NoticeKind.ALREADY_IN_TARGET))
            emit(TranslationEvent.Text(paragraphs.joinToString("\n\n")))
            return@flow
        }

        for (code in listOf(source, target.code)) {
            if (!LanguagePacks.isDownloaded(code)) {
                emit(TranslationEvent.Progress(ProgressStep.DownloadingLanguage(code)))
                try {
                    // La traducción la ha pedido el usuario ahora: no se espera al wifi.
                    LanguagePacks.download(code, wifiOnly = false)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    throw TranslationException.DownloadFailed(e)
                }
                LanguagePacks.refresh(code)
            }
        }

        emit(TranslationEvent.Progress(ProgressStep.Translating))
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target.code).build(),
        )
        try {
            paragraphs.forEachIndexed { i, paragraph ->
                val translated = try {
                    translator.translate(paragraph).await()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    throw TranslationException.EngineFailed(e)
                }
                emit(TranslationEvent.Text(if (i == 0) translated else "\n\n$translated"))
            }
        } finally {
            translator.close()
        }
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val TAG = "Kitra"
    }
}
