package com.alexbbb.uploadservice;

/**
 * @author AZ Aizaz
 */

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore.Files;

public abstract class AbstractFileUploadResultReceiver extends ResultReceiver {

    public AbstractFileUploadResultReceiver(Handler handler) {
        super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        
        if(resultCode == UploadRequest.CODE_RESULT_UPLOAD_FILES){
            ArrayList<FileToUpload> list = resultData.getParcelableArrayList(UploadRequest.KEY_RESULT_UPLAOD_FILES);
            this.onFilesUploadResultReceived(list);
        }
        
    }

    abstract public void onFilesUploadResultReceived(ArrayList<FileToUpload> files);

}
