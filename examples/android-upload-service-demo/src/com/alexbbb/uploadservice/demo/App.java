package com.alexbbb.uploadservice.demo;

/**
 * Created by Divish on 1/15/2015.
 */
public class App {
    private static MainActivity mainActivity;
    public static void log(String message){
        try {
            mainActivity.log(message);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void setMainActivity(MainActivity mainActivity) {
        App.mainActivity = mainActivity;
    }
}
