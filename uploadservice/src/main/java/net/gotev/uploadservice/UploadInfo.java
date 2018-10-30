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
    private Integer notificationID;
    private ArrayList<String> filesLeft = new ArrayList<>();
    private ArrayList<String> successfullyUploadedFiles = new ArrayList<>();

    protected UploadInfo(String uploadId) {
        this.uploadId = uploadId;
        startTime = 0;
        currentTime = 0;
        uploadedBytes = 0;
        totalBytes = 0;
        numberOfRetries = 0;
        notificationID = null;
    }

    protected UploadInfo(String uploadId, long startTime, long uploadedBytes, long totalBytes,
                         int numberOfRetries, List<String> uploadedFiles, List<String> filesLeft) {
        this.uploadId = uploadId;
        this.startTime = startTime;
        currentTime = new Date().getTime();
        this.uploadedBytes = uploadedBytes;
        this.totalBytes = totalBytes;
        this.numberOfRetries = numberOfRetries;

        if (filesLeft != null && !filesLeft.isEmpty()) {
            this.filesLeft.addAll(filesLeft);
        }

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
        parcel.writeInt(notificationID == null ? -1 : notificationID);
        parcel.writeStringList(filesLeft);
        parcel.writeStringList(successfullyUploadedFiles);
    }

    private UploadInfo(Parcel in) {
        uploadId = in.readString();
        startTime = in.readLong();
        currentTime = in.readLong();
        uploadedBytes = in.readLong();
        totalBytes = in.readLong();
        numberOfRetries = in.readInt();

        notificationID = in.readInt();
        if (notificationID == -1) {
            notificationID = null;
        }

        in.readStringList(filesLeft);
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
     * Gets the elapsed time as a string, expressed in seconds if the value is {@code < 60},
     * or expressed in minutes:seconds if the value is {@code >=} 60.
     * @return string representation of the elapsed time
     */
    public String getElapsedTimeString() {
        int elapsedSeconds = (int) (getElapsedTime() / 1000);

        if (elapsedSeconds == 0)
            return "0s";

        int minutes = elapsedSeconds / 60;
        elapsedSeconds -= (60 * minutes);

        if (minutes == 0) {
            return elapsedSeconds + "s";
        }

        return minutes + "m " + elapsedSeconds + "s";
    }

    /**
     * Gets the average upload rate in Kbit/s.
     * @return upload rate
     */
    public double getUploadRate() {
        long elapsedTime = getElapsedTime();

        // wait at least a second to stabilize the upload rate a little bit
        if (elapsedTime < 1000)
            return 0;

        return (double) uploadedBytes / 1024 * 8 / (elapsedTime / 1000);
    }

    /**
     * Returns a string representation of the upload rate, expressed in the most convenient unit of
     * measurement (Mbit/s if the value is {@code >=} 1024, B/s if the value is {@code < 1}, otherwise Kbit/s)
     * @return string representation of the upload rate (e.g. 234 Kbit/s)
     */
    public String getUploadRateString() {
        double uploadRate = getUploadRate();

        if (uploadRate < 1) {
            return (int) (uploadRate * 1000) + " bit/s";

        } else if (uploadRate >= 1024) {
            return (int) (uploadRate / 1024) + " Mbit/s";

        }

        return (int) uploadRate + " Kbit/s";
    }

    /**
     * Gets the list of the successfully uploaded files.
     * @return list of strings
     */
    public ArrayList<String> getSuccessfullyUploadedFiles() {
        return successfullyUploadedFiles;
    }

    /**
     * Gets the list of all the files left to be uploaded.
     * @return list of strings
     */
    public ArrayList<String> getFilesLeft() {
        return filesLeft;
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
        if (totalBytes == 0)
            return 0;

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

    /**
     * Gets the total number of files added to the upload request.
     * @return total number of files to upload
     */
    public int getTotalFiles() {
        return successfullyUploadedFiles.size() + filesLeft.size();
    }

    /**
     * Gets the notification ID.
     * @return Integer number or null if the upload task does not have a notification or the
     * notification is not dismissable at the moment (for example during upload progress).
     */
    public Integer getNotificationID() {
        return notificationID;
    }

    protected void setNotificationID(int id) {
        notificationID = id;
    }
}
