package com.alexbbb.uploadservice.demo;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.alexbbb.uploadservice.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Divish on 1/15/2015.
 */
public class Tests {
    Context context;
    String url;
    String param;

    public Tests(Context context, String url, String param) {
        this.context = context;
        this.url = url;
        this.param = param;

    }




    public List<String> createImagesFromAssets() throws IOException {

         List<String> files = Utils.copyAssets(context);
        log(files.size() + " image files copied from asset");
        return files;

    }

    public void uploadRequests(ArrayList<UploadRequest> requests) {
        boolean errorOccured = false;
        if (requests.isEmpty()){
            log("Requests list is empty");
        }
        for (UploadRequest request : requests){
            try {
                UploadService.startUpload(request);

            } catch (MalformedURLException e) {
                e.printStackTrace();
                log("Error starting request no." + requests.indexOf(request));
                errorOccured = true;

            }
        }


    }

    public UploadRequest createEmptyRequest(){
        final UploadRequest request = new UploadRequest(context, receiver, UUID
                .randomUUID().toString(), url);


                request.setNotificationConfig(R.drawable.ic_launcher,
                        ("Upload test"),
                        "Uploading",
                        "Upload success",
                        "Upload Error", false);
                //UploadService.startUpload(request);


        return request;
    }

    public UploadRequest createRequestFromImages(List<String> list){
        final UploadRequest request = new UploadRequest(context, receiver, UUID
                .randomUUID().toString(), url);

        try {
            for (String file : list) {

                request.addFileToUpload(file, param, new File(file).getName(),
                        ContentType.APPLICATION_OCTET_STREAM);
                //log(file + "sent in task " + request.);
                request.setNotificationConfig(R.drawable.ic_launcher,
                        ("Upload test"),
                        "Uploading",
                        "Upload success",
                        "Upload Error", false);
                //UploadService.startUpload(request);
            }
        } catch (Exception exc) {

            log("Malformed upload request. " + exc.getLocalizedMessage());
        }
        return request;
    }


    //complete task progress return receiver
    private AbstractFileUploadResultReceiver receiver = new AbstractFileUploadResultReceiver(
            new Handler()) {

        @Override
        public void onFilesUploadResultReceived(
                ArrayList<FileToUpload> files , String uploadId) {
            for(FileToUpload file : files){
                //	System.out.println("Is file successful " + file.isUploaded());
                log("	RESULT : uploadId: " + uploadId + ", file:" + file.getFileName() + " : " + file.isUploaded());
            }
        }

        @Override
        public void onLog(String message) {
            log(message);
        }
    };

    //task progress result return receiver
    public final AbstractUploadServiceReceiver uploadReceiver = new AbstractUploadServiceReceiver() {



        @Override
        public void onProgress(String uploadId, int progressTask, int progressCurrentFile, String fileNameBeingUploaded) {
            ((FileProgressListener)context).setFileProgress(uploadId, fileNameBeingUploaded, progressCurrentFile);
            ((RequestProgressListener)context).setRequestProgress(uploadId, progressTask);
        }

        @Override
        public void onError(String uploadId, Exception exception) {

        }

        @Override
        public void onCompleted(String uploadId, int serverResponseCode,
                                String serverResponseMessage) {

        }
    };

    public void registerServiceReceiver(Activity activity){
        uploadReceiver.register(activity);
    }
    public void unRegisterServiceReceiver(Activity activity){
        uploadReceiver.unregister(activity);
    }
    private void log(String message){
        App.log(message);
    }
}
