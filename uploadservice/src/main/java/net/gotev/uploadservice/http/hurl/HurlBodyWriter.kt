package net.gotev.uploadservice.http.hurl

import net.gotev.uploadservice.http.BodyWriter

import java.io.IOException
import java.io.OutputStream

/**
 * @author Aleksandar Gotev
 */

class HurlBodyWriter(private val stream: OutputStream) : BodyWriter() {
    @Throws(IOException::class)
    override fun write(bytes: ByteArray) {
        stream.write(bytes)
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray, lengthToWriteFromStart: Int) {
        stream.write(bytes, 0, lengthToWriteFromStart)
    }

    @Throws(IOException::class)
    override fun flush() {
        stream.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        stream.close()
    }
}
