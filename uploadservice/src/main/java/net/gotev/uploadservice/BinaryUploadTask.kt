package net.gotev.uploadservice

import net.gotev.uploadservice.network.BodyWriter

import java.io.IOException
import java.io.UnsupportedEncodingException

/**
 * Implements a binary file upload task.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
class BinaryUploadTask : HttpUploadTask() {

    private val file by lazy {
        params.files[0].handler
    }

    @Throws(UnsupportedEncodingException::class)
    override fun getBodyLength() = file.size(service)

    @Throws(IOException::class)
    override fun onWriteRequestBody(bodyWriter: BodyWriter) {
        bodyWriter.writeStream(file.stream(service), this)
    }

    override fun onSuccessfulUpload() {
        addSuccessfullyUploadedFile(params.files[0])
    }
}
