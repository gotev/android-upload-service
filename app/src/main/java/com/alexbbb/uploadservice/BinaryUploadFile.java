package com.alexbbb.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Binary file to upload.
 *
 * @author cankov
 */
class BinaryUploadFile implements Parcelable {

    protected final File file;

    BinaryUploadFile(String path) {
        this.file = new File(path);
    }

    public long length() {
        return file.length();
    }

    public final InputStream getStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(file.getAbsolutePath());
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<BinaryUploadFile> CREATOR =
            new Parcelable.Creator<BinaryUploadFile>() {
        @Override
        public BinaryUploadFile createFromParcel(final Parcel in) {
            return new BinaryUploadFile(in);
        }

        @Override
        public BinaryUploadFile[] newArray(final int size) {
            return new BinaryUploadFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    protected BinaryUploadFile(Parcel in) {
        file = new File(in.readString());
    }
}
