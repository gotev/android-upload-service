package net.gotev.uploadservice

import net.gotev.uploadservice.network.BodyWriter

/**
 * Implements a binary file upload task.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
class BinaryUploadTask : HttpUploadTask() {

    private val file by lazy {
        params.files.first().handler
    }

    override val bodyLength: Long
        get() = file.size(context)

    override fun onWriteRequestBody(bodyWriter: BodyWriter) {
        bodyWriter.writeStream(file.stream(context), this)
    }

    override fun onSuccessfulUpload() {
        addSuccessfullyUploadedFile(params.files.first())
    }
}
