Android Upload Service
======================

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Upload%20Service-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2161) [ ![Download](https://api.bintray.com/packages/alexbbb/maven/android-upload-service/images/download.svg) ](https://bintray.com/alexbbb/maven/android-upload-service/_latestVersion) [![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alexgotev%40gmail%2ecom&lc=US&item_name=Android%20Upload%20Service&item_number=AndroidUploadService&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

![Upload Notification](http://alexbbb.github.io/android-upload-service/upload.gif)

Easily upload files in the background with automatic Android Notification Center progress indication.

### Purpose
* upload files to a server with HTTP `multipart/form-data` or binary requests
* handle uploads in the background, even if the device is idle
* automatically retry failed uploads, with an exponential backoff
* possibility to automatically delete successfully uploaded files from the device
* show status in the Android Notification Center.

At the core of the library there is a `Service` which handles multiple concurrent upload tasks in the background. It publishes broadcast intents to notify status. This way the logic is completely decoupled from the UI. Read further to learn how you can use it in your App.

### Index
* [Setup](#setup)
* [JavaDocs](http://alexbbb.github.io/android-upload-service/javadoc/) 
* [HTTP Multipart Upload](#multipart)
* [Binary Upload](#binary)
* [Monitoring upload status](#monitoring-upload)
* [Stop upload tasks](#stop-upload-tasks)
* [Testing upload](#testing-upload)
* [Example app and server implementations](#examples)
* [How to use self-signed certificates](#certificates)
* [Uploading only when a connection is available](#host-monitor)
* [Apps powered by Android Upload Service](#powered)
* [Contribute](#contribute)
* [Before asking for help...](#help)
* [Do you like the project?](#donate)
* [License](#license)

### Setup <a name="setup"></a>
Ensure that you have jcenter in your gradle build file:
```
repositories {
    jcenter()
}
```
then in your dependencies section add:

```
dependencies {
    compile 'com.alexbbb:uploadservice:2.0'
}
```

and do a project sync.
If you're upgrading to 2.0 from 1.6, read [2.0 migration notes](https://github.com/alexbbb/android-upload-service/releases/tag/2.0).
If you're upgrading to 2.0 from 1.5 or older releases, [read 1.6 migration notes first](https://github.com/alexbbb/android-upload-service/releases/tag/1.6).

To start using the library, you have to initialize it in your [Application subclass](http://developer.android.com/reference/android/app/Application.html):
```java
public class Initializer extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // setup the broadcast action namespace string which will
        // be used to notify upload status.
        // Gradle automatically generates proper variable as below.
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
        // Or, you can define it manually.
        UploadService.NAMESPACE = "com.yourcompany.yourapp";
    }
}
```
and now you're ready to rock! Read further to discover how to do upload requests.

<em>I strongly encourage you to build and run the demo app that you can find in the [examples](#examples), together with one of the provided server implementations and to check [JavaDocs](http://alexbbb.github.io/android-upload-service/javadoc/).</em>

### HTTP Multipart Upload <a name="multipart"></a>
This is the most common way to upload files on a server. It's the same kind of request that browsers do when you use the `<form>` tag with one or more files. Here's a minimal example:

```java
public void uploadMultipart(final Context context) {

    final String uploadID = UUID.randomUUID().toString();
    final String serverUrlString = "http://www.yoursite.com/yourscript";

    try {
        new MultipartUploadRequest(context, uploadID, serverUrlString)
            .addFileToUpload("/absolute/path/to/your/file", "your-param-name")
            .setNotificationConfig(new UploadNotificationConfig())
            .setMaxRetries(2)
            .startUpload();
    } catch (Exception exc) {
        Log.e("AndroidUploadService", exc.getMessage(), exc);
    }
}
```
To discover all the available options, check [MultipartUploadRequest JavaDocs](http://alexbbb.github.io/android-upload-service/javadoc/com/alexbbb/uploadservice/MultipartUploadRequest.html)

### Binary Upload <a name="binary"></a>
The binary upload uses a single file as the raw body of the upload request.
Here's a minimal example:

``` java
public void uploadBinary(final Context context) {

    final String uploadID = UUID.randomUUID().toString();
    final String serverUrlString = "http://www.yoursite.com/yourscript";

    try {
        new BinaryUploadRequest(context, uploadID, serverUrlString)
            .setFileToUpload("/absolute/path/to/your/file")
            .setNotificationConfig(new UploadNotificationConfig())
            .setMaxRetries(2)
            .startUpload();
    } catch (Exception exc) {
        Log.e("AndroidUploadService", exc.getMessage(), exc);
    }
}
```
To discover all the available options, check [BinaryUploadRequest JavaDocs](http://alexbbb.github.io/android-upload-service/javadoc/com/alexbbb/uploadservice/BinaryUploadRequest.html)

### Monitoring upload status <a name="monitoring-upload"></a>
To listen for the status of the upload tasks, use the provided [UploadServiceBroadcastReceiver](http://alexbbb.github.io/android-upload-service/javadoc/com/alexbbb/uploadservice/UploadServiceBroadcastReceiver.html) in one of the following ways:
* use it inside a `Service` or `Activity` (check `register` and `unregister` methods JavaDoc for detailed instructions)
* create a new class (e.g. MyReceiver) which extends `UploadServiceBroadcastReceiver` and then register it as a broadcast receiver in your manifest, with the intent filter `com.yourcompany.yourapp.uploadservice.broadcast.status`. Change `com.yourcompany.yourapp` with whatever you have set as `UploadService.NAMESPACE` in the initial setup.
```xml
<receiver android:name="MyReceiver">
    <intent-filter>
        <action android:name="com.yourcompany.yourapp.uploadservice.broadcast.status" />
    </intent-filter>
</receiver>
```

Override its methods to add your own business logic. Example on how to use it in an activity:

```java
public class YourActivity extends Activity {

    private static final String TAG = "AndroidUploadService";

    private final UploadServiceBroadcastReceiver uploadReceiver =
    new UploadServiceBroadcastReceiver() {

        // you can override this progress method if you want to get
        // the completion progress in percent (0 to 100)
        // or if you need to know exactly how many bytes have been transferred
        // override the method below this one
        @Override
        public void onProgress(String uploadId, int progress) {
            Log.i(TAG, "The progress of the upload with ID "
                       + uploadId + " is: " + progress);
        }

        @Override
        public void onProgress(final String uploadId,
                               final long uploadedBytes,
                               final long totalBytes) {
            Log.i(TAG, "Upload with ID " + uploadId +
                       " uploaded bytes: " + uploadedBytes
                       + ", total: " + totalBytes);
        }

        @Override
        public void onError(String uploadId, Exception exception) {
            Log.e(TAG, "Error in upload with ID: " + uploadId + ". "
                       + exception.getLocalizedMessage(), exception);
        }

        @Override
        public void onCompleted(String uploadId,
                                int serverResponseCode,
                                byte[] serverResponseBody) {
            Log.i(TAG, "Upload with ID " + uploadId
                       + " has been completed with HTTP " + serverResponseCode
                       + ". Response from server: "
                       + new String(serverResponseBody));

            //If your server responds with a JSON, you can parse it
            //from serverResponseBody using a library
            //such as org.json (embedded in Android) or Google's gson
        }

        @Override
        public void onCancelled(String uploadId) {
            Log.i(TAG, "Upload with ID " + uploadId
                       + " has been cancelled by the user");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        uploadReceiver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uploadReceiver.unregister(this);
    }

}
```

If you want to monitor upload status in all of your activities, just implement the [UploadServiceBroadcastReceiver](http://alexbbb.github.io/android-upload-service/javadoc/com/alexbbb/uploadservice/UploadServiceBroadcastReceiver.html) in your base activity class from which all of your activities inherits and you're done.

### Stop upload tasks <a name="stop-upload-tasks"></a>
Call this method from anywhere you want to stop all the active upload tasks.
```java
UploadService.stopAllUploads();
```
After that each upload task is stopped, your [broadcast receiver's](http://alexbbb.github.io/android-upload-service/javadoc/com/alexbbb/uploadservice/UploadServiceBroadcastReceiver.html) `onCancelled(final String uploadId)` method will be called.

You can stop a particular upload with `UploadService.stopUpload(uploadId);`

### Testing upload <a name="testing-upload"></a>
You have the following choices:
* Use your own server (which handles one or more of the supported upload types)
* Use one of the server implementations provided in the examples (read below)
* Use the excellent http://www.posttestserver.com/ (bear in mind that the data you post there is public!) for HTTP Multipart requests

### Examples <a name="examples"></a>
In the <b>examples</b> folder you will find:

* A simple Android application that uses this library

* Demo servers which handle upload in:
  * <b>node.js (HTTP Multipart and Binary)</b>. You need to have node.js and npm installed. [Refer to this guide](https://github.com/joyent/node/wiki/installing-node.js-via-package-manager). To run the server, open a terminal, navigate to ```examples/server-nodejs``` folder and simply execute:

    ```
    npm install (only the first time)
    npm start
    ```
    The following endpoints will be available for upload testing:
    ```
    HTTP/Multipart: http://YOUR_LOCAL_IP:3000/upload/multipart
    Binary:         http://YOUR_LOCAL_IP:3000/upload/binary
    ```
  * <b>PHP (HTTP Multipart only)</b>. You need a running web server (e.g. Apache + PHP) in which to put the script. To get up and running in minutes you can use a solution like [XAMPP (supports Windows, OS X and Linux)](https://www.apachefriends.org/download.html).

### How to use self-signed certificates <a name="certificates"></a>
For security reasons, the library doesn't accept self-signed certificates by default when using HTTPS connections, but you can enable them by calling:

```java
AllCertificatesAndHostsTruster.apply();
```

before starting the upload service.

### Uploading only when a connection is available <a name="host-monitor"></a>
If you want to start uploads or retry them based on the remote server's reachability status, [Android Host Monitor](https://github.com/alexbbb/android-host-monitor) may be useful to you in combination with this library.

### Apps powered by Android Upload Service <a name="powered"></a>
To be included in the following list, simply create an issue and provide the app name and a link.

- [VoiSmart IP Communicator](https://play.google.com/store/apps/details?id=com.voismart.softphone)
- [DotShare](http://dot-share.com/index-en.html)
- [NativeScript Background HTTP](https://www.npmjs.com/package/nativescript-background-http)

### Contribute <a name="contribute"></a>
* Do you have a new feature in mind?
* Do you know how to improve existing docs or code?
* Have you found a bug?

Contributions are welcome and encouraged! Just fork the project and then send a pull request. Be ready to discuss your code and design decisions :)

### Before asking for help... <a name="help"></a>
Let's face it, doing network programming is not easy as there are many things that can go wrong, but if upload doesn't work out of the box, consider the following things before posting a new issue:
* Check [JavaDocs](http://alexbbb.github.io/android-upload-service/javadoc/) for full class and methods docs
* Is the server URL correct?
* Is the server URL reachable from your device? Check if there are firewalls or other kind of restrictions between your device and the server.
* Are you sure that the server side is working properly? For example, if you use PHP in your server side, and you get an EPIPE exception, check if the content size you are trying to upload exceeds the values of `upload_max_filesize` or `post_max_size` set in your `php.ini`
* Have you properly set up the request with all the headers, parameters and files that the server expects?
* Have you tried to make an upload using the demo app and one of the provided server implementations? I use the node.js version which provides good feedback and supports both HTTP Multipart and binary uploads.

If you've checked all the above and still something goes wrong...it's time to create a new issue! Be sure to include the following info:
* Android API version
* Device vendor and model
* Code used to generate the request. Replace sensible data values.
* LogCat output
* Server output

Please make use of Markdown styling when you post code or console output.

### Do you like the project? <a name="donate"></a>
Put a star, spread the word and if you want to offer me a free beer, [![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alexgotev%40gmail%2ecom&lc=US&item_name=Android%20Upload%20Service&item_number=AndroidUploadService&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

### License <a name="license"></a>

    Copyright (C) 2013-2016 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
