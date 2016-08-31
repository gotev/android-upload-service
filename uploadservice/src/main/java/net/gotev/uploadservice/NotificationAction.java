package net.gotev.uploadservice;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class which contains all the data for upload notification action
 *
 * @author hendrawd on 8/25/16
 */
public class NotificationAction implements Parcelable {
    private int resourceDrawable;
    private String text;
    private Intent clickIntent;

    public NotificationAction(int resourceDrawable, String text, Intent clickIntent) {
        this.resourceDrawable = resourceDrawable;
        this.text = text;
        this.clickIntent = clickIntent;
    }

    public int getResourceDrawable() {
        return resourceDrawable;
    }

    public String getText() {
        return text;
    }

    public final PendingIntent getPendingIntent(Context context) {
        if (clickIntent == null) {
            return PendingIntent.getBroadcast(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return PendingIntent.getActivity(context, 1, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.resourceDrawable);
        dest.writeString(this.text);
        dest.writeParcelable(this.clickIntent, flags);
    }

    protected NotificationAction(Parcel in) {
        this.resourceDrawable = in.readInt();
        this.text = in.readString();
        this.clickIntent = in.readParcelable(PendingIntent.class.getClassLoader());
    }

    public static final Parcelable.Creator<NotificationAction> CREATOR = new Parcelable.Creator<NotificationAction>() {
        @Override
        public NotificationAction createFromParcel(Parcel source) {
            return new NotificationAction(source);
        }

        @Override
        public NotificationAction[] newArray(int size) {
            return new NotificationAction[size];
        }
    };
}
