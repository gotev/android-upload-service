package net.gotev.uploadservice;

import android.content.Intent;

import net.gotev.uploadservice.http.BodyWriter;

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

        String boundary = BOUNDARY_SIGNATURE + System.currentTimeMillis();
        boundaryBytes = (NEW_LINE + TWO_HYPHENS + boundary + NEW_LINE).getBytes(US_ASCII);
        trailerBytes = (NEW_LINE + TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).getBytes(US_ASCII);
        charset = intent.getBooleanExtra(PARAM_UTF8_CHARSET, false) ?
                Charset.forName("UTF-8") : US_ASCII;

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
    public void onBodyReady(BodyWriter bodyWriter) throws IOException {
        writeRequestParameters(bodyWriter);
        writeFiles(bodyWriter);
        bodyWriter.write(trailerBytes);
    }

    private long getFilesLength() throws UnsupportedEncodingException {
        long total = 0;

        for (UploadFile file : params.getFiles()) {
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
                + NEW_LINE + NEW_LINE + parameter.getValue()).getBytes(charset);
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
        return boundaryBytes.length + getMultipartHeader(file).length + file.length(service);
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
        for (UploadFile file : params.getFiles()) {
            if (!shouldContinue)
                break;

            bodyWriter.write(boundaryBytes);
            byte[] headerBytes = getMultipartHeader(file);
            bodyWriter.write(headerBytes);

            uploadedBytes += boundaryBytes.length + headerBytes.length;
            broadcastProgress(uploadedBytes, totalBytes);

            final InputStream stream = file.getStream(service);
            bodyWriter.writeStream(stream, this);
            stream.close();
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
