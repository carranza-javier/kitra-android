package com.kettenki.kitra.net

import com.kettenki.kitra.core.HttpRequest
import com.kettenki.kitra.core.HttpResponse
import com.kettenki.kitra.core.HttpTransport
import java.net.HttpURLConnection
import java.net.URL

/** Transporte con HttpURLConnection de la plataforma: sin dependencias extra. */
class UrlConnectionTransport : HttpTransport {

    override fun execute(request: HttpRequest): HttpResponse {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        connection.requestMethod = request.method
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.useCaches = false
        request.headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        try {
            request.body?.let { body ->
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            return Response(connection, code)
        } catch (e: Exception) {
            connection.disconnect()
            throw e
        }
    }

    private class Response(private val connection: HttpURLConnection, override val code: Int) : HttpResponse {
        private val reader by lazy {
            val stream = if (code < 400) connection.inputStream else connection.errorStream
            stream?.bufferedReader(Charsets.UTF_8)
        }

        override fun lines(): Sequence<String> = reader?.lineSequence() ?: emptySequence()

        // Desconectar desde otro hilo desbloquea una lectura en curso (cancelación).
        override fun close() = connection.disconnect()
    }
}
