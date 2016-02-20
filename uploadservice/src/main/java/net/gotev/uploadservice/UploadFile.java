package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Represents a file to upload.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class UploadFile implements Parcelable {

    private static final String LOG_TAG = UploadFile.class.getSimpleName();

    private static final String NEW_LINE = "\r\n";
    private static final String UNUSED = "UNUSED";

    protected final File file;
    protected final String paramName;
    protected final String fileName;
    protected String contentType;

    private final Charset US_ASCII = Charset.forName("US-ASCII");
    private final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Creates a new UploadFile.
     *
     * @param path absolute path to the file
     * @param parameterName parameter name of this file in the request
     * @param fileName file name of this file
     * @param contentType content type of this file. Set this to null to auto detect.
     * @throws FileNotFoundException if the file can't be found at the specified path
     * @throws IllegalArgumentException if you passed invalid argument values
     */
    UploadFile(final String path, final String parameterName,
               final String fileName, final String contentType)
            throws FileNotFoundException, IllegalArgumentException {

        if (path == null || "".equals(path)) {
            throw new IllegalArgumentException("Please specify a file path! Passed path value is: " + path);
        }

        File file = new File(path);
        if (!file.exists()) throw new FileNotFoundException("Could not find file at path: " + path);
        this.file = file;

        if (parameterName == null || "".equals(parameterName)) {
            throw new IllegalArgumentException("Please specify parameterName value for file: " + path);
        }

        this.paramName = parameterName;

        if (contentType == null || contentType.isEmpty()) {
            this.contentType = autoDetectMimeType();
            Logger.debug(LOG_TAG, "Detected MIME type for " + file.getAbsolutePath()
                         + " is: " + contentType);
        } else {
            this.contentType = contentType;
            Logger.debug(LOG_TAG, "Content Type set for " + file.getAbsolutePath()
                         + " is: " + contentType);
        }

        if (fileName == null || "".equals(fileName)) {
            this.fileName = this.file.getName();
            Logger.debug(LOG_TAG, "Using original file name: " + fileName);
        } else {
            this.fileName = fileName;
            Logger.debug(LOG_TAG, "Using custom file name: " + fileName);
        }
    }

    /**
     * Creates a new UploadFile by specifying only the full absolute path to it. All the other
     * values are set to {@code UNUSED}. Use this constructor when you don't need to set parameter
     * name, file name and content type.
     * @param path absolute path to the file
     * @throws FileNotFoundException if the file can't be found at the specified path
     */
    UploadFile(final String path) throws FileNotFoundException {
        this(path, UNUSED, UNUSED, UNUSED);
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
     * Gets the HTTP/Multipart header for this file.
     * @param isUtf8 true to get the multipart header in UTF-8 charset, false to use US-ASCII
     * @return multipart header bytes
     * @throws UnsupportedEncodingException if the device does not support the selected encoding
     */
    public byte[] getMultipartHeader(boolean isUtf8) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();

        builder.append("Content-Disposition: form-data; name=\"")
                .append(paramName).append("\"; filename=\"")
                .append(fileName).append("\"").append(NEW_LINE);

        builder.append("Content-Type: ").append(contentType).append(NEW_LINE).append(NEW_LINE);

        return builder.toString().getBytes(isUtf8 ? UTF8 : US_ASCII);
    }

    /**
     * Get the total number of bytes needed by this file in the HTTP/Multipart request, considering
     * that to send each file there is some overhead due to some bytes needed for the boundary
     * and some bytes needed for the multipart headers
     *
     * @param boundaryBytesLength length in bytes of the multipart boundary
     * @param isUtf8 true to get the multipart header in UTF-8 charset, false to use US-ASCII
     * @return total number of bytes needed by this file in the HTTP/Multipart request
     * @throws UnsupportedEncodingException if the device does not support the selected encoding
     */
    public long getTotalMultipartBytes(long boundaryBytesLength, boolean isUtf8)
            throws UnsupportedEncodingException {
        return boundaryBytesLength + getMultipartHeader(isUtf8).length + file.length();
    }

    private String autoDetectMimeType() {
        String extension = null;

        String absolutePath = this.file.getAbsolutePath();
        int index = absolutePath.lastIndexOf(".") + 1;

        if (index >= 0 && index <= absolutePath.length()) {
            extension = absolutePath.substring(index);
        }

        if (extension == null || extension.isEmpty()) {
            return ContentType.APPLICATION_OCTET_STREAM;
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

        if (mimeType == null) {
            return ContentType.APPLICATION_OCTET_STREAM;
        }

        return mimeType;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(file.getAbsolutePath());
        parcel.writeString(paramName);
        parcel.writeString(fileName);
        parcel.writeString(contentType);
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

    protected UploadFile(Parcel in) {
        file = new File(in.readString());
        paramName = in.readString();
        fileName = in.readString();
        contentType = in.readString();
    }
}
