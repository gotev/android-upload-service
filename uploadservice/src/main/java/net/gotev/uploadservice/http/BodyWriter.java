package net.gotev.uploadservice.http;

import net.gotev.uploadservice.UploadService;

import java.io.IOException;
import java.io.InputStream;

/**
 * Exposes the methods to be implemented to write the request body.
 * If you want to use an internal cache or buffer, remember to always get its size value from
 * {@link UploadService#BUFFER_SIZE} and to clear everything when not needed to prevent memory leaks
 * @author Aleksandar Gotev
 */

public abstract class BodyWriter {

    /**
     * Receives the stream write progress and has the ability to cancel it.
     */
    public interface OnStreamWriteListener {
        /**
         * Indicates if the writing of the stream into the body should continue.
         * @return true to continue writing the stream into the body, false to cancel
         */
        boolean shouldContinueWriting();

        /**
         * Called every time that a bunch of bytes were written to the body
         * @param bytesWritten number of written bytes
         */
        void onBytesWritten(int bytesWritten);
    }

    /**
     * Writes an input stream to the request body.
     * The stream will be automatically closed after successful write or if an exception is thrown.
     * @param stream input stream from which to read
     * @param listener listener which gets notified when bytes are written and which controls if
     *                 the transfer should continue
     * @throws IOException if an I/O error occurs
     */
    public final void writeStream(InputStream stream, OnStreamWriteListener listener) throws IOException {
        if (listener == null)
            throw new IllegalArgumentException("listener MUST not be null!");

        byte[] buffer = new byte[UploadService.BUFFER_SIZE];
        int bytesRead;

        try {
            while (listener.shouldContinueWriting() && (bytesRead = stream.read(buffer, 0, buffer.length)) > 0) {
                write(buffer, bytesRead);
                flush();
                listener.onBytesWritten(bytesRead);
            }
        } finally {
            stream.close();
        }
    }

    /**
     * Write a byte array into the request body.
     * @param bytes array with the bytes to write
     * @throws IOException if an error occurs while writing
     */
    public abstract void write(byte[] bytes) throws IOException;

    /**
     * Write a portion of a byte array into the request body.
     * @param bytes array with the bytes to write
     * @param lengthToWriteFromStart how many bytes to write, starting from the first one in
     *                               the array
     * @throws IOException if an error occurs while writing
     */
    public abstract void write(byte[] bytes, int lengthToWriteFromStart) throws IOException;

    /**
     * Ensures the bytes written to the body are all transmitted to the server and clears
     * the local buffer.
     * @throws IOException if an error occurs while flushing the buffer
     */
    public abstract void flush() throws IOException;
}
