package com.alexbbb.uploadservice.demo;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Divish on 1/15/2015.
 */
public class Utils{
    public static ArrayList<String> copyAssets(Context context) {
        ArrayList<String> paths = new ArrayList<String>();
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for(String filename : files) {
            if (filename.endsWith(".jpg")) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open(filename);
                    File outFile = new File(context.getExternalFilesDir(null), filename);

                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    paths.add(outFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    //  return paths;
                }
            }
        }
        return  paths;
    }



    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
