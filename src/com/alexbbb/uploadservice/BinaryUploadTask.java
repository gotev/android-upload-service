package com.alexbbb.uploadservice;

import android.content.Intent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by cankov on 9/3/15.
 */
class BinaryUploadTask extends HttpUploadTask {

    private final BinaryUploadFile file;

    BinaryUploadTask(UploadService service, Intent intent) {
        super(service, intent);
        this.file = intent.getParcelableExtra(UploadService.PARAM_FILE);
    }

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return file.length();
    }

    @Override
    protected void writeBody() throws IOException {
        writeStream(file.getStream());
    }
}
