package com.alexbbb.uploadservice.demo;

/**
 * Created by Divish on 1/15/2015.
 */
public interface FileProgressListener {
    public void setFileProgress(String uploadId, String fileName, int progress);
}
