package net.gotev.uploadservicedemo.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.gotev.uploadservice.UploadService;

public class NotificationActionsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !NotificationActions.INTENT_ACTION.equals(intent.getAction())) {
            return;
        }

        if (NotificationActions.ACTION_CANCEL_UPLOAD.equals(intent.getStringExtra(NotificationActions.PARAM_ACTION))) {
            onUserRequestedUploadCancellation(context, intent.getStringExtra(NotificationActions.PARAM_UPLOAD_ID));
        }

    }

    private void onUserRequestedUploadCancellation(Context context, String uploadId) {
        Log.e("CANCEL_UPLOAD", "User requested cancellation of upload with ID: " + uploadId);
        UploadService.stopUpload(uploadId);
    }
}
