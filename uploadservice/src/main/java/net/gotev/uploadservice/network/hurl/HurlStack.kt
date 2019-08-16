package net.gotev.uploadservice.network.hurl

import net.gotev.uploadservice.network.HttpConnection
import net.gotev.uploadservice.network.HttpStack

import java.io.IOException

/**
 * HttpUrlConnection stack implementation.
 * @author gotev (Aleksandar Gotev)
 */
class HurlStack(private val followRedirects: Boolean = true,
                private val useCaches: Boolean = false,
                private val connectTimeout: Int = 15000,
                private val readTimeout: Int = 30000) : HttpStack {

    @Throws(IOException::class)
    override fun createNewConnection(method: String, url: String): HttpConnection {
        return HurlStackConnection(method, url, followRedirects, useCaches,
                connectTimeout, readTimeout)
    }

}
