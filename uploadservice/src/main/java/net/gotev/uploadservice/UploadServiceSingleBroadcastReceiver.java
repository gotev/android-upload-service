package net.gotev.uploadservice;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Utility broadcast receiver to receive only the events for a single uploadID.
 * @author Aleksandar Gotev
 */

public class UploadServiceSingleBroadcastReceiver extends UploadServiceBroadcastReceiver {

    private WeakReference<UploadStatusDelegate> mDelegate;
    private String mUploadID = null;

    public UploadServiceSingleBroadcastReceiver(UploadStatusDelegate delegate) {
        mDelegate = new WeakReference<>(delegate);
    }

    public void setUploadID(String uploadID) {
        mUploadID = uploadID;
    }

    @Override
    protected boolean shouldAcceptEventFrom(UploadInfo uploadInfo) {
        return mUploadID != null && uploadInfo.getUploadId().equals(mUploadID);
    }

    @Override
    public final void onProgress(Context context, UploadInfo uploadInfo) {
        if (mDelegate != null && mDelegate.get() != null) {
            mDelegate.get().onProgress(context, uploadInfo);
        }
    }

    @Override
    public final void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
        if (mDelegate != null && mDelegate.get() != null) {
            mDelegate.get().onError(context, uploadInfo, serverResponse, exception);
        }
    }

    @Override
    public final void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
        if (mDelegate != null && mDelegate.get() != null) {
            mDelegate.get().onCompleted(context, uploadInfo, serverResponse);
        }
    }

    @Override
    public final void onCancelled(Context context, UploadInfo uploadInfo) {
        if (mDelegate != null && mDelegate.get() != null) {
            mDelegate.get().onCancelled(context, uploadInfo);
        }
    }
}
