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
public class HttpJsonTask extends HttpUploadTask {

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        httpParams.addRequestHeader("Content-Type", "application/json");
    }

    // @Override
    // protected long getBodyLength() throws UnsupportedEncodingException {
    //     return (getRequestParametersLength() + getFilesLength() + trailerBytes.length);
    // }

    // @Override
    // public void onBodyReady(BodyWriter bodyWriter) throws IOException {
    //     writeRequestParameters(bodyWriter);
    //     writeFiles(bodyWriter);
    //     bodyWriter.write(trailerBytes);
    // }

    // private long getFilesLength() throws UnsupportedEncodingException {
    //     long total = 0;

    //     for (UploadFile file : params.getFiles()) {
    //         total += getTotalMultipartBytes(file);
    //     }

    //     return total;
    // }

    // private long getRequestParametersLength() throws UnsupportedEncodingException {
    //     long parametersBytes = 0;

    //     if (!httpParams.getRequestParameters().isEmpty()) {
    //         for (final NameValue parameter : httpParams.getRequestParameters()) {
    //             // the bytes needed for every parameter are the sum of the boundary bytes
    //             // and the bytes occupied by the parameter
    //             parametersBytes += boundaryBytes.length + getMultipartBytes(parameter).length;
    //         }
    //     }

    //     return parametersBytes;
    // }

    // private byte[] getMultipartBytes(NameValue parameter) throws UnsupportedEncodingException {
    //     return ("Content-Disposition: form-data; name=\"" + parameter.getName() + "\""
    //             + NEW_LINE + NEW_LINE + parameter.getValue()).getBytes(charset);
    // }

    // private byte[] getMultipartHeader(UploadFile file)
    //         throws UnsupportedEncodingException {
    //     String header = "Content-Disposition: form-data; name=\"" +
    //             file.getProperty(PROPERTY_PARAM_NAME) + "\"; filename=\"" +
    //             file.getProperty(PROPERTY_REMOTE_FILE_NAME) + "\"" + NEW_LINE +
    //             "Content-Type: " + file.getProperty(PROPERTY_CONTENT_TYPE) +
    //             NEW_LINE + NEW_LINE;

    //     return header.getBytes(charset);
    // }

    // private long getTotalMultipartBytes(UploadFile file)
    //         throws UnsupportedEncodingException {
    //     return boundaryBytes.length + getMultipartHeader(file).length + file.length(service);
    // }

    // private void writeRequestParameters(BodyWriter bodyWriter) throws IOException {
    //     if (!httpParams.getRequestParameters().isEmpty()) {
    //         for (final NameValue parameter : httpParams.getRequestParameters()) {
    //             bodyWriter.write(boundaryBytes);
    //             byte[] formItemBytes = getMultipartBytes(parameter);
    //             bodyWriter.write(formItemBytes);

    //             uploadedBytes += boundaryBytes.length + formItemBytes.length;
    //             broadcastProgress(uploadedBytes, totalBytes);
    //         }
    //     }
    // }

    // private void writeFiles(BodyWriter bodyWriter) throws IOException {
    //     for (UploadFile file : params.getFiles()) {
    //         if (!shouldContinue)
    //             break;

    //         bodyWriter.write(boundaryBytes);
    //         byte[] headerBytes = getMultipartHeader(file);
    //         bodyWriter.write(headerBytes);

    //         uploadedBytes += boundaryBytes.length + headerBytes.length;
    //         broadcastProgress(uploadedBytes, totalBytes);

    //         final InputStream stream = file.getStream(service);
    //         bodyWriter.writeStream(stream, this);
    //         stream.close();
    //     }
    // }

    // @Override
    // protected void onSuccessfulUpload() {
    //     for (UploadFile file : params.getFiles()) {
    //         addSuccessfullyUploadedFile(file.getPath());
    //     }
    //     params.getFiles().clear();
    // }

}
