package net.gotev.uploadservice.network.hurl

import net.gotev.uploadservice.network.BodyWriter

import java.io.IOException
import java.io.OutputStream

class HurlBodyWriter(private val stream: OutputStream, listener: OnStreamWriteListener) : BodyWriter(listener) {
    @Throws(IOException::class)
    override fun internalWrite(bytes: ByteArray) {
        stream.write(bytes)
    }

    @Throws(IOException::class)
    override fun internalWrite(bytes: ByteArray, lengthToWriteFromStart: Int) {
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
