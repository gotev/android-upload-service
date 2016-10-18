package net.gotev.uploadservice;

import android.content.Intent;

import net.gotev.uploadservice.http.HttpConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Implements an HTTP Multipart upload task.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class MultipartUploadTask extends HttpUploadTask {

    protected static final String PARAM_UTF8_CHARSET = "multipartUtf8Charset";

    private static final String BOUNDARY_SIGNATURE = "-------AndroidUploadService";
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    // properties associated to each file
    protected static final String PROPERTY_REMOTE_FILE_NAME = "httpRemoteFileName";
    protected static final String PROPERTY_CONTENT_TYPE = "httpContentType";
    protected static final String PROPERTY_PARAM_NAME = "httpParamName";

    private String boundary;
    private byte[] boundaryBytes;
    private byte[] trailerBytes;
    private boolean isUtf8Charset;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        boundary = getBoundary();
        boundaryBytes = getBoundaryBytes();
        trailerBytes = getTrailerBytes();
        isUtf8Charset = intent.getBooleanExtra(PARAM_UTF8_CHARSET, false);

        if (params.getFiles().size() <= 1) {
            httpParams.addRequestHeader("Connection", "close");
        } else {
            httpParams.addRequestHeader("Connection", "Keep-Alive");
        }

        httpParams.addRequestHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return (getRequestParametersLength() + getFilesLength() + trailerBytes.length);
    }

    @Override
    protected void writeBody(HttpConnection connection) throws IOException {
        writeRequestParameters(connection);
        writeFiles(connection);
        connection.writeBody(trailerBytes);
    }

    private String getBoundary() {
        return BOUNDARY_SIGNATURE + System.currentTimeMillis();
    }

    private byte[] getBoundaryBytes() throws UnsupportedEncodingException {
        return (NEW_LINE + TWO_HYPHENS + boundary + NEW_LINE).getBytes(US_ASCII);
    }

    private byte[] getTrailerBytes() throws UnsupportedEncodingException {
        return (NEW_LINE + TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).getBytes(US_ASCII);
    }

    private long getFilesLength() throws UnsupportedEncodingException {
        long total = 0;

        for (UploadFile file : params.getFiles()) {
            total += getTotalMultipartBytes(file, isUtf8Charset);
        }

        return total;
    }

    private long getRequestParametersLength() throws UnsupportedEncodingException {
        long parametersBytes = 0;

        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter
                parametersBytes += boundaryBytes.length
                                + parameter.getMultipartBytes(isUtf8Charset).length;
            }
        }

        return parametersBytes;
    }

    private byte[] getMultipartHeader(UploadFile file, boolean isUtf8)
            throws UnsupportedEncodingException {
        String header = "Content-Disposition: form-data; name=\"" +
                file.getProperty(PROPERTY_PARAM_NAME) + "\"; filename=\"" +
                file.getProperty(PROPERTY_REMOTE_FILE_NAME) + "\"" + NEW_LINE +
                "Content-Type: " + file.getProperty(PROPERTY_CONTENT_TYPE) +
                NEW_LINE + NEW_LINE;

        return header.getBytes(isUtf8 ? UTF8 : US_ASCII);
    }

    private long getTotalMultipartBytes(UploadFile file, boolean isUtf8)
            throws UnsupportedEncodingException {
        return boundaryBytes.length + getMultipartHeader(file, isUtf8).length + file.length(service);
    }

    private void writeRequestParameters(HttpConnection connection) throws IOException {
        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                connection.writeBody(boundaryBytes);
                byte[] formItemBytes = parameter.getMultipartBytes(isUtf8Charset);
                connection.writeBody(formItemBytes);

                uploadedBytes += boundaryBytes.length + formItemBytes.length;
                broadcastProgress(uploadedBytes, totalBytes);
            }
        }
    }

    private void writeFiles(HttpConnection connection) throws IOException {
        for (UploadFile file : params.getFiles()) {
            if (!shouldContinue)
                break;

            connection.writeBody(boundaryBytes);
            byte[] headerBytes = getMultipartHeader(file, isUtf8Charset);
            connection.writeBody(headerBytes);

            uploadedBytes += boundaryBytes.length + headerBytes.length;
            broadcastProgress(uploadedBytes, totalBytes);

            final InputStream stream = file.getStream(service);
            writeStream(stream);
        }
    }

    @Override
    protected void onSuccessfulUpload() {
        for (UploadFile file : params.getFiles()) {
            addSuccessfullyUploadedFile(file.getPath());
        }
        params.getFiles().clear();
    }

}
