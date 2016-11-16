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

        switch (data.getStatus()) {
            case ERROR:
                onError(context, data.getUploadInfo(), data.getException());
                break;

            case COMPLETED:
                onCompleted(context, data.getUploadInfo(), data.getServerResponse());
                break;

            case IN_PROGRESS:
                onProgress(context, data.getUploadInfo());
                break;

            case CANCELLED:
                onCancelled(context, data.getUploadInfo());
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

    @Override
    public void onProgress(final Context context, final UploadInfo uploadInfo) {
    }

    @Override
    public void onError(final Context context, final UploadInfo uploadInfo, final Exception exception) {
    }

    @Override
    public void onCompleted(final Context context, final UploadInfo uploadInfo, final ServerResponse serverResponse) {
    }

    @Override
    public void onCancelled(final Context context, final UploadInfo uploadInfo) {
    }
}
