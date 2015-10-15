package com.alexbbb.uploadservice;

import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

/**
 * Implements an HTTP Multipart upload task.
 *
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 * @author cankov
 */
class MultipartUploadTask extends HttpUploadTask {

    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private final ArrayList<MultipartUploadFile> files;
    private final ArrayList<NameValue> parameters;

    private String boundary;
    private byte[] boundaryBytes;
    private byte[] trailerBytes;

    MultipartUploadTask(UploadService service, Intent intent) {
        super(service, intent);
        this.files = intent.getParcelableArrayListExtra(UploadService.PARAM_FILES);
        this.parameters = intent.getParcelableArrayListExtra(UploadService.PARAM_REQUEST_PARAMETERS);
    }

    @Override
    protected void upload() throws IOException {
        boundary = getBoundary();
        boundaryBytes = getBoundaryBytes();
        trailerBytes = getTrailerBytes();
        super.upload();
    }

    @Override
    protected HttpURLConnection getHttpURLConnection() throws IOException {
        final HttpURLConnection conn = super.getHttpURLConnection();

        if (files.size() <= 1) {
            conn.setRequestProperty("Connection", "close");
        } else {
            conn.setRequestProperty("Connection", "Keep-Alive");
        }
        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        return conn;
    }

    private String getBoundary() {
        final StringBuilder builder = new StringBuilder();
        builder.append("---------------------------").append(System.currentTimeMillis());
        return builder.toString();
    }

    private byte[] getBoundaryBytes() throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();
        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(NEW_LINE);
        return builder.toString().getBytes("US-ASCII");
    }

    private byte[] getTrailerBytes() throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();
        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(TWO_HYPHENS).append(NEW_LINE);
        return builder.toString().getBytes("US-ASCII");
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        // get the content length of the entire HTTP/Multipart request body
        long parameterBytes = getRequestParametersLength();
        final long totalFileBytes = getFilesLength();

        final long bodyLength = parameterBytes + totalFileBytes + trailerBytes.length;
        return bodyLength;
    }

    private long getFilesLength() throws UnsupportedEncodingException {
        long total = 0;

        for (MultipartUploadFile file : files) {
            total += file.getTotalMultipartBytes(boundaryBytes.length);
        }

        return total;
    }

    private long getRequestParametersLength() throws UnsupportedEncodingException {
        long parametersBytes = 0;

        if (!parameters.isEmpty()) {
            for (final NameValue parameter : parameters) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter. Check setRequestParameters method
                parametersBytes += boundaryBytes.length + parameter.getBytes().length;
            }
        }

        return parametersBytes;
    }

    @Override
    protected void writeBody() throws IOException {
        writeRequestParameters();
        writeFiles();
        requestStream.write(trailerBytes, 0, trailerBytes.length);
    }

    private void writeRequestParameters() throws IOException {
        if (!parameters.isEmpty()) {
            for (final NameValue parameter : parameters) {
                requestStream.write(boundaryBytes, 0, boundaryBytes.length);
                byte[] formItemBytes = parameter.getBytes();
                requestStream.write(formItemBytes, 0, formItemBytes.length);

                uploadedBodyBytes += boundaryBytes.length + formItemBytes.length;
                broadcastProgress(uploadedBodyBytes, totalBodyBytes);
            }
        }
    }

    private void writeFiles() throws IOException {
        for (MultipartUploadFile file : files) {
            if (!shouldContinue)
                continue;

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
