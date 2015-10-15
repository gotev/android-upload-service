package com.alexbbb.uploadservicedemo;

import android.app.Application;

import com.alexbbb.uploadservice.UploadService;

/**
 * @author alexbbb (Aleksandar Gotev)
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set your application namespace to avoid conflicts with other apps
        // using this library
        UploadService.NAMESPACE = "com.alexbbb";
    }
}
