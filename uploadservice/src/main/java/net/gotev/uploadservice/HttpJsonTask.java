package net.gotev.uploadservice;

import android.content.Intent;

import net.gotev.uploadservice.http.BodyWriter;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Implements an HTTP Multipart upload task.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class HttpJsonTask extends HttpUploadTask {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final String PARAM_UTF8_CHARSET = "multipartUtf8Charset";

    private Charset charset;
    private String jsonBody;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        httpParams.addHeader("Content-Type", "application/json");
        charset = intent.getBooleanExtra(PARAM_UTF8_CHARSET, false) ? Charset.forName("UTF-8") : US_ASCII;
        jsonBody = httpParams.getRequestParameters().get(0).getValue();
    }

    @Override
    protected long getBodyLength() {
        return jsonBody.length();
    }

    @Override
    public void onBodyReady(BodyWriter bodyWriter) throws IOException {
        bodyWriter.write(jsonBody.getBytes(charset));
    }
}
