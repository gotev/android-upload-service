package net.gotev.uploadservice.network.hurl

import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.NameValue
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.network.HttpConnection
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.LinkedHashMap

/**
 * [HttpConnection] implementation using [HttpURLConnection].
 * @author gotev (Aleksandar Gotev)
 */
class HurlStackConnection(
        method: String,
        url: String,
        followRedirects: Boolean,
        useCaches: Boolean,
        connectTimeout: Int,
        readTimeout: Int) : HttpConnection {

    private val connection: HttpURLConnection
    private val uuid = UUID.randomUUID().toString()

    private fun String.createConnection(): HttpURLConnection {
        val url = URL(trim())

        return if (url.protocol.toLowerCase() == "https") {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }
    }

    init {
        UploadServiceLogger.debug(javaClass.simpleName, "creating new HttpURLConnection (uuid: $uuid)")

        connection = url.createConnection().apply {
            doInput = true
            doOutput = true
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
            this.useCaches = useCaches
            instanceFollowRedirects = followRedirects
            requestMethod = method
        }
    }

    private val responseBody: ByteArray
        @Throws(IOException::class)
        get() {
            return if (connection.responseCode / 100 == 2) {
                connection.inputStream
            } else {
                connection.errorStream
            }.use {
                it.readBytes()
            }
        }

    private val responseHeaders: LinkedHashMap<String, String>
        @Throws(IOException::class)
        get() {
            val headers = connection.headerFields ?: return LinkedHashMap(0)

            return LinkedHashMap<String, String>(headers.size).apply {
                headers.entries
                        .filter { it.key != null && it.value != null && it.value.isNotEmpty() }
                        .forEach { (key, values) ->
                            this[key] = values.first()
                        }
            }
        }

    @Throws(IOException::class)
    override fun setHeaders(requestHeaders: List<NameValue>): HttpConnection {
        for (param in requestHeaders) {
            connection.setRequestProperty(param.name.trim(), param.value.trim())
        }

        return this
    }

    override fun setTotalBodyBytes(totalBodyBytes: Long, isFixedLengthStreamingMode: Boolean): HttpConnection {
        connection.apply {
            if (isFixedLengthStreamingMode) {
                setFixedLengthStreamingMode(totalBodyBytes)
            } else {
                setChunkedStreamingMode(0)
            }
        }

        return this
    }

    @Throws(IOException::class)
    override fun getResponse(delegate: HttpConnection.RequestBodyDelegate): ServerResponse {
        HurlBodyWriter(connection.outputStream).use {
            delegate.onWriteRequestBody(it)
        }

        return ServerResponse(connection.responseCode, responseBody, responseHeaders)
    }

    override fun close() {
        UploadServiceLogger.debug(javaClass.simpleName, "closing HttpURLConnection (uuid: $uuid)")

        try {
            connection.inputStream.close()
        } catch (ignored: Throwable) {
        }

        try {
            connection.outputStream.flush()
        } catch (ignored: Throwable) {
        }

        try {
            connection.outputStream.close()
        } catch (ignored: Throwable) {
        }

        try {
            connection.disconnect()
        } catch (ignored: Throwable) {
        }
    }
}
