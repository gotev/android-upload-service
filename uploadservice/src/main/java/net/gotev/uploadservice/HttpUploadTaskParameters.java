package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which contains specific parameters for HTTP uploads.
 *
 * @author gotev (Aleksandar Gotev)
 */
public final class HttpUploadTaskParameters implements Parcelable {

    protected static final String PARAM_HTTP_TASK_PARAMETERS = "httpTaskParameters";

    private String customUserAgent;
    private String method = "POST";
    private boolean usesFixedLengthStreamingMode = true;
    private ArrayList<NameValue> requestHeaders = new ArrayList<>();
    private ArrayList<NameValue> requestParameters = new ArrayList<>();

    public HttpUploadTaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<HttpUploadTaskParameters> CREATOR =
            new Parcelable.Creator<HttpUploadTaskParameters>() {
                @Override
                public HttpUploadTaskParameters createFromParcel(final Parcel in) {
                    return new HttpUploadTaskParameters(in);
                }

                @Override
                public HttpUploadTaskParameters[] newArray(final int size) {
                    return new HttpUploadTaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(method);
        parcel.writeString(customUserAgent);
        parcel.writeByte((byte) (usesFixedLengthStreamingMode ? 1 : 0));
        parcel.writeList(requestHeaders);
        parcel.writeList(requestParameters);
    }

    private HttpUploadTaskParameters(Parcel in) {
        method = in.readString();
        customUserAgent = in.readString();
        usesFixedLengthStreamingMode = in.readByte() == 1;
        in.readList(requestHeaders, NameValue.class.getClassLoader());
        in.readList(requestParameters, NameValue.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void addRequestHeader(String name, String value) {
        requestHeaders.add(new NameValue(name, value, true));
    }

    public void addRequestParameter(String name, String value) {
        requestParameters.add(new NameValue(name, value, false));
    }

    public List<NameValue> getRequestHeaders() {
        return requestHeaders;
    }

    public List<NameValue> getRequestParameters() {
        return requestParameters;
    }

    public String getMethod() {
        return method;
    }

    public HttpUploadTaskParameters setMethod(String method) {
        if (method != null && method.length() > 0)
            this.method = method;
        return this;
    }

    public boolean isUsesFixedLengthStreamingMode() {
        return usesFixedLengthStreamingMode;
    }

    public HttpUploadTaskParameters setUsesFixedLengthStreamingMode(boolean usesFixedLengthStreamingMode) {
        this.usesFixedLengthStreamingMode = usesFixedLengthStreamingMode;
        return this;
    }

    public String getCustomUserAgent() {
        return customUserAgent;
    }

    public boolean isCustomUserAgentDefined() {
        return customUserAgent != null && !"".equals(customUserAgent);
    }

    public HttpUploadTaskParameters setCustomUserAgent(String customUserAgent) {
        this.customUserAgent = customUserAgent;
        return this;
    }
}
