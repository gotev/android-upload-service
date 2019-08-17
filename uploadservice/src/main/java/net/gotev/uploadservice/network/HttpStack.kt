package net.gotev.uploadservice.network

import java.io.IOException

/**
 * Defines the methods that has to be implemented by an HTTP stack.
 * @author gotev (Aleksandar Gotev)
 */
interface HttpStack {

    /**
     * Creates a new connection for a given URL and HTTP Method.
     * @param method HTTP Method
     * @param url URL to which to connect to
     * @return new connection object
     * @throws IOException if an error occurs while creating the connection object
     */
    @Throws(IOException::class)
    fun createNewConnection(method: String, url: String): HttpConnection
}