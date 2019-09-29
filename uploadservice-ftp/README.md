FTP Upload for Android Upload Service
============================================

This module adds FTP upload ([RFC959](https://tools.ietf.org/html/rfc959)) capability to Android Upload Service. It wraps [Apache Commons Net FTP](https://commons.apache.org/proper/commons-net/dependency-info.html) library.

## Setup
Refer to [UploadService Setup](https://github.com/gotev/android-upload-service/wiki/Setup) to get the latest upload service version
```groovy
compile "net.gotev:uploadservice-ftp:$uploadServiceVersion"
```

## Minimal example
```java
public void uploadFTP(final Context context) {
    try {
        String uploadId =
          new FTPUploadRequest(context, "my.ftpserver.com", 21)
            .setUsernameAndPassword("ftpuser", "testpassword")
            .addFileToUpload("/absolute/path/to/file", "/remote/path")
            .setNotificationConfig(new UploadNotificationConfig())
            .setMaxRetries(4)
            .startUpload();
    } catch (Exception exc) {
        Log.e("AndroidUploadService", exc.getMessage(), exc);
    }
}
```
Refer to [FTPUploadRequest JavaDoc](http://gotev.github.io/android-upload-service/javadoc-ftp/net/gotev/uploadservice/ftp/FTPUploadRequest.html) for all the available features.

## Test FTP server
If you don't already have an FTP server to make tests, no worries. You only need docker installed

First of all, check out `android-upload-service` (you need git. I'm not going to cover how to install that) and navigate to the `test-server` directory:
```
git clone https://github.com/gotev/android-upload-service.git
cd android-upload-service/uploadservice-ftp/test-server
```

### OS X
I'm assuming you've followed the instructions in the previous paragraph and you are already in a terminal in `android-upload-service/uploadservice-ftp/test-server` directory.
```
alex@mbp:~$ ./vsftpd-osx
```
#### Access Credentials
* IP: Your local IP address
* PORT: 21
* Username: myuser
* Password: mypass
