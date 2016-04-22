package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Contains upload information and statistics.
 * @author Aleksandar Gotev
 */
public class UploadInfo implements Parcelable {

    private String uploadId;
    private long startTime;
    private long currentTime;
    private long uploadedBytes;
    private long totalBytes;
    private int numberOfRetries;
    private ArrayList<String> successfullyUploadedFiles = new ArrayList<>();

    public UploadInfo(String uploadId, long startTime, long uploadedBytes, long totalBytes,
                      int numberOfRetries, List<String> uploadedFiles) {
        this.uploadId = uploadId;
        this.startTime = startTime;
        currentTime = new Date().getTime();
        this.uploadedBytes = uploadedBytes;
        this.totalBytes = totalBytes;
        this.numberOfRetries = numberOfRetries;

        if (uploadedFiles != null && !uploadedFiles.isEmpty())
            successfullyUploadedFiles.addAll(uploadedFiles);
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<UploadInfo> CREATOR =
            new Parcelable.Creator<UploadInfo>() {
                @Override
                public UploadInfo createFromParcel(final Parcel in) {
                    return new UploadInfo(in);
                }

                @Override
                public UploadInfo[] newArray(final int size) {
                    return new UploadInfo[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(uploadId);
        parcel.writeLong(startTime);
        parcel.writeLong(currentTime);
        parcel.writeLong(uploadedBytes);
        parcel.writeLong(totalBytes);
        parcel.writeInt(numberOfRetries);
        parcel.writeStringList(successfullyUploadedFiles);
    }

    private UploadInfo(Parcel in) {
        uploadId = in.readString();
        startTime = in.readLong();
        currentTime = in.readLong();
        uploadedBytes = in.readLong();
        totalBytes = in.readLong();
        numberOfRetries = in.readInt();
        in.readStringList(successfullyUploadedFiles);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns the Upload ID.
     * @return string
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Gets upload task's start timestamp in milliseconds.
     * @return long value
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets upload task's elapsed time in milliseconds.
     * @return long value
     */
    public long getElapsedTime() {
        return (currentTime - startTime);
    }

    /**
     * Gets the average upload rate in Kbit/s.
     * @return upload rate
     */
    public double getUploadRate() {
        return (double) uploadedBytes / 1024 * 8 / (getElapsedTime() / 1000);
    }

    /**
     * Gets the list of the successfully uploaded files.
     * @return {@link ArrayList<String>}
     */
    public ArrayList<String> getSuccessfullyUploadedFiles() {
        return successfullyUploadedFiles;
    }

    /**
     * Gets the uploaded bytes.
     * @return long value
     */
    public long getUploadedBytes() {
        return uploadedBytes;
    }

    /**
     * Gets upload task's total bytes.
     * @return long value
     */
    public long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Gets the upload progress in percent (from 0 to 100).
     * @return integer value
     */
    public int getProgressPercent() {
        return (int) (uploadedBytes * 100 / totalBytes);
    }

    /**
     * Gets the number of the retries that has been made during the upload process.
     * If no retries has been made, this value will be zero.
     * @return int value
     */
    public int getNumberOfRetries() {
        return numberOfRetries;
    }
}
