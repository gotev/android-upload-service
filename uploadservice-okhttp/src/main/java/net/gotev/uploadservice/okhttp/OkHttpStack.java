package net.gotev.uploadservice.okhttp;

import net.gotev.uploadservice.http.HttpConnection;
import net.gotev.uploadservice.http.HttpStack;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Implementation of the OkHttp Stack.
 * @author Aleksandar Gotev
 */
public class OkHttpStack implements HttpStack {

    private OkHttpClient mClient;

    public OkHttpStack() {
        mClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(null)
                .build();
    }

    public OkHttpStack(OkHttpClient client) {
        mClient = client;
    }

    @Override
    public HttpConnection createNewConnection(String method, String url) throws IOException {
        return new OkHttpStackConnection(mClient, method, url);
    }
}
