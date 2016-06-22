package net.gotev.uploadservicedemo;

import android.app.Application;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.okhttp.OkHttpStack;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * @author gotev (Aleksandar Gotev)
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set your application namespace to avoid conflicts with other apps
        // using this library
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;

        // Set the HTTP stack to use. The default is HurlStack which uses HttpURLConnection.
        // To use OkHttp for example, you have to add the required dependency in your gradle file
        // and then you can simply un-comment the following line. Read the wiki for more info.
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(null)
                .build();
        UploadService.HTTP_STACK = new OkHttpStack(client);

        // Set upload service debug log messages level
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
    }
}
