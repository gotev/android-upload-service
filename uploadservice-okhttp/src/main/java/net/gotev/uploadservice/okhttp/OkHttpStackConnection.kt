package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.Logger
import net.gotev.uploadservice.NameValue
import net.gotev.uploadservice.http.HttpConnection
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * [HttpConnection] implementation using OkHttpClient.
 *
 * @author Aleksandar Gotev
 */
class OkHttpStackConnection(private val httpClient: OkHttpClient, private val httpMethod: String, url: String) : HttpConnection {

    private val requestBuilder = Request.Builder().url(URL(url))
    private var bodyLength = 0L
    private var contentType: MediaType? = null
    private val uuid = UUID.randomUUID().toString()

    init {
        Logger.debug(javaClass.simpleName, "creating new OkHttp connection (uuid: $uuid)")
    }

    @Throws(IOException::class)
    override fun setHeaders(requestHeaders: List<NameValue>): HttpConnection {
        for (param in requestHeaders) {
            if ("content-type" == param.name.trim().toLowerCase())
                contentType = param.value.trim().toMediaTypeOrNull()

            requestBuilder.header(param.name.trim(), param.value.trim())
        }

        return this
    }

    override fun setTotalBodyBytes(totalBodyBytes: Long, isFixedLengthStreamingMode: Boolean): HttpConnection {
        // http://stackoverflow.com/questions/33921894/how-do-i-enable-disable-chunked-transfer-encoding-for-a-multi-part-post-that-inc#comment55679982_33921894
        bodyLength = if (isFixedLengthStreamingMode) totalBodyBytes else -1

        return this
    }

    private fun request(delegate: HttpConnection.RequestBodyDelegate) =
            requestBuilder.method(
                    method = httpMethod,
                    body = body(httpMethod, bodyLength, contentType, delegate)
            ).build()

    @Throws(IOException::class)
    override fun getResponse(delegate: HttpConnection.RequestBodyDelegate) = httpClient
            .newCall(request(delegate))
            .execute()
            .use { it.asServerResponse() }

    // Resources are automatically freed after usage. Log only.
    override fun close() {
        Logger.debug(javaClass.simpleName, "closing OkHttp connection (uuid: $uuid)")
    }
}
