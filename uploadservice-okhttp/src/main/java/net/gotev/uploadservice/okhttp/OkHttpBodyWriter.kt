package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.network.BodyWriter
import okio.BufferedSink
import java.io.IOException

/**
 * @author Aleksandar Gotev
 */

class OkHttpBodyWriter(private val sink: BufferedSink, listener: OnStreamWriteListener) : BodyWriter(listener) {
    @Throws(IOException::class)
    override fun internalWrite(bytes: ByteArray) {
        sink.write(bytes)
    }

    @Throws(IOException::class)
    override fun internalWrite(bytes: ByteArray, lengthToWriteFromStart: Int) {
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
