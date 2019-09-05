package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.network.HttpConnection
import net.gotev.uploadservice.network.HttpStack
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Implementation of the OkHttp Stack.
 * @author Aleksandar Gotev
 */
class OkHttpStack(private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()) : HttpStack {
    @Throws(IOException::class)
    override fun createNewConnection(method: String, url: String): HttpConnection {
        return OkHttpStackConnection(client, method, url)
    }
}
