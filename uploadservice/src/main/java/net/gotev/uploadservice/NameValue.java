package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Represents a request parameter.
 *
 * @author gotev (Aleksandar Gotev)
 *
 */
public final class NameValue implements Parcelable {

    private static final String NEW_LINE = "\r\n";

    private final String name;
    private final String value;

    private final Charset US_ASCII = Charset.forName("US-ASCII");
    private final Charset UTF8 = Charset.forName("UTF-8");

    public NameValue(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public final String getName() {
        return name;
    }

    public final String getValue() {
        return value;
    }

    public byte[] getMultipartBytes(boolean isUtf8) throws UnsupportedEncodingException {
        return ("Content-Disposition: form-data; name=\"" + name + "\""
                + NEW_LINE + NEW_LINE + value).getBytes(isUtf8 ? UTF8 : US_ASCII);
    }

    @Override
    public boolean equals(Object object) {
        final boolean areEqual;

        if (object instanceof NameValue) {
            final NameValue other = (NameValue) object;
            areEqual = this.name.equals(other.name) && this.value.equals(other.value);
        } else {
            areEqual = false;
        }

        return areEqual;
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<NameValue> CREATOR =
            new Parcelable.Creator<NameValue>() {
                @Override
                public NameValue createFromParcel(final Parcel in) {
                    return new NameValue(in);
                }

                @Override
                public NameValue[] newArray(final int size) {
                    return new NameValue[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(name);
        parcel.writeString(value);
    }

    private NameValue(Parcel in) {
        name = in.readString();
        value = in.readString();
    }
}
