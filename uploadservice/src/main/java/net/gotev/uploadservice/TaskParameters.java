package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class which contains all the parameters passed to the upload task.
 *
 * @author gotev (Aleksandar Gotev)
 */
public final class TaskParameters implements Parcelable {

    private String id;
    private String url;
    private String method = "POST";
    private String customUserAgent;
    private int maxRetries = 0;
    private boolean usesFixedLengthStreamingMode = true;
    private boolean autoDeleteSuccessfullyUploadedFiles = false;
    private UploadNotificationConfig notificationConfig;
    private ArrayList<NameValue> requestHeaders = new ArrayList<>();
    private ArrayList<NameValue> requestParameters = new ArrayList<>();
    private ArrayList<UploadFile> files = new ArrayList<>();

    public TaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<TaskParameters> CREATOR =
            new Parcelable.Creator<TaskParameters>() {
                @Override
                public TaskParameters createFromParcel(final Parcel in) {
                    return new TaskParameters(in);
                }

                @Override
                public TaskParameters[] newArray(final int size) {
                    return new TaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(id);
        parcel.writeString(url);
        parcel.writeString(method);
        parcel.writeString(customUserAgent);
        parcel.writeInt(maxRetries);
        parcel.writeByte((byte) (autoDeleteSuccessfullyUploadedFiles ? 1 : 0));
        parcel.writeByte((byte) (usesFixedLengthStreamingMode ? 1 : 0));
        parcel.writeParcelable(notificationConfig, 0);
        parcel.writeList(requestHeaders);
        parcel.writeList(requestParameters);
        parcel.writeList(files);
    }

    private TaskParameters(Parcel in) {
        id = in.readString();
        url = in.readString();
        method = in.readString();
        customUserAgent = in.readString();
        maxRetries = in.readInt();
        autoDeleteSuccessfullyUploadedFiles = in.readByte() == 1;
        usesFixedLengthStreamingMode = in.readByte() == 1;
        notificationConfig = in.readParcelable(UploadNotificationConfig.class.getClassLoader());
        in.readList(requestHeaders, NameValue.class.getClassLoader());
        in.readList(requestParameters, NameValue.class.getClassLoader());
        in.readList(files, UploadFile.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void addFile(UploadFile file)
            throws FileNotFoundException {
        files.add(file);
    }

    public List<UploadFile> getFiles() {
        return files;
    }

    public void addRequestHeader(String name, String value) {
        requestHeaders.add(new NameValue(name, value));
    }

    public void addRequestParameter(String name, String value) {
        requestParameters.add(new NameValue(name, value));
    }

    public List<NameValue> getRequestHeaders() {
        return requestHeaders;
    }

    public List<NameValue> getRequestParameters() {
        return requestParameters;
    }

    public UploadNotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    public TaskParameters setNotificationConfig(UploadNotificationConfig notificationConfig) {
        this.notificationConfig = notificationConfig;
        return this;
    }

    public String getId() {
        return id;
    }

    public TaskParameters setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public TaskParameters setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public TaskParameters setMethod(String method) {
        if (method != null && method.length() > 0)
            this.method = method;
        return this;
    }

    public String getCustomUserAgent() {
        return customUserAgent;
    }

    public boolean isCustomUserAgentDefined() {
        return customUserAgent != null && !"".equals(customUserAgent);
    }

    public TaskParameters setCustomUserAgent(String customUserAgent) {
        this.customUserAgent = customUserAgent;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public TaskParameters setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;

        return this;
    }

    public boolean isAutoDeleteSuccessfullyUploadedFiles() {
        return autoDeleteSuccessfullyUploadedFiles;
    }

    public TaskParameters setAutoDeleteSuccessfullyUploadedFiles(boolean autoDeleteSuccessfullyUploadedFiles) {
        this.autoDeleteSuccessfullyUploadedFiles = autoDeleteSuccessfullyUploadedFiles;
        return this;
    }

    public boolean isUsesFixedLengthStreamingMode() {
        return usesFixedLengthStreamingMode;
    }

    public TaskParameters setUsesFixedLengthStreamingMode(boolean usesFixedLengthStreamingMode) {
        this.usesFixedLengthStreamingMode = usesFixedLengthStreamingMode;
        return this;
    }
}
