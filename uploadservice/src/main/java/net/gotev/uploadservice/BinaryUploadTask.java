package net.gotev.uploadservice;

import net.gotev.uploadservice.http.BodyWriter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Implements a binary file upload task.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class BinaryUploadTask extends HttpUploadTask {

    @Override
    protected long getBodyLength() throws UnsupportedEncodingException {
        return params.getFiles().get(0).length(service);
    }

    @Override
    public void onBodyReady(BodyWriter bodyWriter) throws IOException {
        if (params.isChunkUpload()) {
            bodyWriter.writeStream(params.getFiles().get(0).getCurrentChunk().getStream(service), this);
        } else {
            bodyWriter.writeStream(params.getFiles().get(0).getStream(service), this);
        }
    }

    @Override
    protected void onSuccessfulUpload() {
        addSuccessfullyUploadedFile(params.getFiles().get(0).getPath());
        params.getFiles().clear();
    }

    @Override
    protected void onSuccessfulChunkUpload() {
        addSuccessfullyUploadedChunk(params.getFiles().get(0).getCurrentChunk().getPath());
    }

    @Override
    protected void upload() throws Exception {
        super.upload();
    }
}
