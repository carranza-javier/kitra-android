package com.kettenki.kitra.engine

import android.graphics.BitmapFactory
import android.util.Log
import com.kettenki.kitra.core.EncodedImage
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.Language
import com.kettenki.kitra.core.NanoState
import com.kettenki.kitra.core.TranslationEvent
import com.kettenki.kitra.core.TranslationException
import com.kettenki.kitra.core.TranslationPrompt
import com.kettenki.kitra.core.TranslationProvider
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Gemini Nano en el dispositivo con la Prompt API de ML Kit GenAI (beta): recibe la captura como imagen,
 * igual que Gemini en la nube, y la traduce sin que salga del teléfono.
 * Requisitos de la API: móvil compatible y app en primer plano (la ventana de resultado lo está).
 */
class NanoProvider(private val model: GenerativeModel = Nano.model) : TranslationProvider {
    override val engine = Engine.GEMINI_NANO

    override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
        val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
            ?: throw TranslationException.EngineFailed()
        val systemPrompt = runCatching { model.isSystemPromptAvailable() }.getOrDefault(false)
        val request = if (systemPrompt) {
            generateContentRequest(
                SystemInstruction(TranslationPrompt.onDeviceSystemInstruction(target)),
                ImagePart(bitmap),
                TextPart(TranslationPrompt.onDeviceUserInstruction(target)),
            ) { configure() }
        } else {
            // Sin instrucciones de sistema, las reglas van en el mismo turno que la imagen.
            generateContentRequest(
                ImagePart(bitmap),
                TextPart(TranslationPrompt.onDeviceSystemInstruction(target) + "\n\n" + TranslationPrompt.onDeviceUserInstruction(target)),
            ) { configure() }
        }
        var emittedAny = false
        model.generateContentStream(request)
            .catch { e ->
                if (e is CancellationException) throw e
                // Solo el código de error, nunca contenido.
                Log.w(TAG, "Gemini Nano failed: ${(e as? com.google.mlkit.genai.common.GenAiException)?.errorCode ?: e.javaClass.simpleName}")
                throw TranslationException.EngineFailed(e)
            }
            .collect { response ->
                val text = response.candidates.firstOrNull()?.text.orEmpty()
                if (text.isNotEmpty()) {
                    emittedAny = true
                    emit(TranslationEvent.Text(text))
                }
            }
        if (!emittedAny) throw TranslationException.EmptyResponse()
    }.flowOn(Dispatchers.Default)

    private fun com.google.mlkit.genai.prompt.GenerateContentRequest.Builder.configure() {
        temperature = 0.2f
        topK = 16
        candidateCount = 1
    }

    private companion object {
        const val TAG = "Kitra"
    }
}

/** Disponibilidad y descarga de Gemini Nano (gestionado por AICore, compartido por todo el sistema). */
object Nano {
    private const val TAG = "Kitra"

    /** Modelo estable y completo: se prioriza la calidad de traducción sobre unos milisegundos. */
    val model: GenerativeModel by lazy {
        Generation.getClient(
            generationConfig {
                modelConfig = modelConfig {
                    releaseStage = ModelReleaseStage.STABLE
                    preference = ModelPreference.FULL
                }
            },
        )
    }

    suspend fun state(): NanoState = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> NanoState.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> NanoState.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> NanoState.DOWNLOADING
            else -> NanoState.UNAVAILABLE
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        // En emuladores y móviles sin AICore la comprobación puede fallar: equivale a «no disponible».
        Log.i(TAG, "Gemini Nano status check failed: ${e.javaClass.simpleName}")
        NanoState.UNAVAILABLE
    }

    /** Descarga Gemini Nano. Emite los bytes descargados; termina al completarse o lanza si falla. */
    fun download(): Flow<Long> = flow {
        model.download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadProgress -> emit(status.totalBytesDownloaded)
                is DownloadStatus.DownloadFailed -> throw TranslationException.DownloadFailed(status.e)
                else -> Unit
            }
        }
    }
}
