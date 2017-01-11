package net.gotev.uploadservice.http;

import java.io.IOException;

/**
 * Exposes the methods to be implemented to write the request body.
 * @author Aleksandar Gotev
 */

public interface BodyWriter {

    /**
     * Write a byte array into the request body.
     * @param bytes array with the bytes to write
     * @throws IOException if an error occurs while writing
     */
    void write(byte[] bytes) throws IOException;

    /**
     * Write a portion of a byte array into the request body.
     * @param bytes array with the bytes to write
     * @param lengthToWriteFromStart how many bytes to write, starting from the first one in
     *                               the array
     * @throws IOException if an error occurs while writing
     */
    void write(byte[] bytes, int lengthToWriteFromStart) throws IOException;

    /**
     * Ensures the bytes written to the body are all transmitted to the server and clears
     * the local buffer.
     * @throws IOException if an error occurs while flushing the buffer
     */
    void flush() throws IOException;
}
