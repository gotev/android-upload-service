package com.alexbbb.uploadservice;

import android.content.Intent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Task to upload a binary file.
 *
 * @author cankov
 * @author alexbbb
 */
class BinaryUploadTask extends HttpUploadTask {

    private BinaryUploadFile file;

    @Override
    protected void init(UploadService service, Intent intent) {
        super.init(service, intent);
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

    @Override
    protected void onSuccessfulUpload() {
        if (autoDeleteFilesAfterSuccessfulUpload) {
            deleteFile(this.getClass().getSimpleName(), this.file.file);
        }
    }
}
