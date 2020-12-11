package net.gotev.uploadservice.protocols.multipart

import net.gotev.uploadservice.BuildConfig
import net.gotev.uploadservice.HttpUploadTask
import net.gotev.uploadservice.data.NameValue
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.extensions.addHeader
import net.gotev.uploadservice.extensions.asciiByes
import net.gotev.uploadservice.extensions.utf8Bytes
import net.gotev.uploadservice.network.BodyWriter

/**
 * Implements an HTTP Multipart upload task.
 */
class MultipartUploadTask : HttpUploadTask() {

    companion object {
        private const val BOUNDARY_SIGNATURE = "-------UploadService${BuildConfig.VERSION_NAME}-"
        private const val NEW_LINE = "\r\n"
        private const val TWO_HYPHENS = "--"
    }

    private val boundary = BOUNDARY_SIGNATURE + System.nanoTime()
    private val boundaryBytes = (TWO_HYPHENS + boundary + NEW_LINE).asciiByes
    private val trailerBytes = (TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).asciiByes
    private val newLineBytes = NEW_LINE.utf8Bytes

    private val NameValue.multipartHeader: ByteArray
        get() = boundaryBytes + (
            "Content-Disposition: form-data; " +
                "name=\"$name\"$NEW_LINE$NEW_LINE$value$NEW_LINE"
            ).utf8Bytes

    private val UploadFile.multipartHeader: ByteArray
        get() = boundaryBytes + (
            "Content-Disposition: form-data; " +
                "name=\"$parameterName\"; " +
                "filename=\"$remoteFileName\"$NEW_LINE" +
                "Content-Type: $contentType$NEW_LINE$NEW_LINE"
            ).utf8Bytes

    private val UploadFile.totalMultipartBytes: Long
        get() = multipartHeader.size.toLong() + handler.size(context) + newLineBytes.size.toLong()

    private fun BodyWriter.writeRequestParameters() {
        httpParams.requestParameters.forEach {
            write(it.multipartHeader)
        }
    }

    private fun BodyWriter.writeFiles() {
        for (file in params.files) {
            if (!shouldContinue) break

            write(file.multipartHeader)
            writeStream(file.handler.stream(context))
            write(newLineBytes)
        }
    }

    private val requestParametersLength: Long
        get() = httpParams.requestParameters.map { it.multipartHeader.size.toLong() }.sum()

    private val filesLength: Long
        get() = params.files.map { it.totalMultipartBytes }.sum()

    override val bodyLength: Long
        get() = requestParametersLength + filesLength + trailerBytes.size

    override fun performInitialization() {
        httpParams.requestHeaders.apply {
            addHeader("Content-Type", "multipart/form-data; boundary=$boundary")
            addHeader("Connection", if (params.files.size <= 1) "close" else "Keep-Alive")
        }
    }

    override fun onWriteRequestBody(bodyWriter: BodyWriter) {
        // reset uploaded bytes when the body is ready to be written
        // because sometimes this gets invoked when network changes
        resetUploadedBytes()
        setAllFilesHaveBeenSuccessfullyUploaded(false)

        bodyWriter.apply {
            writeRequestParameters()
            writeFiles()
            write(trailerBytes)
        }
    }
}
