package net.gotev.uploadservice.schemehandlers;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Allows for different file representations to be used by abstracting several characteristics
 * and operations
 * @author stephentuso
 */
public interface SchemeHandler {
    long getLength(Context context);
    InputStream getInputStream(Context context) throws FileNotFoundException;
    String getContentType(Context context);
    String getName(Context context);
}
