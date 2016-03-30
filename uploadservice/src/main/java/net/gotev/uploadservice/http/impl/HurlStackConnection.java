package net.gotev.uploadservice.http.impl;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.NameValue;
import net.gotev.uploadservice.http.HttpConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * {@link HttpConnection} implementation using {@link HttpURLConnection}.
 * @author gotev (Aleksandar Gotev)
 */
public class HurlStackConnection implements HttpConnection {

    private static final String LOG_TAG = HurlStackConnection.class.getSimpleName();

    private static final int BUFFER_SIZE = 4096;

    private HttpURLConnection mConnection;

    public HurlStackConnection(String method, String url, boolean followRedirects,
                               boolean useCaches, int connectTimeout, int readTimeout)
            throws IOException {
        Logger.debug(getClass().getSimpleName(), "creating new connection");

        mConnection = (HttpURLConnection) new URL(url).openConnection();

        mConnection.setDoInput(true);
        mConnection.setDoOutput(true);
        mConnection.setConnectTimeout(connectTimeout);
        mConnection.setReadTimeout(readTimeout);
        mConnection.setUseCaches(useCaches);
        mConnection.setInstanceFollowRedirects(followRedirects);
        mConnection.setRequestMethod(method);
    }

    @Override
    public void setHeaders(List<NameValue> requestHeaders, boolean isFixedLengthStreamingMode,
                           long totalBodyBytes) throws IOException {

        if (isFixedLengthStreamingMode) {
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                mConnection.setFixedLengthStreamingMode(totalBodyBytes);

            } else {
                if (totalBodyBytes > Integer.MAX_VALUE)
                    throw new RuntimeException("You need Android API version 19 or newer to "
                            + "upload more than 2GB in a single request using "
                            + "fixed size content length. Try switching to "
                            + "chunked mode instead, but make sure your server side supports it!");

                mConnection.setFixedLengthStreamingMode((int) totalBodyBytes);
            }
        } else {
            mConnection.setChunkedStreamingMode(0);
        }

        for (final NameValue param : requestHeaders) {
            mConnection.setRequestProperty(param.getName(), param.getValue());
        }
    }

    @Override
    public void writeBody(byte[] bytes) throws IOException {
        mConnection.getOutputStream().write(bytes, 0, bytes.length);
    }

    @Override
    public void writeBody(byte[] bytes, int lengthToWriteFromStart) throws IOException {
        mConnection.getOutputStream().write(bytes, 0, lengthToWriteFromStart);
    }

    @Override
    public int getServerResponseCode() throws IOException {
        return mConnection.getResponseCode();
    }

    @Override
    public byte[] getServerResponseBody() throws IOException {
        InputStream stream = null;

        try {
            if (mConnection.getResponseCode() / 100 == 2) {
                stream = mConnection.getInputStream();
            } else {
                stream = mConnection.getErrorStream();
            }

            return getResponseBodyAsByteArray(stream);

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception exc) {
                    Logger.error(LOG_TAG, "Error while closing server response stream", exc);
                }
            }
        }
    }

    private byte[] getResponseBodyAsByteArray(final InputStream inputStream) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) > 0) {
                byteStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception ignored) {}

        return byteStream.toByteArray();
    }

    @Override
    public void close() {
        Logger.debug(getClass().getSimpleName(), "closing connection");

        if (mConnection != null) {
            try {
                mConnection.getInputStream().close();
            } catch (Exception ignored) { }

            try {
                mConnection.getOutputStream().flush();
                mConnection.getOutputStream().close();
            } catch (Exception ignored) { }

            try {
                mConnection.disconnect();
            } catch (Exception exc) {
                Logger.error(LOG_TAG, "Error while closing connection", exc);
            }
        }
    }
}
