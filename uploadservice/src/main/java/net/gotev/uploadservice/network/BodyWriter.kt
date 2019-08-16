package net.gotev.uploadservice.network

import net.gotev.uploadservice.UploadService
import java.io.Closeable

import java.io.IOException
import java.io.InputStream

/**
 * Exposes the methods to be implemented to write the request body.
 * If you want to use an internal cache or buffer, remember to always get its size value from
 * [UploadService.BUFFER_SIZE] and to clear everything when not needed to prevent memory leaks
 * @author Aleksandar Gotev
 */

abstract class BodyWriter : Closeable {

    /**
     * Receives the stream write progress and has the ability to cancel it.
     */
    interface OnStreamWriteListener {
        /**
         * Indicates if the writing of the stream into the body should continue.
         * @return true to continue writing the stream into the body, false to cancel
         */
        fun shouldContinueWriting(): Boolean

        /**
         * Called every time that a bunch of bytes were written to the body
         * @param bytesWritten number of written bytes
         */
        fun onBytesWritten(bytesWritten: Int)
    }

    /**
     * Writes an input stream to the request body.
     * The stream will be automatically closed after successful write or if an exception is thrown.
     * @param stream input stream from which to read
     * @param listener listener which gets notified when bytes are written and which controls if
     * the transfer should continue
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun writeStream(stream: InputStream, listener: OnStreamWriteListener) {
        val buffer = ByteArray(UploadService.BUFFER_SIZE)
        var bytesRead: Int

        stream.use {
            while (listener.shouldContinueWriting()) {
                bytesRead = it.read(buffer, 0, buffer.size)
                if (bytesRead <= 0) break

                write(buffer, bytesRead)
                flush()
                listener.onBytesWritten(bytesRead)
            }
        }
    }

    /**
     * Write a byte array into the request body.
     * @param bytes array with the bytes to write
     * @throws IOException if an error occurs while writing
     */
    @Throws(IOException::class)
    abstract fun write(bytes: ByteArray)

    /**
     * Write a portion of a byte array into the request body.
     * @param bytes array with the bytes to write
     * @param lengthToWriteFromStart how many bytes to write, starting from the first one in
     * the array
     * @throws IOException if an error occurs while writing
     */
    @Throws(IOException::class)
    abstract fun write(bytes: ByteArray, lengthToWriteFromStart: Int)

    /**
     * Ensures the bytes written to the body are all transmitted to the server and clears
     * the local buffer.
     * @throws IOException if an error occurs while flushing the buffer
     */
    @Throws(IOException::class)
    abstract fun flush()
}
