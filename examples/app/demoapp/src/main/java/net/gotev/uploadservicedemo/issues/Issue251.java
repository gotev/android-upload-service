package net.gotev.uploadservicedemo.issues;

import android.content.Context;
import android.util.Log;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadStatusDelegate;

/**
 * https://github.com/gotev/android-upload-service/issues/251
 * @author Aleksandar Gotev
 */

public class Issue251 implements Runnable {

    private Context context;

    public Issue251(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        try {
            new MultipartUploadRequest(context, "http://posttestserver.com/post.php")
                    .setMethod("POST")
                    .setNotificationConfig(null)
                    .setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {
                            UploadService.stopUpload(uploadInfo.getUploadId());
                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, Exception exception) {

                        }

                        @Override
                        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {

                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {

                        }
                    })
                    .setAutoDeleteFilesAfterSuccessfulUpload(true)
                    .addParameter("color", "#ffffff")
                    .setMaxRetries(2)
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "Error", exc);
        }
    }

}
