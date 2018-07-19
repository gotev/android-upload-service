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

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final String PARAM_UTF8_CHARSET = "multipartUtf8Charset";

    private byte[] boundaryBytes;
    private Charset charset;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        httpParams.addHeader("Content-Type", "application/json");
        charset = intent.getBooleanExtra(PARAM_UTF8_CHARSET, false) ? Charset.forName("UTF-8") : US_ASCII;
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return getRequestParametersLength();
    }

    @Override
    public void onBodyReady(BodyWriter bodyWriter) throws IOException {
        writeRequestParameters(bodyWriter);
    }


    private long getRequestParametersLength() throws UnsupportedEncodingException {
        long parametersBytes = 0;
        int paramCount = 0;
        int totalParamCount = getRequestParametersCount();
        parametersBytes += "{".getBytes(charset).length;
        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                paramCount = paramCount + 1;
                boolean isLastPram = paramCount == totalParamCount;
                parametersBytes += getMultipartBytes(parameter, isLastPram).length;
            }
        }
        parametersBytes += "}".getBytes(charset).length;
        return parametersBytes;
    }

    private byte[] getMultipartBytes(NameValue parameter, boolean isLastPram) throws UnsupportedEncodingException {
        if (isLastPram) {
            return ("\"" + parameter.getName() + "\"" + ":" + "\"" + parameter.getValue() + "\"").getBytes(charset);
        }
        return ("\"" + parameter.getName() + "\"" + ":" + "\"" + parameter.getValue() + "\"" + ",").getBytes(charset);
    }

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

   

    private int getRequestParametersCount () throws UnsupportedEncodingException {
        int parameterCount = 0;
        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                parameterCount = parameterCount + 1;
            }
        }
        return parameterCount;
    }

    private byte[] joinParams(byte[] body, byte[] el) throws UnsupportedEncodingException {
        byte[] result = new byte[body.length + el.length];
        System.arraycopy(body, 0, result, 0, body.length);
        System.arraycopy(el, 0, result, body.length, el.length);
        return result;
    }

    private void writeRequestParameters(BodyWriter bodyWriter) throws IOException {
        byte[] jsonPrefix = "{".getBytes(charset);
        byte[] jsonSuffix = "}".getBytes(charset);
        int paramCount = 0;
        int totalParamCount = getRequestParametersCount();

        byte[] encodedJson = jsonPrefix;
        if (!httpParams.getRequestParameters().isEmpty()) {
            for (final NameValue parameter : httpParams.getRequestParameters()) {
                paramCount = paramCount + 1;
                byte[] jsonBytes;
                boolean isLastPram = paramCount == totalParamCount;
                jsonBytes = getMultipartBytes(parameter, isLastPram);
                encodedJson = joinParams(encodedJson, jsonBytes);
            }
            encodedJson = joinParams(encodedJson, jsonSuffix);
            bodyWriter.write(encodedJson);
        }
    }

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

    @Override
    protected void onSuccessfulUpload() {
        for (UploadFile file : params.files) {
            addSuccessfullyUploadedFile(file);
        }
        params.files.clear();
    }

}
