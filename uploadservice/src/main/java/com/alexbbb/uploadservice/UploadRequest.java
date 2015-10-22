package com.alexbbb.uploadservice;

import android.content.Context;

/**
 * Represents a multipart upload request.
 * Exposes the same functionality as the MultipartUploadRequest but is here for backward compatibility.
 *
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class UploadRequest extends MultipartUploadRequest {

    /**
     * Creates a new multipart upload request.
     *
     * @deprecated As of 1.4, use MultipartUploadRequest
     * @param context application context
     * @param uploadId unique ID to assign to this upload request.
     *                 It's used in the broadcast receiver when receiving updates.
     * @param serverUrl URL of the server side script that handles the multipart form upload
     */
    public UploadRequest(final Context context, final String uploadId, final String serverUrl) {
        super(context, uploadId, serverUrl);
    }
}
