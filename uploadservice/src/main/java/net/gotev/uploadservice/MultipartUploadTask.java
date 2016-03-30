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

    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private String boundary;
    private byte[] boundaryBytes;
    private byte[] trailerBytes;
    private boolean isUtf8Charset;

    private final Charset US_ASCII = Charset.forName("US-ASCII");

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        boundary = getBoundary();
        boundaryBytes = getBoundaryBytes();
        trailerBytes = getTrailerBytes();
        isUtf8Charset = intent.getBooleanExtra(PARAM_UTF8_CHARSET, false);

        if (params.getFiles().size() <= 1) {
            params.addRequestHeader("Connection", "close");
        } else {
            params.addRequestHeader("Connection", "Keep-Alive");
        }

        params.addRequestHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        // get the content length of the entire HTTP/Multipart request body
        long parameterBytes = getRequestParametersLength();
        final long totalFileBytes = getFilesLength();

        return (parameterBytes + totalFileBytes + trailerBytes.length);
    }

    @Override
    protected void writeBody(HttpConnection connection) throws IOException {
        writeRequestParameters(connection);
        writeFiles(connection);
        connection.writeBody(trailerBytes);
    }

    private String getBoundary() {
        return "-------AndroidUploadService" + System.currentTimeMillis();
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
            total += file.getTotalMultipartBytes(boundaryBytes.length, isUtf8Charset);
        }

        return total;
    }

    private long getRequestParametersLength() throws UnsupportedEncodingException {
        long parametersBytes = 0;

        if (!params.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : params.getRequestParameters()) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter
                parametersBytes += boundaryBytes.length
                                + parameter.getMultipartBytes(isUtf8Charset).length;
            }
        }

        return parametersBytes;
    }

    private void writeRequestParameters(HttpConnection connection) throws IOException {
        if (!params.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : params.getRequestParameters()) {
                connection.writeBody(boundaryBytes);
                byte[] formItemBytes = parameter.getMultipartBytes(isUtf8Charset);
                connection.writeBody(formItemBytes);

                uploadedBodyBytes += boundaryBytes.length + formItemBytes.length;
                broadcastProgress(uploadedBodyBytes, totalBodyBytes);
            }
        }
    }

    private void writeFiles(HttpConnection connection) throws IOException {
        for (UploadFile file : params.getFiles()) {
            if (!shouldContinue)
                break;

            connection.writeBody(boundaryBytes);
            byte[] headerBytes = file.getMultipartHeader(isUtf8Charset);
            connection.writeBody(headerBytes);

            uploadedBodyBytes += boundaryBytes.length + headerBytes.length;
            broadcastProgress(uploadedBodyBytes, totalBodyBytes);

            final InputStream stream = file.getStream();
            writeStream(stream);
        }
    }

}
