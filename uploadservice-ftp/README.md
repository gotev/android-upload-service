FTP Upload for Android Upload Service
============================================

This module adds FTP upload ([RFC959](https://tools.ietf.org/html/rfc959)) capability to Android Upload Service. It wraps [Apache Commons Net FTP](https://commons.apache.org/proper/commons-net/dependency-info.html) library.

## Setup
Refer to the [Getting Started](https://github.com/gotev/android-upload-service/wiki/Getting-Started-with-4.x) guide to get the latest upload service version.

Add the following to your dependencies:
```groovy
implementation "net.gotev:uploadservice-ftp:$uploadServiceVersion"
```

## Minimal example

Example in kotlin:
```kotlin
FTPUploadRequest(context, "my.ftpserver.com", 21)
    .setUsernameAndPassword("ftpuser", "testpassword")
    .addFileToUpload("/absolute/path/to/file", "/remote/path")
    .startUpload();
```
Check the FTPUploadRequest class and its docs for all the available features. To monitor upload progress and status, check: https://github.com/gotev/android-upload-service/wiki/Monitor-Uploads

> By default, the global [notification configuration](https://github.com/gotev/android-upload-service/wiki/Configuration#notifications) and [retry policy](https://github.com/gotev/android-upload-service/wiki/Configuration#retry-policy) will be applied. You can override both in each request using `setNotificationConfig` and `setMaxRetries` methods.

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
