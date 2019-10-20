package net.gotev.uploadservicedemo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import net.gotev.uploadservice.UploadServiceConfig;
import net.gotev.uploadservice.data.RetryPolicyConfig;
import net.gotev.uploadservice.logger.UploadServiceLogger;
import net.gotev.uploadservice.observer.request.RequestObserver;
import net.gotev.uploadservice.okhttp.OkHttpStack;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import static com.facebook.stetho.Stetho.newInitializerBuilder;

/**
 * @author gotev (Aleksandar Gotev)
 */
public class App extends Application {

    public static String CHANNEL = "UploadServiceDemoChannel";

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Stetho.initialize(
                    newInitializerBuilder(this)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                            .build());

            //enableStrictMode();
        }

        // Set your application namespace to avoid conflicts with other apps
        // using this library
        UploadServiceConfig.setNamespace(BuildConfig.APPLICATION_ID);

        // Set up the Http Stack to use. If you omit this or comment it, HurlStack will be
        // used by default
        UploadServiceConfig.setHttpStack(new OkHttpStack(getOkHttpClient()));

        // setup backoff multiplier
        UploadServiceConfig.setRetryPolicy(new RetryPolicyConfig(1, 10, 2, 3));

        // Set upload service debug log messages level
        UploadServiceLogger.setDevelopmentMode(BuildConfig.DEBUG);

        createNotificationChannel();

        new RequestObserver(this, new GlobalBroadcastReceiver()).register();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Upload Service Demo", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)

                // you can add your own request interceptors to add authorization headers.
                // do not modify the body or the http method here, as they are set and managed
                // internally by Upload Service, and tinkering with them will result in strange,
                // erroneous and unpredicted behaviors
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request.Builder request = chain.request().newBuilder()
                                .addHeader("myheader", "myvalue")
                                .addHeader("mysecondheader", "mysecondvalue");

                        return chain.proceed(request.build());
                    }
                })

                // open up your Chrome and go to: chrome://inspect
                .addNetworkInterceptor(new StethoInterceptor())

                // if you use HttpLoggingInterceptor, be sure to put it always as the last interceptor
                // in the chain and to not use BODY level logging, otherwise you will get all your
                // file contents in the log. Logging body is suitable only for small requests.
                .addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Log.d("OkHttp", message);
                    }
                }).setLevel(HttpLoggingInterceptor.Level.HEADERS))

                .cache(null)
                .build();
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDialog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                .penaltyLog()
                .build());
    }
}
