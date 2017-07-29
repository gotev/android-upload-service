package net.gotev.uploadservice;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

/**
 * Class which represents a notification action.
 * It is necessary because NotificationCompat.Action is not serializable or Parcelable, thus it's
 * not possible to pass it directly in the intents.
 *
 * @author Aleksandar Gotev
 */

public class UploadNotificationAction implements Parcelable {

    private int icon;
    private CharSequence title;
    private PendingIntent actionIntent;

    /**
     * Creates a new object from an existing NotificationCompat.Action object.
     * @param action notification compat action
     * @return new instance
     */
    public static UploadNotificationAction from(NotificationCompat.Action action) {
        return new UploadNotificationAction(action.icon, action.title, action.actionIntent);
    }

    /**
     * Creates a new {@link UploadNotificationAction} object.
     * @param icon icon to show for this action
     * @param title the title of the action
     * @param intent the {@link PendingIntent} to fire when users trigger this action
     */
    public UploadNotificationAction(int icon, CharSequence title, PendingIntent intent) {
        this.icon = icon;
        this.title = title;
        this.actionIntent = intent;
    }

    final NotificationCompat.Action toAction() {
        return new NotificationCompat.Action.Builder(icon, title, actionIntent).build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.icon);

        TextUtils.writeToParcel(title, dest, flags);

        if (actionIntent != null) {
            dest.writeInt(1);
            actionIntent.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
    }

    protected UploadNotificationAction(Parcel in) {
        this.icon = in.readInt();

        this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        if (in.readInt() == 1) {
            actionIntent = PendingIntent.CREATOR.createFromParcel(in);
        }
    }

    public static final Parcelable.Creator<UploadNotificationAction> CREATOR = new Parcelable.Creator<UploadNotificationAction>() {
        @Override
        public UploadNotificationAction createFromParcel(Parcel source) {
            return new UploadNotificationAction(source);
        }

        @Override
        public UploadNotificationAction[] newArray(int size) {
            return new UploadNotificationAction[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadNotificationAction)) return false;

        UploadNotificationAction that = (UploadNotificationAction) o;

        if (icon != that.icon) return false;
        if (!title.equals(that.title)) return false;
        return actionIntent.equals(that.actionIntent);

    }

    @Override
    public int hashCode() {
        int result = icon;
        result = 31 * result + title.hashCode();
        result = 31 * result + actionIntent.hashCode();
        return result;
    }
}
