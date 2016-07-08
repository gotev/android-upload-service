package net.gotev.uploadservice;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.util.Log;

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
    protected final UploadFileHandler handler;

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

        this.path = standardizePath(path);
        this.handler = getHandler();
    }

    private UploadFileHandler getHandler() throws FileNotFoundException {
        if (path.startsWith(ContentResolver.SCHEME_CONTENT)) {
            return new UriHandler(path);
        } else {
            return new FileHandler(path);
        }
    }

    /**
     * Gets the file length in bytes.
     * @return file length
     */
    public long length(Context context) {
        return handler.getLength(context);
    }

    /**
     * Gets the {@link InputStream} to read the content of this file.
     * @return file input stream
     * @throws FileNotFoundException if the file can't be found at the path specified in the
     * constructor
     */
    public final InputStream getStream(Context context) throws FileNotFoundException {
        return handler.getInputStream(context);
    }

    /**
     * Returns the content type for the file
     * @return content type
     */
    public final String getContentType(Context context) {
        return handler.getContentType(context);
    }

    /**
     * Returns the name of this file.
     * @return string
     */
    public final String getName(Context context) {
        return handler.getName(context);
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

        UploadFileHandler handler = null;
        try {
            handler = getHandler();
        } catch (FileNotFoundException e) {
            //This shouldn't happen, it would've been thrown in the main constructor
            Log.e(TAG, "File not found when constructing from parcel", e);
        }
        this.handler = handler;
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

    /**
     * Removes the prefix from file URIs so they can be used with
     * the java.io.File constructor
     * TODO: Check if this is actually necessary
     */
    private String standardizePath(String path) {
        if (path.startsWith("file://")) {
            return path.substring("file://".length());
        }
        return path;
    }

    /**
     * Allows for different file representations to be used by abstracting several characteristics
     * and operations
     */
    private interface UploadFileHandler {
        long getLength(Context context);
        InputStream getInputStream(Context context) throws FileNotFoundException;
        String getContentType(Context context);
        String getName(Context context);
    }

    /**
     * Handler for java.io.File
     */
    private static class FileHandler implements UploadFileHandler {

        final File file;

        FileHandler(String path) throws FileNotFoundException {
            this.file = new File(path);
        }

        @Override
        public long getLength(Context context) {
            return file.length();
        }

        @Override
        public InputStream getInputStream(Context context) throws FileNotFoundException {
            return new FileInputStream(file);
        }

        @Override
        public String getContentType(Context context) {
            return ContentType.autoDetect(file.getAbsolutePath());
        }

        @Override
        public String getName(Context context) {
            return file.getName();
        }
    }

    /**
     * Handles Android content uris, wraps android.content.Uri
     */
    private static class UriHandler implements UploadFileHandler {

        final Uri uri;

        UriHandler(String path) {
            this.uri = Uri.parse(path);
        }

        @Override
        public long getLength(Context context) {
            return getUriSize(context);
        }

        @Override
        public InputStream getInputStream(Context context) throws FileNotFoundException {
            return context.getContentResolver().openInputStream(uri);
        }

        @Override
        public String getContentType(Context context) {
            return context.getContentResolver().getType(uri);
        }

        @Override
        public String getName(Context context) {
            return getUriName(context);
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

}
