package net.gotev.uploadservice;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import net.gotev.uploadservice.schemehandlers.SchemeHandler;
import net.gotev.uploadservice.schemehandlers.SchemeHandlerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;

/**
 * Represents a file to upload.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class UploadFile implements Parcelable {

    protected final String path;
    private LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    protected final SchemeHandler handler;
    private long startByte = 0;
    private long endByte;
    private UploadFile currentChunk;

    /**
     * Creates a new UploadFile.
     *
     * @param path absolute path to a file or an Android content Uri string
     * @throws FileNotFoundException if the file can't be found at the specified path
     * @throws IllegalArgumentException if you passed invalid argument values
     */
    public UploadFile(String path) throws FileNotFoundException {

        if (path == null || "".equals(path)) {
            throw new IllegalArgumentException("Please specify a file path!");
        }

        if (!SchemeHandlerFactory.getInstance().isSupported(path))
            throw new UnsupportedOperationException("Unsupported scheme: " + path);

        this.path = path;

        try {
            this.handler = SchemeHandlerFactory.getInstance().get(path);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * Gets the file length in bytes.
     * @param context service context
     * @return file length
     */
    public long length(Context context) {
        return handler.getLength(context);
    }

    /**
     * Gets the {@link InputStream} to read the content of this file.
     * @param context service context
     * @return file input stream
     * @throws FileNotFoundException if the file can't be found at the path specified in the
     * constructor
     */
    public final InputStream getStream(Context context) throws FileNotFoundException {
        return handler.getInputStream(context);
    }

    /**
     * Returns the content type for the file
     * @param context service context
     * @return content type
     */
    public final String getContentType(Context context) {
        return handler.getContentType(context);
    }

    /**
     * Returns the name of this file.
     * @param context service context
     * @return string
     */
    public final String getName(Context context) {
        return handler.getName(context);
    }

    /**
     * Returns the string this was initialized with,
     * either an absolute file path or Android content URI
     * @return String
     */
    public final String getPath() {
        return this.path;
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
        parcel.writeLong(startByte);
        parcel.writeLong(endByte);
        parcel.writeParcelable(currentChunk, arg1);
    }

    @SuppressWarnings("unchecked")
    private UploadFile(Parcel in) {
        this.path = in.readString();
        this.properties = (LinkedHashMap<String, String>) in.readSerializable();
        this.startByte = in.readLong();
        this.endByte = in.readLong();
        this.currentChunk = in.readParcelable(UploadFile.class.getClassLoader());

        try {
            this.handler = SchemeHandlerFactory.getInstance().get(path);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
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

    /**
     * if this file is a chunk file, this will return the start byte of this chunk from the parent file
     *
     * @return the number of the start byte or 0 if it's first chunk or main file
     */
    public long getStartByte() {
        return startByte;
    }

    /**
     * sets this chunk's start byte in the parent file
     *
     * @param startByte
     */
    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    /**
     * if this file is a chunk file, this will return the end byte of this chunk from the parent file
     *
     * @return the number of the start byte or null if this is the main file
     */
    public long getEndByte() {
        return endByte;
    }

    /**
     * sets this chunk's end byte in the parent file
     *
     * @param endByte
     */
    public void setEndByte(long endByte) {
        this.endByte = endByte;
    }

    /**
     * generates a chunk file based on the startByte parameter and the UploadService.CHUNK_SIZE limit
     * the chunk can be obtained with getCurrentChunk()
     *
     */
    public void generateChunk() {

        try {
            File currentFile = new File(getPath());

            RandomAccessFile randomAccessFile = new RandomAccessFile(currentFile, "r");
            randomAccessFile.seek(startByte);

            int dataSize = UploadService.CHUNK_SIZE;
            if (currentFile.length() - startByte < dataSize) {
                dataSize = (int) (currentFile.length() - startByte);
            }

            byte[] data = new byte[dataSize];
            randomAccessFile.read(data, 0, data.length);

            String partFilename = getPath() + ".part";
            File partFile = new File(partFilename);

            OutputStream outfile = new BufferedOutputStream(new FileOutputStream(partFile));
            outfile.write(data);
            outfile.close();

            UploadFile chunk = new UploadFile(partFilename);
            chunk.setStartByte(startByte);
            chunk.setEndByte(startByte + partFile.length() - 1);

            setCurrentChunk(chunk);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * returns the current generated chunk
     *
     * @return
     */
    public UploadFile getCurrentChunk() {
        return currentChunk;
    }

    /**
     * sets the current generated chunk
     *
     * @param currentChunk
     */
    public void setCurrentChunk(UploadFile currentChunk) {
        this.currentChunk = currentChunk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadFile)) return false;

        UploadFile that = (UploadFile) o;

        return path.equals(that.path);

    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
