package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedHashMap;

/**
 * Represents a file to upload.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class UploadFile implements Parcelable {

    protected final File file;
    private LinkedHashMap<String, String> properties = new LinkedHashMap<>();

    /**
     * Creates a new UploadFile.
     *
     * @param path absolute path to the file
     * @throws FileNotFoundException if the file can't be found at the specified path
     * @throws IllegalArgumentException if you passed invalid argument values
     */
    public UploadFile(final String path) throws FileNotFoundException {

        if (path == null || "".equals(path)) {
            throw new IllegalArgumentException("Please specify a file path!");
        }

        File file = new File(path);

        if (!file.exists())
            throw new FileNotFoundException("Could not find file at path: " + path);
        if (file.isDirectory())
            throw new FileNotFoundException("The specified path refers to a directory: " + path);

        this.file = file;

    }

    /**
     * Gets the file length in bytes.
     * @return file length
     */
    public long length() {
        return file.length();
    }

    /**
     * Gets the {@link InputStream} to read the content of this file.
     * @return file input stream
     * @throws FileNotFoundException if the file can't be found at the path specified in the
     * constructor
     */
    public final InputStream getStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    /**
     * Returns the absolute path to the file.
     * @return absolute file path
     */
    public final String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    /**
     * Returns the name of this file.
     * @return string
     */
    public final String getName() {
        return file.getName();
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<UploadFile> CREATOR =
            new Parcelable.Creator<UploadFile>() {
        @Override
        public UploadFile createFromParcel(final Parcel in) {
            return new UploadFile(in);
        }

        @Override
        public UploadFile[] newArray(final int size) {
            return new UploadFile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(file.getAbsolutePath());
        parcel.writeSerializable(properties);
    }

    @SuppressWarnings("unchecked")
    private UploadFile(Parcel in) {
        file = new File(in.readString());
        properties = (LinkedHashMap<String, String>) in.readSerializable();
    }

    /**
     * Sets a property for this file.
     * If you want to store objects, serialize them in JSON strings.
     * @param key property key
     * @param value property value
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Gets a property associated to this file.
     * @param key property key
     * @return property value or null if the value does not exist.
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Gets a property associated to this file.
     * @param key property key
     * @param defaultValue default value to use if the key does not exist or the value is null
     * @return property value or the default value passed
     */
    public String getProperty(String key, String defaultValue) {
        String val = properties.get(key);

        if (val == null) {
            val = defaultValue;
        }

        return val;
    }
}
