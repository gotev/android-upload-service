package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedHashMap;

/**
 * Contains the server response.
 * @author Aleksandar Gotev
 */
public class ServerResponse implements Parcelable {

    private int httpCode;
    private byte[] body;
    private LinkedHashMap<String, String> headers;

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<ServerResponse> CREATOR =
            new Parcelable.Creator<ServerResponse>() {
                @Override
                public ServerResponse createFromParcel(final Parcel in) {
                    return new ServerResponse(in);
                }

                @Override
                public ServerResponse[] newArray(final int size) {
                    return new ServerResponse[size];
                }
            };

    /**
     * Creates a new server response object.
     * @param httpCode HTTP response code got from the server. If you are implementing another
     *                 protocol, set this to {@link UploadTask#TASK_COMPLETED_SUCCESSFULLY}
     *                 to inform that the task has been completed successfully. Integer values
     *                 lower than 200 or greater that 299 indicates error response from server.
     * @param body bytes read from server's response body. If your server does not
     *             return anything, set this to {@link UploadTask#EMPTY_RESPONSE}.
     * @param headers contains all the headers sent by the server. Set this to null or
     *                an empty map if the server has not sent any response header.
     */
    public ServerResponse(int httpCode, byte[] body, LinkedHashMap<String, String> headers) {
        this.httpCode = httpCode;

        if (body != null && body.length > 0)
            this.body = body;
        else
            this.body = new byte[1];

        if (headers != null && !headers.isEmpty())
            this.headers = headers;
        else
            this.headers = new LinkedHashMap<>(1);
    }

    @SuppressWarnings("unchecked")
    protected ServerResponse(Parcel in) {
        httpCode = in.readInt();
        body = new byte[in.readInt()];
        in.readByteArray(body);
        headers = (LinkedHashMap<String, String>) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(httpCode);
        parcel.writeInt(body.length);
        parcel.writeByteArray(body);
        parcel.writeSerializable(headers);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Gets server HTTP response code.
     * @return integer value
     */
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * Gets server response body.
     * If your server responds with a string, you can get it with
     * {@link ServerResponse#getBodyAsString()}.
     * If the string is a JSON, you can parse it using a library such as org.json
     * (embedded in Android) or google's gson
     * @return response bytes
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Gets server response body as string.
     * If the string is a JSON, you can parse it using a library such as org.json
     * (embedded in Android) or google's gson
     * @return string
     */
    public String getBodyAsString() {
        return new String(body);
    }

    /**
     * Gets all the server response headers.
     * @return map containing all the headers (key = header name, value = header value)
     */
    public LinkedHashMap<String, String> getHeaders() {
        return headers;
    }
}
