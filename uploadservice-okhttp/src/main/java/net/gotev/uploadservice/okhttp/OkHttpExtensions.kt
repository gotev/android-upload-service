package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.ServerResponse
import net.gotev.uploadservice.http.HttpConnection
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/**
 * @author Aleksandar Gotev
 */
private fun String.requiresRequestBody() = this == "POST" || this == "PUT" || this == "PATCH" || this == "PROPPATCH" || this == "REPORT"

private fun String.permitsRequestBody() = !(this == "GET" || this == "HEAD")

internal fun body(httpMethod: String, bodyLength: Long, contentType: MediaType?, delegate: HttpConnection.RequestBodyDelegate): RequestBody? {
    val method = httpMethod.trim().toUpperCase()

    if (!method.permitsRequestBody() && !method.requiresRequestBody()) return null

    return object : RequestBody() {
        override fun contentLength() = bodyLength

        override fun contentType() = contentType

        override fun writeTo(sink: BufferedSink) {
            OkHttpBodyWriter(sink).apply {
                delegate.onBodyReady(this)
                flush()
            }
        }
    }
}

private fun Response.headersHashMap() = LinkedHashMap(headers.toMap())

private fun Response.bodyBytes() = body?.bytes() ?: ByteArray(0)

internal fun Response.asServerResponse() = ServerResponse(code, bodyBytes(), headersHashMap())

