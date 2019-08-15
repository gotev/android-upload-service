package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.http.BodyWriter
import okio.BufferedSink
import java.io.Closeable
import java.io.IOException

/**
 * @author Aleksandar Gotev
 */

class OkHttpBodyWriter(private val sink: BufferedSink) : BodyWriter(), Closeable {
    @Throws(IOException::class)
    override fun write(bytes: ByteArray) {
        sink.write(bytes)
    }

    @Throws(IOException::class)
    override fun write(bytes: ByteArray, lengthToWriteFromStart: Int) {
        sink.write(bytes, 0, lengthToWriteFromStart)
    }

    @Throws(IOException::class)
    override fun flush() {
        sink.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        sink.close()
    }
}
