package net.gotev.uploadservice.http;

import net.gotev.uploadservice.NameValue;

import java.io.IOException;
import java.util.List;

/**
 * Defines the methods that has to be implemented by an HTTP connection.
 * If you're implementing your custom HTTP connection, remember to never cache anything,
 * especially in writeBody methods, as this will surely cause memory issues when uploading
 * large files. The only things which you are allowed to cache are the response code and body
 * from the server, which must not be large though.
 * @author gotev (Aleksandar Gotev)
 */
public interface HttpConnection {

    /**
     * Set request headers.
     * @param requestHeaders request headers to set
     * @param isFixedLengthStreamingMode true if the fixed length streaming mode must be used. If
     *                                   it's false, chunked streaming mode has to be used
     * @param totalBodyBytes total number of bytes
     * @throws IOException if an error occurs while setting request headers
     */
    void setHeaders(List<NameValue> requestHeaders, boolean isFixedLengthStreamingMode,
                    long totalBodyBytes) throws IOException;

    /**
     * Write a byte array into the request body.
     * @param bytes array with the bytes to write
     * @throws IOException if an error occurs while writing
     */
    void writeBody(byte[] bytes) throws IOException;

    /**
     * Write a portion of a byte array into the request body.
     * @param bytes array with the bytes to write
     * @param lengthToWriteFromStart how many bytes to write, starting from the first one in
     *                               the array
     * @throws IOException if an error occurs while writing
     */
    void writeBody(byte[] bytes, int lengthToWriteFromStart) throws IOException;

    /**
     * Gets the HTTP response code from the server.
     * @return an integer representing the HTTP response code (e.g. 200)
     * @throws IOException if an error occurs while getting the server response code
     */
    int getServerResponseCode() throws IOException;

    /**
     * Gets the server response body.
     * @return response body bytes
     * @throws IOException if an error occurs while getting the server response body
     */
    byte[] getServerResponseBody() throws IOException;

    /**
     * Closes the connection and frees all the allocated resources.
     */
    void close();
}
