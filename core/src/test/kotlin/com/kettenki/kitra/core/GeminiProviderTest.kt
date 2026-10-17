package com.kettenki.kitra.core

import com.kettenki.kitra.core.gemini.GeminiProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiProviderTest {

    private val image = EncodedImage(byteArrayOf(1, 2, 3), "image/jpeg")

    private class FakeResponse(override val code: Int, private val body: List<String>) : HttpResponse {
        var closed = false
        override fun lines(): Sequence<String> = body.asSequence()
        override fun close() { closed = true }
    }

    private class RecordingTransport(private val response: () -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override fun execute(request: HttpRequest): HttpResponse {
            requests += request
            return response()
        }
    }

    private fun sse(vararg texts: String) = texts.flatMap {
        listOf("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":${Json.encodeToString(it)}}]}}]}", "")
    }

    private fun provider(transport: HttpTransport, model: String = "gemini-test") =
        GeminiProvider("YOUR_GEMINI_API_KEY", model, transport, UnconfinedTestDispatcher())

    @Test
    fun streamsChunksInOrder() = runTest {
        val response = FakeResponse(200, sse("Hola", " mundo", "!"))
        val transport = RecordingTransport { response }
        val chunks = provider(transport).translate(image, Language.SPANISH).toList()
        assertEquals(listOf("Hola", " mundo", "!"), chunks.texts())
        assertTrue(response.closed)
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun requestUsesStreamingEndpointHeaderAndPrompt() = runTest {
        val transport = RecordingTransport { FakeResponse(200, sse("x")) }
        provider(transport, "gemini-3.5-flash-lite").translate(image, Language.GERMAN).toList()
        val req = transport.requests.single()
        assertEquals("POST", req.method)
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:streamGenerateContent?alt=sse",
            req.url,
        )
        // La clave viaja en cabecera, nunca en la URL.
        assertEquals("YOUR_GEMINI_API_KEY", req.headers["x-goog-api-key"])
        assertFalse(req.url.contains("key="))
        val body = Json.parseToJsonElement(req.body!!.decodeToString()).jsonObject
        val system = body["systemInstruction"]!!.jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        assertEquals(TranslationPrompt.systemInstruction(Language.GERMAN), system)
        val parts = body["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray
        val inline = parts[0].jsonObject["inlineData"]!!.jsonObject
        assertEquals("image/jpeg", inline["mimeType"]!!.jsonPrimitive.content)
        assertEquals("AQID", inline["data"]!!.jsonPrimitive.content)
    }

    @Test
    fun ignoresThoughtPartsAndNonDataLines() = runTest {
        val lines = listOf(
            ": keep-alive",
            "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"pensando\",\"thought\":true},{\"text\":\"Hola\"}]}}]}",
            "",
        )
        val chunks = provider(RecordingTransport { FakeResponse(200, lines) }).translate(image, Language.SPANISH).toList()
        assertEquals(listOf("Hola"), chunks.texts())
    }

    @Test
    fun emptyStreamIsEmptyResponse() = runTest {
        assertThrows<TranslationException.EmptyResponse> {
            provider(RecordingTransport { FakeResponse(200, emptyList()) }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun invalidKeyIsMapped() = runTest {
        val body = """{"error":{"code":400,"message":"API key not valid.","status":"INVALID_ARGUMENT",
            "details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"API_KEY_INVALID"}]}}"""
        assertThrows<TranslationException.InvalidKey> {
            provider(RecordingTransport { FakeResponse(400, body.lines()) }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun rejectedKeyTypeIsMapped() = runTest {
        // Error real que reportan las nuevas claves «AQ.» en el foro de Google (2026).
        val body = """{"error":{"code":401,"message":"Expected OAuth 2 access token, login cookie or other valid authentication credential.",
            "status":"UNAUTHENTICATED","details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"ACCESS_TOKEN_TYPE_UNSUPPORTED"}]}}"""
        assertThrows<TranslationException.KeyTypeUnsupported> {
            provider(RecordingTransport { FakeResponse(401, body.lines()) }).verify()
        }
    }

    @Test
    fun quotaIsMapped() = runTest {
        val body = """{"error":{"code":429,"message":"Quota exceeded","status":"RESOURCE_EXHAUSTED"}}"""
        assertThrows<TranslationException.QuotaExceeded> {
            provider(RecordingTransport { FakeResponse(429, listOf(body)) }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun serverErrorsAreUnavailable() = runTest {
        assertThrows<TranslationException.Unavailable> {
            provider(RecordingTransport { FakeResponse(503, listOf("Service Unavailable")) }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun unknownModelIsMapped() = runTest {
        assertThrows<TranslationException.ModelNotFound> {
            provider(RecordingTransport { FakeResponse(404, listOf("""{"error":{"code":404,"status":"NOT_FOUND"}}""")) }).verify()
        }
    }

    @Test
    fun networkFailureIsNoConnection() = runTest {
        val transport = HttpTransport { throw UnknownHostException("generativelanguage.googleapis.com") }
        assertThrows<TranslationException.NoConnection> { provider(transport).translate(image, Language.SPANISH).toList() }
        assertThrows<TranslationException.NoConnection> { provider(transport).verify() }
    }

    @Test
    fun connectionDroppedMidStreamIsNoConnection() = runTest {
        val response = object : HttpResponse {
            override val code = 200
            override fun lines() = sequence {
                yield(sse("Hola").first())
                throw IOException("reset")
            }
            override fun close() {}
        }
        assertThrows<TranslationException.NoConnection> {
            provider(RecordingTransport { response }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun blockedPromptIsMapped() = runTest {
        val lines = listOf("data: {\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}")
        assertThrows<TranslationException.Blocked> {
            provider(RecordingTransport { FakeResponse(200, lines) }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun errorInsideStreamIsMapped() = runTest {
        val lines = listOf("data: {\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}")
        assertThrows<TranslationException.QuotaExceeded> {
            provider(RecordingTransport { FakeResponse(200, lines) }).translate(image, Language.SPANISH).toList()
        }
    }

    @Test
    fun cancellingStopsAndClosesResponse() = runTest {
        val response = FakeResponse(200, sse("uno", "dos", "tres"))
        val first = provider(RecordingTransport { response }).translate(image, Language.SPANISH).first()
        assertEquals("uno", (first as TranslationEvent.Text).text)
        assertTrue(response.closed)
    }

    @Test
    fun verifyUsesMetadataEndpoint() = runTest {
        val transport = RecordingTransport { FakeResponse(200, listOf("{}")) }
        provider(transport, "gemini-3.5-flash-lite").verify()
        val req = transport.requests.single()
        assertEquals("GET", req.method)
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite", req.url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsModelNamesThatCouldAlterTheUrl() {
        GeminiProvider("YOUR_GEMINI_API_KEY", "../evil?x=", transport = { error("unused") })
    }

    private suspend inline fun <reified T : Throwable> assertThrows(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.simpleName}")
        } catch (e: Throwable) {
            if (e !is T) throw AssertionError("Expected ${T::class.simpleName} but got $e", e)
        }
    }
}

internal fun List<TranslationEvent>.texts(): List<String> = filterIsInstance<TranslationEvent.Text>().map { it.text }
