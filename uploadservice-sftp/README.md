FTP Upload for Android Upload Service
============================================

This module adds SFTP upload  capability to Android Upload Service. It wraps [JSch FTP](http://www.jcraft.com/jsch/) library.

## Setup
Refer to [UploadService Setup](https://github.com/gotev/android-upload-service/wiki/Setup) to get the latest upload service version
```groovy
compile "net.gotev:uploadservice-sftp:$uploadServiceVersion"
```

## Minimal example
```java
public void uploadFTP(final Context context) {
    try {
        String uploadId =
          new SFTPUploadRequest(context, "my.sftpserver.com", 22)
            .setUsernameAndPassword("sftpuser", "testpassword")
            .addFileToUpload("/absolute/path/to/file", "/remote/path")
            .setNotificationConfig(new UploadNotificationConfig())
            .setMaxRetries(4)
            .startUpload();
    } catch (Exception exc) {
        Log.e("AndroidUploadService", exc.getMessage(), exc);
    }
}
```
Refer to [SFTPUploadRequest JavaDoc](http://gotev.github.io/android-upload-service/javadoc-ftp/net/gotev/uploadservice/sftp/SFTPUploadRequest.html) for all the available features.