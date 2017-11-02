package net.gotev.uploadservice;

import android.content.Intent;

import net.gotev.uploadservice.http.BodyWriter;

import java.io.IOException;
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
    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    // properties associated to each file
    protected static final String PROPERTY_REMOTE_FILE_NAME = "httpRemoteFileName";
    protected static final String PROPERTY_CONTENT_TYPE = "httpContentType";
    protected static final String PROPERTY_PARAM_NAME = "httpParamName";

    private byte[] boundaryBytes;
    private byte[] trailerBytes;
    private Charset charset;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);

        String boundary = BOUNDARY_SIGNATURE + System.nanoTime();
        boundaryBytes = (TWO_HYPHENS + boundary + NEW_LINE).getBytes(US_ASCII);
        trailerBytes = (TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).getBytes(US_ASCII);
        charset = intent.getBooleanExtra(PARAM_UTF8_CHARSET, false) ?
                Charset.forName("UTF-8") : US_ASCII;

        if (params.files.size() <= 1) {
            httpParams.addHeader("Connection", "close");
        } else {
            httpParams.addHeader("Connection", "Keep-Alive");
        }

        httpParams.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return (getRequestParametersLength() + getFilesLength() + trailerBytes.length);
    }

    @Override
    public void onBodyReady(BodyWriter bodyWriter) throws IOException {
        //reset uploaded bytes when the body is ready to be written
        //because sometimes this gets invoked when network changes
        uploadedBytes = 0;
        writeRequestParameters(bodyWriter);
        writeFiles(bodyWriter);
        bodyWriter.write(trailerBytes);
        uploadedBytes += trailerBytes.length;
        broadcastProgress(uploadedBytes, totalBytes);
    }

    private long getFilesLength() throws UnsupportedEncodingException {
        long total = 0;

        for (UploadFile file : params.files) {
            total += getTotalMultipartBytes(file);
        }

        return total;
    }

    private long getRequestParametersLength() throws UnsupportedEncodingException {
        long parametersBytes = 0;

        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter
                parametersBytes += boundaryBytes.length + getMultipartBytes(parameter).length;
            }
        }

        return parametersBytes;
    }

    private byte[] getMultipartBytes(NameValue parameter) throws UnsupportedEncodingException {
        return ("Content-Disposition: form-data; name=\"" + parameter.getName() + "\""
                + NEW_LINE + NEW_LINE + parameter.getValue() + NEW_LINE).getBytes(charset);
    }

    private byte[] getMultipartHeader(UploadFile file)
            throws UnsupportedEncodingException {
        String header = "Content-Disposition: form-data; name=\"" +
                file.getProperty(PROPERTY_PARAM_NAME) + "\"; filename=\"" +
                file.getProperty(PROPERTY_REMOTE_FILE_NAME) + "\"" + NEW_LINE +
                "Content-Type: " + file.getProperty(PROPERTY_CONTENT_TYPE) +
                NEW_LINE + NEW_LINE;

        return header.getBytes(charset);
    }

    private long getTotalMultipartBytes(UploadFile file)
            throws UnsupportedEncodingException {
        return boundaryBytes.length + getMultipartHeader(file).length + file.length(service)
                + NEW_LINE.getBytes(charset).length;
    }

    private void writeRequestParameters(BodyWriter bodyWriter) throws IOException {
        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                bodyWriter.write(boundaryBytes);
                byte[] formItemBytes = getMultipartBytes(parameter);
                bodyWriter.write(formItemBytes);

                uploadedBytes += boundaryBytes.length + formItemBytes.length;
                broadcastProgress(uploadedBytes, totalBytes);
            }
        }
    }

    private void writeFiles(BodyWriter bodyWriter) throws IOException {
        for (UploadFile file : params.files) {
            if (!shouldContinue)
                break;

            bodyWriter.write(boundaryBytes);
            byte[] headerBytes = getMultipartHeader(file);
            bodyWriter.write(headerBytes);

            uploadedBytes += boundaryBytes.length + headerBytes.length;
            broadcastProgress(uploadedBytes, totalBytes);

            bodyWriter.writeStream(file.getStream(service), this);

            byte[] newLineBytes = NEW_LINE.getBytes(charset);
            bodyWriter.write(newLineBytes);
            uploadedBytes += newLineBytes.length;
        }
    }

    @Override
    protected void onSuccessfulUpload() {
        addAllFilesToSuccessfullyUploadedFiles();
    }

}
