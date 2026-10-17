package com.kettenki.kitra.core

import java.io.Closeable

/**
 * Transporte HTTP mínimo y bloqueante. La app lo implementa con HttpURLConnection y los tests
 * con uno falso, para que la lógica de Gemini no dependa de la plataforma.
 */
fun interface HttpTransport {
    /** Lanza [java.io.IOException] si no hay conexión. */
    fun execute(request: HttpRequest): HttpResponse
}

class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray? = null,
)

interface HttpResponse : Closeable {
    val code: Int

    /** Líneas del cuerpo, leídas a medida que llegan. */
    fun lines(): Sequence<String>

    fun bodyText(): String = lines().joinToString("\n")
}
