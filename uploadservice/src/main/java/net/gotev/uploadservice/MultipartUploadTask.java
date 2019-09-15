package net.gotev.uploadservice;

import net.gotev.uploadservice.data.NameValue;
import net.gotev.uploadservice.data.UploadFile;
import net.gotev.uploadservice.network.BodyWriter;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Implements an HTTP Multipart upload task.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class MultipartUploadTask extends HttpUploadTask {

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
    private Charset charset = Charset.forName("UTF-8");

    @Override
    public void performInitialization() {

        String boundary = BOUNDARY_SIGNATURE + System.nanoTime();
        boundaryBytes = (TWO_HYPHENS + boundary + NEW_LINE).getBytes(US_ASCII);
        trailerBytes = (TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).getBytes(US_ASCII);

        HttpUploadTaskParameters httpParams = getHttpParams();

        if (params.getFiles().size() <= 1) {
            httpParams.addHeader("Connection", "close");
        } else {
            httpParams.addHeader("Connection", "Keep-Alive");
        }

        httpParams.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    @Override
    public long getBodyLength() {
        return (getRequestParametersLength() + getFilesLength() + trailerBytes.length);
    }

    @Override
    public void onWriteRequestBody(BodyWriter bodyWriter) throws IOException {
        //reset uploaded bytes when the body is ready to be written
        //because sometimes this gets invoked when network changes
        resetUploadedBytes();
        writeRequestParameters(bodyWriter);
        writeFiles(bodyWriter);
        bodyWriter.write(trailerBytes);
        broadcastProgress(trailerBytes.length);
    }

    private long getFilesLength() {
        long total = 0;

        for (UploadFile file : params.getFiles()) {
            total += getTotalMultipartBytes(file);
        }

        return total;
    }

    private long getRequestParametersLength() {
        long parametersBytes = 0;
        HttpUploadTaskParameters params = getHttpParams();

        if (!params.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : params.getRequestParameters()) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter
                parametersBytes += boundaryBytes.length + getMultipartBytes(parameter).length;
            }
        }

        return parametersBytes;
    }

    private byte[] getMultipartBytes(NameValue parameter) {
        return ("Content-Disposition: form-data; name=\"" + parameter.getName() + "\""
                + NEW_LINE + NEW_LINE + parameter.getValue() + NEW_LINE).getBytes(charset);
    }

    private byte[] getMultipartHeader(UploadFile file) {
        String header = "Content-Disposition: form-data; name=\"" +
                file.getProperties().get(PROPERTY_PARAM_NAME) + "\"; filename=\"" +
                file.getProperties().get(PROPERTY_REMOTE_FILE_NAME) + "\"" + NEW_LINE +
                "Content-Type: " + file.getProperties().get(PROPERTY_CONTENT_TYPE) +
                NEW_LINE + NEW_LINE;

        return header.getBytes(charset);
    }

    private long getTotalMultipartBytes(UploadFile file) {
        return boundaryBytes.length + getMultipartHeader(file).length + file.getHandler().size(context)
                + NEW_LINE.getBytes(charset).length;
    }

    private void writeRequestParameters(BodyWriter bodyWriter) throws IOException {
        HttpUploadTaskParameters params = getHttpParams();

        if (!params.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : params.getRequestParameters()) {
                bodyWriter.write(boundaryBytes);
                byte[] formItemBytes = getMultipartBytes(parameter);
                bodyWriter.write(formItemBytes);

                broadcastProgress(boundaryBytes.length + formItemBytes.length);
            }
        }
    }

    private void writeFiles(BodyWriter bodyWriter) throws IOException {
        for (UploadFile file : params.getFiles()) {
            if (!getShouldContinue())
                break;

            bodyWriter.write(boundaryBytes);
            byte[] headerBytes = getMultipartHeader(file);
            bodyWriter.write(headerBytes);

            broadcastProgress(boundaryBytes.length + headerBytes.length);

            bodyWriter.writeStream(file.getHandler().stream(context), this);

            byte[] newLineBytes = NEW_LINE.getBytes(charset);
            bodyWriter.write(newLineBytes);

            broadcastProgress(newLineBytes.length);
        }
    }

    @Override
    protected void onSuccessfulUpload() {
        addAllFilesToSuccessfullyUploadedFiles();
    }

}
