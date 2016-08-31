package net.gotev.uploadservicedemo;

import android.app.Application;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.UploadService;

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

        // Set upload service debug log messages level
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
    }
}
