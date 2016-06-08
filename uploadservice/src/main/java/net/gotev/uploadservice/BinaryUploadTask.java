package net.gotev.uploadservice;

import android.content.Context;

import net.gotev.uploadservice.http.HttpConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Implements a binary file upload task.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class BinaryUploadTask extends HttpUploadTask {

    public BinaryUploadTask(Context context) {
        super(context);
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return params.getFiles().get(0).length(context);
    }

    @Override
    protected void writeBody(HttpConnection connection) throws IOException {
        writeStream(params.getFiles().get(0).getStream(context));
    }

    @Override
    protected void onSuccessfulUpload() {
        addSuccessfullyUploadedFile(params.getFiles().get(0).getName(context));
    }
}
