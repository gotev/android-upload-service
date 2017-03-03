package net.gotev.uploadservicedemo.issues;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

/**
 * https://github.com/gotev/android-upload-service/issues/226
 * @author Aleksandar Gotev
 */

public class Issue226 implements Runnable {

    private Context ctx;

    public Issue226(Context context) {
        ctx = context;
    }

    @Override
    public void run() {
        Handler handler = new Handler();

        for (int a = 1; a<= 10; a++) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    multipleRequests(ctx);
                }
            }, 1000 * a);
        }
    }

    private void multipleRequests(Context ctx) {
        final String endpoint = "http://posttestserver.com/post.php";
        final int maxRetries = 2;

        final UploadNotificationConfig notificationConfig = new UploadNotificationConfig()
                .setAutoClearOnCancel(false)
                .setAutoClearOnSuccess(true);

        try {
            String fatherId = "father" + Long.toString(System.nanoTime());
            new MultipartUploadRequest(ctx, fatherId, endpoint)
                    .setMethod("POST")
                    .setNotificationConfig(notificationConfig.setTitle(fatherId))
                    .addParameter("color", "#ffffff")
                    .setMaxRetries(maxRetries)
                    .setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {

                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, Exception exception) {

                        }

                        @Override
                        public void onCompleted(final Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                            startChildRequest(context, endpoint, notificationConfig, maxRetries);
                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {

                        }
                    })
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "multipleRequests Error", exc);
        }
    }

    private void startChildRequest(final Context context,
                                   final String endpoint,
                                   final UploadNotificationConfig notificationConfig,
                                   final int maxRetries) {
        try {
            String childId = "child" + Long.toString(System.nanoTime());
            new MultipartUploadRequest(context, childId, endpoint)
                    .setMethod("POST")
                    .setNotificationConfig(notificationConfig.setTitle(childId))
                    .addParameter("color", "#ffffff")
                    .addParameter("test", "value")
                    .addParameter("new", "parameter")
                    .setMaxRetries(maxRetries)
                    .setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {

                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, Exception exception) {

                        }

                        @Override
                        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                            Log.i(uploadInfo.getUploadId(), "Completed");
                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {
                            Log.e(uploadInfo.getUploadId(), "Cancelled");
                        }
                    })
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "second request error", exc);
        }
    }
}
