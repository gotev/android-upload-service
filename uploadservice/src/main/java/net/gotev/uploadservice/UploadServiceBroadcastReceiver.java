package net.gotev.uploadservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Broadcast receiver to sublass to create a receiver for
 * {@link UploadService} events.
 *
 * It provides the boilerplate code to properly handle broadcast messages coming from the
 * upload service and dispatch them to the proper handler method.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 * @author mabdurrahman
 *
 */
public class UploadServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !UploadService.getActionBroadcast().equals(intent.getAction()))
            return;

        BroadcastData data = intent.getParcelableExtra(UploadService.PARAM_BROADCAST_DATA);

        switch (data.getStatus()) {
            case ERROR:
                onError(data.getId(), data.getException());
                break;

            case COMPLETED:
                onCompleted(data.getId(), data.getResponseCode(), data.getResponseBody());
                break;

            case IN_PROGRESS:
                onProgress(data.getId(), data.getProgressPercent());
                onProgress(data.getId(), data.getUploadedBytes(), data.getTotalBytes());
                break;

            case CANCELLED:
                onCancelled(data.getId());
                break;

            default:
                break;
        }
    }

    /**
     * Register this upload receiver.<br>
     * If you use this receiver in an {@link android.app.Activity}, you have to call this method inside
     * {@link android.app.Activity#onResume()}, after {@code super.onResume();}.<br>
     * If you use it in a {@link android.app.Service}, you have to
     * call this method inside {@link android.app.Service#onCreate()}, after {@code super.onCreate();}.
     *
     * @param context context in which to register this receiver
     */
    public void register(final Context context) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UploadService.getActionBroadcast());
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregister this upload receiver.<br>
     * If you use this receiver in an {@link android.app.Activity}, you have to call this method inside
     * {@link android.app.Activity#onPause()}, after {@code super.onPause();}.<br>
     * If you use it in a {@link android.app.Service}, you have to
     * call this method inside {@link android.app.Service#onDestroy()}.
     *
     * @param context context in which to unregister this receiver
     */
    public void unregister(final Context context) {
        context.unregisterReceiver(this);
    }

    /**
     * Called when the upload progress changes. Override this method to add your own logic.
     *
     * @param uploadId unique ID of the upload request
     * @param progress value from 0 to 100
     */
    public void onProgress(final String uploadId, final int progress) {
    }

    /**
     * Called when the upload progress changes. Override this method to add your own logic.
     *
     * @param uploadId unique ID of the upload request
     * @param uploadedBytes the count of the bytes uploaded so far
     * @param totalBytes the total expected bytes to upload
     */
    public void onProgress(final String uploadId, final long uploadedBytes, final long totalBytes) {
    }

    /**
     * Called when an error happens during the upload. Override this method to add your own logic.
     *
     * @param uploadId unique ID of the upload request
     * @param exception exception that caused the error
     */
    public void onError(final String uploadId, final Exception exception) {
    }

    /**
     * Called when the upload is completed successfully. Override this method to add your own logic.
     *
     * @param uploadId unique ID of the upload request
     * @param serverResponseCode status code returned by the server
     * @param serverResponseBody byte array containing the response body received from the server.
     *                           If your server responds with a string, you can get it with
     *                           {@code new String(serverResponseBody)}. If the string is a
     *                           JSON, you can parse it using a library such as org.json
     *                           (embedded in Android) or google's gson
     */
    public void onCompleted(final String uploadId, final int serverResponseCode,
                            final byte[] serverResponseBody) {
    }

    /**
     * Called when the upload is cancelled. Override this method to add your own logic.
     *
     * @param uploadId unique ID of the upload request
     */
    public void onCancelled(final String uploadId) {
    }
}
