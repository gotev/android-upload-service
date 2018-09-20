package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a request parameter.
 *
 * @author gotev (Aleksandar Gotev)
 *
 */
public final class NameValue implements Parcelable {

    private final String name;
    private final String value;

    public static NameValue header(final String name, final String value) {
        if (!isAllASCII(name) || !isAllASCII(value))
            throw new IllegalArgumentException("Header " + name + " must be ASCII only! Read http://stackoverflow.com/a/4410331");

        return new NameValue(name, value);
    }

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

    private static boolean isAllASCII(String input) {
        if (input == null || input.isEmpty())
            return false;

        boolean isASCII = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 127) {
                isASCII = false;
                break;
            }
        }
        return isASCII;
    }
}
