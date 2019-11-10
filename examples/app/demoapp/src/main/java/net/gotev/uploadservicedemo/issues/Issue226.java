package net.gotev.uploadservicedemo.issues;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest;
import net.gotev.uploadservice.data.UploadNotificationConfig;
import net.gotev.uploadservicedemo.App;

/**
 * https://github.com/gotev/android-upload-service/issues/226
 *
 * For the issue to be resolved, there shouldn't be any cancelled upload notifications.
 *
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

        final int total = 10;
        for (int a = 1; a<= total; a++) {
            final int requestNumber = a;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    multipleRequests(ctx, requestNumber, total);
                }
            }, 1000 * a);
        }
    }

    private void multipleRequests(Context ctx, int requestNumber, int totalRequests) {
        final String endpoint = "http://posttestserver.com/post.php";
        final int maxRetries = 2;

        final UploadNotificationConfig notificationConfig = new UploadNotificationConfig(App.CHANNEL);

        notificationConfig.getCancelled().setAutoClear(false);
        notificationConfig.getSuccess().setAutoClear(true);

        try {
            final String fatherId = "father " + requestNumber + "/" + totalRequests + "-" + System.nanoTime();
            new MultipartUploadRequest(ctx, endpoint)
                    .setUploadID(fatherId)
                    .setMethod("POST")
                    .setNotificationConfig(uploadId -> notificationConfig.setTitleForAllStatuses(fatherId))
                    .addParameter("color", "#ffffff")
                    .setMaxRetries(maxRetries)
                    /*.setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {

                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Throwable exception) {

                        }

                        @Override
                        public void onCompleted(final Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                            startChildRequest(context, fatherId, endpoint, notificationConfig, maxRetries);
                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {

                        }
                    })*/
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "multipleRequests Error", exc);
        }
    }

    private void startChildRequest(final Context context,
                                   final String fatherId,
                                   final String endpoint,
                                   final UploadNotificationConfig notificationConfig,
                                   final int maxRetries) {
        try {
            String childId = fatherId + " -> child" + System.nanoTime();
            new MultipartUploadRequest(context, endpoint)
                    .setUploadID(childId)
                    .setMethod("POST")
                    .setNotificationConfig(uploadId -> notificationConfig.setTitleForAllStatuses(childId))
                    .addParameter("color", "#ffffff")
                    .addParameter("test", "value")
                    .addParameter("new", "parameter")
                    .setMaxRetries(maxRetries)
                    /*.setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {

                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Throwable exception) {

                        }

                        @Override
                        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                            Log.i(uploadInfo.getUploadId(), "Completed");
                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {
                            Log.e(uploadInfo.getUploadId(), "Cancelled");
                        }
                    })*/
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "second request error", exc);
        }
    }
}
