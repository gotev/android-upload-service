package net.gotev.uploadservice.protocols.binary

import net.gotev.uploadservice.HttpUploadTask
import net.gotev.uploadservice.extensions.addHeader
import net.gotev.uploadservice.network.BodyWriter

/**
 * Implements a binary file upload task.
 */
class BinaryUploadTask : HttpUploadTask() {
    private val file by lazy { params.files.first().handler }

    override val bodyLength: Long
        get() = file.size(context)

    override fun performInitialization() {
        with(httpParams.requestHeaders) {
            if (none { it.name.lowercase() == "content-type" }) {
                addHeader("Content-Type", file.contentType(context))
            }
        }
    }

    override fun onWriteRequestBody(bodyWriter: BodyWriter) {
        bodyWriter.writeStream(file.stream(context))
    }
}
