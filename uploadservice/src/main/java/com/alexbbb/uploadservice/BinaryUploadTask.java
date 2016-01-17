package com.alexbbb.uploadservice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

/**
 * Task to upload a binary file.
 *
 * @author cankov
 * @author alexbbb (Aleksandar Gotev)
 */
public class BinaryUploadTask extends HttpUploadTask {

    @Override
    protected void setupHttpUrlConnection(HttpURLConnection connection) throws IOException {
        // nothing additional to setup for this request type
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return params.getFiles().get(0).length();
    }

    @Override
    protected void writeBody() throws IOException {
        writeStream(params.getFiles().get(0).getStream());
    }

}
