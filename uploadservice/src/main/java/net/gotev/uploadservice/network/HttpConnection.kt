package net.gotev.uploadservice.network

import net.gotev.uploadservice.data.NameValue

import java.io.IOException

/**
 * Defines the methods that has to be implemented by an HTTP connection.
 * If you're implementing your custom HTTP connection, remember to never cache anything,
 * especially in BodyWriter methods, as this will surely cause memory issues when uploading
 * large files. The only things which you are allowed to cache are the response code and body
 * from the server, which must not be large though.
 * @author gotev (Aleksandar Gotev)
 */
interface HttpConnection {

    /**
     * Delegate called when the body is ready to be written.
     */
    interface RequestBodyDelegate {

        /**
         * Handles the writing of the request body.
         * @param bodyWriter object with which to write on the body
         * @throws IOException if an error occurs while writing the body
         */
        @Throws(IOException::class)
        fun onWriteRequestBody(bodyWriter: BodyWriter)
    }

    /**
     * Set request headers.
     * @param requestHeaders request headers to set
     * @throws IOException if an error occurs while setting request headers
     * @return instance
     */
    @Throws(IOException::class)
    fun setHeaders(requestHeaders: List<NameValue>): HttpConnection

    /**
     * Sets the total body bytes.
     * @param totalBodyBytes total number of bytes
     * @param isFixedLengthStreamingMode true if the fixed length streaming mode must be used. If
     * it's false, chunked streaming mode has to be used.
     * https://gist.github.com/CMCDragonkai/6bfade6431e9ffb7fe88
     * @return instance
     */
    fun setTotalBodyBytes(totalBodyBytes: Long, isFixedLengthStreamingMode: Boolean): HttpConnection

    /**
     * Gets the server response.
     * @return object containing the server response status, headers and body.
     * @param delegate delegate which handles the writing of the request body
     * @throws IOException if an error occurs while getting the server response
     * @return response from server
     */
    @Throws(IOException::class)
    fun getResponse(delegate: RequestBodyDelegate): ServerResponse

    /**
     * Closes the connection and frees all the allocated resources.
     */
    fun close()
}
