package net.gotev.uploadservice.schemehandlers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import net.gotev.uploadservice.ContentType;
import net.gotev.uploadservice.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Handles Android content uris, wraps android.content.Uri
 * @author stephentuso
 * @author gotev
 */
class ContentSchemeHandler implements SchemeHandler {

    private Uri uri;

    @Override
    public void init(String path) {
        uri = Uri.parse(path);
    }

    @Override
    public long getLength(Context context) {
        return getUriSize(context);
    }

    @Override
    public InputStream getInputStream(Context context) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(uri);
    }

    @Override
    public String getContentType(Context context) {
        String type = context.getContentResolver().getType(uri);
        if (type == null || type.isEmpty()) {
            type = ContentType.APPLICATION_OCTET_STREAM;
        }
        return type;
    }

    @Override
    public String getName(Context context) {
        return getUriName(context);
    }

    private long getUriSize(Context context) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            Logger.error(getClass().getSimpleName(), "null cursor for " + uri + ", returning size 0");
            return 0;
        }
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        cursor.moveToFirst();
        long size = cursor.getLong(sizeIndex);
        cursor.close();
        return size;
    }

    private String getUriName(Context context) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return getUriNameFallback();
        }
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String name = cursor.getString(nameIndex);
        cursor.close();
        return name;
    }

    private String getUriNameFallback() {
        String[] components = uri.toString().split(File.separator);
        return components[components.length - 1];
    }

}
