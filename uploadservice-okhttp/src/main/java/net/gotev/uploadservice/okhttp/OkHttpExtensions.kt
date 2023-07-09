package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.network.ServerResponse
import okhttp3.Response

/**
 * @author Aleksandar Gotev
 */
private fun String.requiresRequestBody() =
    this == "POST" || this == "PUT" || this == "PATCH" || this == "PROPPATCH" || this == "REPORT"

private fun String.permitsRequestBody() = !(this == "GET" || this == "HEAD")

internal fun String.hasBody(): Boolean {
    val method = trim().uppercase()
    return method.permitsRequestBody() || method.requiresRequestBody()
}

private fun Response.headersHashMap() = LinkedHashMap(headers.toMap())

private fun Response.bodyBytes() = body?.bytes() ?: ByteArray(0)

internal fun Response.asServerResponse() = ServerResponse(code, bodyBytes(), headersHashMap())
