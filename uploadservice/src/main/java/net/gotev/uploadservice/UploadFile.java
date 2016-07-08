package net.gotev.uploadservice;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.OpenableColumns;

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

    private static final String TAG = UploadFile.class.getName();

    protected final String path;
    private LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    protected final File file;
    protected final Uri uri;
    protected final boolean isContentUri;

    /**
     * Creates a new UploadFile.
     *
     * @param path absolute path to a file or a content Uri string
     * @throws FileNotFoundException if the file can't be found at the specified path
     * @throws IllegalArgumentException if you passed invalid argument values
     */
    public UploadFile(String path) throws FileNotFoundException {

        if (path == null || "".equals(path)) {
            throw new IllegalArgumentException("Please specify a file path!");
        }

        this.path = sanitizePath(path);
        this.isContentUri = path.startsWith(ContentResolver.SCHEME_CONTENT);

        if (isContentUri) {
            this.uri = Uri.parse(path);
            this.file = null;
            return;
        }

        File file = new File(this.path);

        if (!file.exists())
            throw new FileNotFoundException("Could not find file at path: " + path);
        if (file.isDirectory())
            throw new FileNotFoundException("The specified path refers to a directory: " + path);

        this.file = file;
        this.uri = null;
    }

    /**
     * Gets the file length in bytes.
     * @return file length
     */
    public long length(Context context) {

        if (isContentUri) {
            return getUriSize(context);
        }

        return file.length();
    }

    /**
     * Gets the {@link InputStream} to read the content of this file.
     * @return file input stream
     * @throws FileNotFoundException if the file can't be found at the path specified in the
     * constructor
     */
    public final InputStream getStream(Context context) throws FileNotFoundException {

        if (isContentUri) {
            return context.getContentResolver().openInputStream(uri);
        }

        return new FileInputStream(file);
    }

    /**
     * Returns the content type for the file
     * @return content type
     */
    public final String getContentType(Context context) {

        if (isContentUri) {
            return context.getContentResolver().getType(uri);
        }

        return ContentType.autoDetect(file.getAbsolutePath());
    }

    /**
     * Returns the name of this file.
     * @return string
     */
    public final String getName(Context context) {

        if (isContentUri) {
            return getUriName(context);
        }

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
        parcel.writeString(path);
        parcel.writeSerializable(properties);
    }

    @SuppressWarnings("unchecked")
    private UploadFile(Parcel in) {
        this.path = in.readString();
        this.properties = (LinkedHashMap<String, String>) in.readSerializable();
        this.isContentUri = path.startsWith(ContentResolver.SCHEME_CONTENT);

        if (isContentUri) {
            this.uri = Uri.parse(path);
            this.file = null;
        } else {
            this.file = new File(path);
            this.uri = null;
        }
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

    private String sanitizePath(String path) {
        if (path.startsWith("file://")) {
            return path.substring("file://".length());
        }
        return path;
    }

    private long getUriSize(Context context) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        long size = cursor.getLong(sizeIndex);
        cursor.close();
        return size;
    }

    private String getUriName(Context context) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return getUriNameFallback();
        }
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String name = cursor.getString(nameIndex);
        cursor.close();
        return name;
    }

    private String getUriNameFallback() {
        String[] components = uri.toString().split(File.separator);
        return components[components.length - 1];
    }

}
