package com.alexbbb.uploadservice;

import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * Implements an HTTP Multipart upload task.
 *
 * @author alexbbb (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class MultipartUploadTask extends HttpUploadTask {

    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private String boundary;
    private byte[] boundaryBytes;
    private byte[] trailerBytes;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        boundary = getBoundary();
        boundaryBytes = getBoundaryBytes();
        trailerBytes = getTrailerBytes();
    }

    @Override
    protected void setupHttpUrlConnection(HttpURLConnection connection) throws IOException {
        if (params.getFiles().size() <= 1) {
            connection.setRequestProperty("Connection", "close");
        } else {
            connection.setRequestProperty("Connection", "Keep-Alive");
        }

        connection.setRequestProperty("ENCTYPE", "multipart/form-data");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        // get the content length of the entire HTTP/Multipart request body
        long parameterBytes = getRequestParametersLength();
        final long totalFileBytes = getFilesLength();

        return (parameterBytes + totalFileBytes + trailerBytes.length);
    }

    @Override
    protected void writeBody() throws IOException {
        writeRequestParameters();
        writeFiles();
        requestStream.write(trailerBytes, 0, trailerBytes.length);
    }

    private String getBoundary() {
        return "---------------------------" + System.currentTimeMillis();
    }

    private byte[] getBoundaryBytes() throws UnsupportedEncodingException {
        return (NEW_LINE + TWO_HYPHENS + boundary + NEW_LINE).getBytes("US-ASCII");
    }

    private byte[] getTrailerBytes() throws UnsupportedEncodingException {
        return (NEW_LINE + TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).getBytes("US-ASCII");
    }

    private long getFilesLength() throws UnsupportedEncodingException {
        long total = 0;

        for (UploadFile file : params.getFiles()) {
            total += file.getTotalMultipartBytes(boundaryBytes.length);
        }

        return total;
    }

    private long getRequestParametersLength() throws UnsupportedEncodingException {
        long parametersBytes = 0;

        if (!params.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : params.getRequestParameters()) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter
                parametersBytes += boundaryBytes.length + parameter.getMultipartBytes().length;
            }
        }

        return parametersBytes;
    }

    private void writeRequestParameters() throws IOException {
        if (!params.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : params.getRequestParameters()) {
                requestStream.write(boundaryBytes, 0, boundaryBytes.length);
                byte[] formItemBytes = parameter.getMultipartBytes();
                requestStream.write(formItemBytes, 0, formItemBytes.length);

                uploadedBodyBytes += boundaryBytes.length + formItemBytes.length;
                broadcastProgress(uploadedBodyBytes, totalBodyBytes);
            }
        }
    }

    private void writeFiles() throws IOException {
        for (UploadFile file : params.getFiles()) {
            if (!shouldContinue)
                break;

            requestStream.write(boundaryBytes, 0, boundaryBytes.length);
            byte[] headerBytes = file.getMultipartHeader();
            requestStream.write(headerBytes, 0, headerBytes.length);

            uploadedBodyBytes += boundaryBytes.length + headerBytes.length;
            broadcastProgress(uploadedBodyBytes, totalBodyBytes);

            final InputStream stream = file.getStream();
            writeStream(stream);
        }
    }

}
