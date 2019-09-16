package net.gotev.uploadservice

import net.gotev.uploadservice.data.NameValue
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.network.BodyWriter

/**
 * Implements an HTTP Multipart upload task.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
class MultipartUploadTask : HttpUploadTask() {

    companion object {
        private const val BOUNDARY_SIGNATURE = "-------AndroidUploadService"
        private const val NEW_LINE = "\r\n"
        private const val TWO_HYPHENS = "--"
        private val US_ASCII = Charsets.US_ASCII

        // properties associated to each file
        const val PROPERTY_REMOTE_FILE_NAME = "httpRemoteFileName"
        const val PROPERTY_CONTENT_TYPE = "httpContentType"
        const val PROPERTY_PARAM_NAME = "httpParamName"
    }

    private val charset = Charsets.UTF_8

    private val boundary by lazy {
        BOUNDARY_SIGNATURE + System.nanoTime()
    }

    private val boundaryBytes by lazy {
        (TWO_HYPHENS + boundary + NEW_LINE).toByteArray(US_ASCII)
    }

    private val trailerBytes by lazy {
        (TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).toByteArray(US_ASCII)
    }

    private val newLineBytes by lazy {
        NEW_LINE.toByteArray(charset)
    }

    override val bodyLength: Long
        get() = requestParametersLength + filesLength + trailerBytes.size

    private val filesLength: Long
        get() = params.files.map { it.getTotalMultipartBytes() }.sum()

    // the bytes needed for every parameter are the sum of the boundary bytes
    // and the bytes occupied by the parameter
    private val requestParametersLength: Long
        get() {
            var parametersBytes: Long = 0
            val params = httpParams

            if (params.requestParameters.isNotEmpty()) {
                for (parameter in params.requestParameters) {
                    parametersBytes += (boundaryBytes.size + parameter.getMultipartBytes().size).toLong()
                }
            }

            return parametersBytes
        }

    private fun NameValue.getMultipartBytes(): ByteArray {
        return ("Content-Disposition: form-data; name=\"" + name + "\""
                + NEW_LINE + NEW_LINE + value + NEW_LINE).toByteArray(charset)
    }

    private fun UploadFile.getMultipartHeader(): ByteArray {
        val header = "Content-Disposition: form-data; name=\"" +
                properties[PROPERTY_PARAM_NAME] + "\"; filename=\"" +
                properties[PROPERTY_REMOTE_FILE_NAME] + "\"" + NEW_LINE +
                "Content-Type: " + properties[PROPERTY_CONTENT_TYPE] +
                NEW_LINE + NEW_LINE

        return header.toByteArray(charset)
    }

    private fun UploadFile.getTotalMultipartBytes(): Long {
        return boundaryBytes.size.toLong() +
                getMultipartHeader().size.toLong() +
                handler.size(context) +
                newLineBytes.size.toLong()
    }

    private fun BodyWriter.writeRequestParameters() {
        val params = httpParams

        if (params.requestParameters.isNotEmpty()) {
            for (parameter in params.requestParameters) {
                val formItemBytes = parameter.getMultipartBytes()
                write(boundaryBytes)
                write(formItemBytes)
                broadcastProgress((boundaryBytes.size + formItemBytes.size).toLong())
            }
        }
    }

    private fun BodyWriter.writeFiles() {
        for (file in params.files) {
            if (!shouldContinue) break

            val headerBytes = file.getMultipartHeader()

            write(boundaryBytes)
            write(headerBytes)
            broadcastProgress((boundaryBytes.size + headerBytes.size).toLong())

            writeStream(file.handler.stream(context), this@MultipartUploadTask)
            write(newLineBytes)
            broadcastProgress(newLineBytes.size.toLong())
        }
    }

    override fun performInitialization() {
        httpParams.apply {
            addHeader("Content-Type", "multipart/form-data; boundary=$boundary")
            addHeader("Connection", if (params.files.size <= 1) "close" else "Keep-Alive")
        }
    }

    override fun onWriteRequestBody(bodyWriter: BodyWriter) {
        //reset uploaded bytes when the body is ready to be written
        //because sometimes this gets invoked when network changes
        resetUploadedBytes()

        bodyWriter.apply {
            writeRequestParameters()
            writeFiles()
            write(trailerBytes)
        }

        broadcastProgress(trailerBytes.size.toLong())
    }

    override fun onSuccessfulUpload() {
        addAllFilesToSuccessfullyUploadedFiles()
    }

}
