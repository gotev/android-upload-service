package net.gotev.uploadservice.http.impl;

import net.gotev.uploadservice.http.BodyWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Aleksandar Gotev
 */

public class HurlBodyWriter extends BodyWriter {

    private OutputStream mOutputStream;

    public HurlBodyWriter(OutputStream outputStream) {
        mOutputStream = outputStream;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        mOutputStream.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int lengthToWriteFromStart) throws IOException {
        mOutputStream.write(bytes, 0, lengthToWriteFromStart);
    }

    @Override
    public void flush() throws IOException {
        mOutputStream.flush();
    }
}
