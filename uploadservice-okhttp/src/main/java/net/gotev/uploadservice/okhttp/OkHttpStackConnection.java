package net.gotev.uploadservice.okhttp;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.NameValue;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.http.HttpConnection;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;
import okio.BufferedSink;

/**
 * {@link HttpConnection} implementation using OkHttpClient.
 * @author Aleksandar Gotev
 */
public class OkHttpStackConnection implements HttpConnection {

    private static final String LOG_TAG = OkHttpStackConnection.class.getSimpleName();

    private OkHttpClient mClient;
    private Request.Builder mRequestBuilder;
    private String mMethod;
    private long mBodyLength;
    private String mContentType;
    private Response mResponse;

    public OkHttpStackConnection(OkHttpClient client, String method, String url) throws IOException {
        Logger.debug(getClass().getSimpleName(), "creating new connection");

        mResponse = null;
        mClient = client;
        mMethod = method;

        mRequestBuilder = new Request.Builder().url(new URL(url));
    }

    @Override
    public HttpConnection setHeaders(List<NameValue> requestHeaders) throws IOException {
        for (final NameValue param : requestHeaders) {
            if ("Content-Type".equalsIgnoreCase(param.getName()))
                mContentType = param.getValue();

            mRequestBuilder.header(param.getName(), param.getValue());
        }

        return this;
    }

    @Override
    public HttpConnection setTotalBodyBytes(long totalBodyBytes, boolean isFixedLengthStreamingMode) {
        if (isFixedLengthStreamingMode) {
            if (android.os.Build.VERSION.SDK_INT < 19 && totalBodyBytes > Integer.MAX_VALUE)
                throw new RuntimeException("You need Android API version 19 or newer to "
                        + "upload more than 2GB in a single request using "
                        + "fixed size content length. Try switching to "
                        + "chunked mode instead, but make sure your server side supports it!");

            mBodyLength = totalBodyBytes;

        } else {
            // http://stackoverflow.com/questions/33921894/how-do-i-enable-disable-chunked-transfer-encoding-for-a-multi-part-post-that-inc#comment55679982_33921894
            mBodyLength = -1;
        }

        return this;
    }

    private LinkedHashMap<String, String> getServerResponseHeaders(Headers headers) throws IOException {
        LinkedHashMap<String, String> out = new LinkedHashMap<>(headers.size());

        for (String headerName : headers.names()) {
            out.put(headerName, headers.get(headerName));
        }

        return out;
    }

    @Override
    public ServerResponse getResponse(final RequestBodyDelegate delegate) throws IOException {
        if (HttpMethod.permitsRequestBody(mMethod) || HttpMethod.requiresRequestBody(mMethod)) {
            RequestBody body = new RequestBody() {
                @Override
                public long contentLength() throws IOException {
                    return mBodyLength;
                }

                @Override
                public MediaType contentType() {
                    if (mContentType == null)
                        return null;
                    return MediaType.parse(mContentType);
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    final OkHttpBodyWriter bodyWriter = new OkHttpBodyWriter(sink);
                    delegate.onBodyReady(bodyWriter);
                    bodyWriter.flush();
                }
            };

            mRequestBuilder.method(mMethod, body);
        } else {
            mRequestBuilder.method(mMethod, null);
        }

        mResponse = mClient.newCall(mRequestBuilder.build()).execute();

        return new ServerResponse(mResponse.code(),
                mResponse.body().bytes(),
                getServerResponseHeaders(mResponse.headers()));
    }

    @Override
    public void close() {
        Logger.debug(getClass().getSimpleName(), "closing connection");

        if (mResponse != null) {
            try {
                mResponse.close();
            } catch (Throwable exc) {
                Logger.error(LOG_TAG, "Error while closing connection", exc);
            }
        }
    }
}
