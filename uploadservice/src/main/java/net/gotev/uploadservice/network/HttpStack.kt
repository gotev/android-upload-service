package net.gotev.uploadservice.network

import java.io.IOException

interface HttpStack {
    /**
     * Creates a new connection for a given URL and HTTP Method.
     * @param uploadId ID of the upload which requested this connection
     * @param method HTTP Method
     * @param url URL to which to connect to
     * @return new connection object
     * @throws IOException if an error occurs while creating the connection object
     */
    @Throws(IOException::class)
    fun newRequest(uploadId: String, method: String, url: String): HttpRequest
}
