package net.gotev.uploadservice.http;

import net.gotev.uploadservice.NameValue;
import net.gotev.uploadservice.ServerResponse;

import java.io.IOException;
import java.util.List;

/**
 * Defines the methods that has to be implemented by an HTTP connection.
 * If you're implementing your custom HTTP connection, remember to never cache anything,
 * especially in BodyWriter methods, as this will surely cause memory issues when uploading
 * large files. The only things which you are allowed to cache are the response code and body
 * from the server, which must not be large though.
 * @author gotev (Aleksandar Gotev)
 */
public interface HttpConnection {

    /**
     * Delegate called when the body is ready to be written.
     */
    interface RequestBodyDelegate {

        /**
         * Handles the writing of the request body.
         * @param bodyWriter object with which to write on the body
         * @throws IOException if an error occurs while writing the body
         */
        void onBodyReady(BodyWriter bodyWriter) throws IOException;
    }

    /**
     * Set request headers.
     * @param requestHeaders request headers to set
     * @throws IOException if an error occurs while setting request headers
     * @return instance
     */
    HttpConnection setHeaders(List<NameValue> requestHeaders) throws IOException;

    /**
     * Sets the total body bytes.
     * @param totalBodyBytes total number of bytes
     * @param isFixedLengthStreamingMode true if the fixed length streaming mode must be used. If
     *                                   it's false, chunked streaming mode has to be used
     * @return instance
     */
    HttpConnection setTotalBodyBytes(long totalBodyBytes, boolean isFixedLengthStreamingMode);

    /**
     * Gets the server response.
     * @return object containing the server response status, headers and body.
     * @param delegate delegate which handles the writing of the request body
     * @throws IOException if an error occurs while getting the server response
     * @return response from server
     */
    ServerResponse getResponse(RequestBodyDelegate delegate) throws IOException;

    /**
     * Closes the connection and frees all the allocated resources.
     */
    void close();
}
