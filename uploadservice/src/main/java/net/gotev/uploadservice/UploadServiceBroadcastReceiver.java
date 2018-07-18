package net.gotev.uploadservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Broadcast receiver to subclass to create a receiver for {@link UploadService} events.
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
public class UploadServiceBroadcastReceiver extends BroadcastReceiver
        implements UploadStatusDelegate {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !UploadService.getActionBroadcast().equals(intent.getAction()))
            return;

        BroadcastData data = intent.getParcelableExtra(UploadService.PARAM_BROADCAST_DATA);

        if (data == null) {
            Logger.error(getClass().getSimpleName(), "Missing intent parameter: " + UploadService.PARAM_BROADCAST_DATA);
            return;
        }

        UploadInfo uploadInfo = data.getUploadInfo();

        if (!shouldAcceptEventFrom(uploadInfo)) {
            return;
        }

        switch (data.getStatus()) {
            case ERROR:
                onError(context, uploadInfo, data.getServerResponse(), data.getException());
                break;

            case COMPLETED:
                onCompleted(context, uploadInfo, data.getServerResponse());
                break;

            case IN_PROGRESS:
                onProgress(context, uploadInfo);
                break;

            case CANCELLED:
                onCancelled(context, uploadInfo);
                break;

            default:
                break;
        }
    }

    /**
     * Method called every time a new event arrives from an upload task, to decide whether or not
     * to process it. This is useful if you want to filter events based on custom business logic.
     *
     * @param uploadInfo upload info to
     * @return true to accept the event, false to discard it
     */
    protected boolean shouldAcceptEventFrom(UploadInfo uploadInfo) {
        return true;
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

    @Override
    public void onProgress(final Context context, final UploadInfo uploadInfo) {
    }

    @Override
    public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {

    }

    @Override
    public void onCompleted(final Context context, final UploadInfo uploadInfo, final ServerResponse serverResponse) {
    }

    @Override
    public void onCancelled(final Context context, final UploadInfo uploadInfo) {
    }
}
