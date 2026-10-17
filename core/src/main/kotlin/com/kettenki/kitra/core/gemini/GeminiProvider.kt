package com.kettenki.kitra.core.gemini

import com.kettenki.kitra.core.EncodedImage
import com.kettenki.kitra.core.HttpRequest
import com.kettenki.kitra.core.HttpResponse
import com.kettenki.kitra.core.HttpTransport
import com.kettenki.kitra.core.Language
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.TranslationEvent
import com.kettenki.kitra.core.TranslationException
import com.kettenki.kitra.core.TranslationPrompt
import com.kettenki.kitra.core.TranslationProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import kotlin.io.encoding.Base64

/**
 * Cliente REST directo de Gemini API con la clave del usuario (BYOK, sin backend ni Firebase).
 * Usa `streamGenerateContent?alt=sse` para mostrar el texto a medida que llega.
 */
class GeminiProvider(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val transport: HttpTransport,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val baseUrl: String = BASE_URL,
) : TranslationProvider {

    init {
        require(MODEL_PATTERN.matches(model)) { "Invalid model name" }
    }

    override val engine: Engine get() = Engine.GEMINI_CLOUD

    override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
        val request = HttpRequest(
            method = "POST",
            url = "$baseUrl/models/$model:streamGenerateContent?alt=sse",
            headers = headers() + ("Content-Type" to "application/json"),
            body = buildRequestBody(image, target).toByteArray(Charsets.UTF_8),
        )
        var emittedAny = false
        coroutineScope {
            val response = open(request)
            // Si se cancela la corrutina (el usuario cierra la vista), cerrar la conexión desbloquea la lectura.
            val closer = launch { try { awaitCancellation() } finally { response.close() } }
            try {
                if (response.code != 200) throw mapHttpError(response.code, safeBody(response))
                for (line in readLines(response)) {
                    currentCoroutineContext().ensureActive()
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue
                    val text = parseChunk(payload)
                    if (text.isNotEmpty()) {
                        emittedAny = true
                        emit(TranslationEvent.Text(text))
                    }
                }
            } finally {
                closer.cancel()
                response.close()
            }
        }
        if (!emittedAny) throw TranslationException.EmptyResponse()
    }.flowOn(ioDispatcher)

    /** Comprueba la clave y el modelo con una llamada de metadatos que no consume tokens. */
    override suspend fun verify() = withContext(ioDispatcher) {
        val response = open(HttpRequest("GET", "$baseUrl/models/$model", headers()))
        response.use {
            if (it.code != 200) throw mapHttpError(it.code, safeBody(it))
        }
    }

    private fun headers() = mapOf("x-goog-api-key" to apiKey)

    private fun open(request: HttpRequest): HttpResponse = try {
        transport.execute(request)
    } catch (e: IOException) {
        throw TranslationException.NoConnection(e)
    }

    private fun readLines(response: HttpResponse): Sequence<String> {
        val iterator = response.lines().iterator()
        return generateSequence {
            try {
                if (iterator.hasNext()) iterator.next() else null
            } catch (e: IOException) {
                throw TranslationException.NoConnection(e)
            }
        }
    }

    private fun safeBody(response: HttpResponse): String = try {
        response.bodyText()
    } catch (_: IOException) {
        ""
    }

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

        /** Modelo predeterminado; se puede cambiar por configuración. Ver docs/DECISIONES.md. */
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

        private val MODEL_PATTERN = Regex("[a-zA-Z0-9._-]+")
        private val json = Json { ignoreUnknownKeys = true }

        internal fun buildRequestBody(image: EncodedImage, target: Language): String = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { addJsonObject { put("text", TranslationPrompt.systemInstruction(target)) } }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", image.mimeType)
                                put("data", Base64.encode(image.bytes))
                            }
                        }
                        addJsonObject { put("text", TranslationPrompt.userInstruction(target)) }
                    }
                }
            }
        }.toString()

        /** Extrae el texto de un fragmento SSE. Lanza [TranslationException] si el fragmento indica error o bloqueo. */
        internal fun parseChunk(payload: String): String {
            val obj = try {
                json.parseToJsonElement(payload).jsonObject
            } catch (_: Exception) {
                return ""
            }
            obj["error"]?.let { throw mapErrorObject(it.jsonObject, (it.jsonObject["code"]?.jsonPrimitive?.contentOrNull)?.toIntOrNull()) }
            obj["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull?.let {
                throw TranslationException.Blocked(it)
            }
            val candidate = obj["candidates"]?.jsonArray?.firstOrNull()?.jsonObject ?: return ""
            val text = candidate["content"]?.jsonObject?.get("parts")?.jsonArray
                ?.mapNotNull { part ->
                    val p = part.jsonObject
                    // Las partes de razonamiento ("thought") no se muestran.
                    if (p["thought"]?.jsonPrimitive?.contentOrNull == "true") null
                    else p["text"]?.jsonPrimitive?.contentOrNull
                }
                ?.joinToString("")
                .orEmpty()
            val finish = candidate["finishReason"]?.jsonPrimitive?.contentOrNull
            if (text.isEmpty() && finish in BLOCK_FINISH_REASONS) throw TranslationException.Blocked(finish)
            return text
        }

        private val BLOCK_FINISH_REASONS = setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "IMAGE_SAFETY", "RECITATION")

        internal fun mapHttpError(code: Int, body: String): TranslationException {
            val error = try {
                json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
            } catch (_: Exception) {
                null
            }
            return mapErrorObject(error, code)
        }

        private fun mapErrorObject(error: JsonObject?, code: Int?): TranslationException {
            val status = error?.get("status")?.jsonPrimitive?.contentOrNull
            val reasons = error?.get("details")?.let { details ->
                runCatching { details.jsonArray.mapNotNull { it.jsonObject["reason"]?.jsonPrimitive?.contentOrNull } }.getOrNull()
            }.orEmpty()
            return when {
                // Problema conocido de las nuevas claves «AQ.» de AI Studio (foro de Google, 2026).
                reasons.any { it == "ACCESS_TOKEN_TYPE_UNSUPPORTED" } -> TranslationException.KeyTypeUnsupported()
                reasons.any { it == "API_KEY_INVALID" || it == "API_KEY_EXPIRED" } -> TranslationException.InvalidKey()
                code == 401 || status == "UNAUTHENTICATED" -> TranslationException.InvalidKey()
                code == 403 || status == "PERMISSION_DENIED" -> TranslationException.InvalidKey()
                code == 429 || status == "RESOURCE_EXHAUSTED" -> TranslationException.QuotaExceeded()
                code == 404 || status == "NOT_FOUND" -> TranslationException.ModelNotFound()
                code == 413 -> TranslationException.ImageTooLarge()
                code != null && code >= 500 -> TranslationException.Unavailable(code)
                status == "UNAVAILABLE" || status == "INTERNAL" -> TranslationException.Unavailable(code)
                else -> TranslationException.Unknown(code)
            }
        }
    }
}
