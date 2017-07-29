package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Class which contains specific parameters for HTTP uploads.
 *
 * @author gotev (Aleksandar Gotev)
 */
public final class HttpUploadTaskParameters implements Parcelable {

    protected static final String PARAM_HTTP_TASK_PARAMETERS = "httpTaskParameters";

    public String customUserAgent;
    public String method = "POST";
    public boolean usesFixedLengthStreamingMode = true;
    private ArrayList<NameValue> requestHeaders = new ArrayList<>(10);
    private ArrayList<NameValue> requestParameters = new ArrayList<>(10);

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

    public boolean isCustomUserAgentDefined() {
        return customUserAgent != null && !"".equals(customUserAgent);
    }

    public HttpUploadTaskParameters addHeader(String name, String value) {
        requestHeaders.add(NameValue.header(name, value));
        return this;
    }

    public ArrayList<NameValue> getRequestHeaders() {
        return requestHeaders;
    }

    public HttpUploadTaskParameters addParameter(String name, String value) {
        requestParameters.add(new NameValue(name, value));
        return this;
    }

    public ArrayList<NameValue> getRequestParameters() {
        return requestParameters;
    }
}
