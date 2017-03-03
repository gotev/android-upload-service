package net.gotev.uploadservicedemo.issues;

import android.content.Context;
import android.util.Log;

import net.gotev.uploadservice.MultipartUploadRequest;

/**
 * https://github.com/gotev/android-upload-service/issues/245
 * @author Aleksandar Gotev
 */

public class Issue245 implements Runnable {

    private Context context;

    public Issue245(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            new MultipartUploadRequest(context, "http://posttestserver.com/post.php")
                    .setMethod("POST")
                    .setNotificationConfig(null)
                    .setAutoDeleteFilesAfterSuccessfulUpload(true)
                    .addParameter("color", "#ffffff")
                    .setMaxRetries(2)
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "Error", exc);
        }
    }

}
