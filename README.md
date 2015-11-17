Android Upload Service
======================

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Upload%20Service-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2161)

## Are you using Android Upload Service in your app?
Let me know, and I'll be glad to include a link in the following list :)

- [VoiSmart IP Communicator](https://play.google.com/store/apps/details?id=com.voismart.softphone)
- [DotShare](http://dot-share.com/index-en.html)
- [NativeScript Background HTTP] (https://www.npmjs.com/package/nativescript-background-http)

## Purpose
I needed an easy and efficient way to upload multipart form data (HTTP parameters and files) to a server, and
I haven't found anything useful so far that suited my needs. I also needed that the upload got handled in the
background and in the most efficient way on Android. More than that, I also needed to show upload status in the
Android Notification Center.

So, after some research on the web I found that the best way to do this is to implement an IntentService and notify
status with broadcast intents. This way the logic is decoupled from the UI and it's much more reusable. By using an
IntentService, you can do multiple uploads being sure that they will be performed sequentially, and so you don't
have to deal with the nightmare of concurrency. Also, an IntentService is much more efficient than an AsyncTask, it
gets executed in the background and it's completely detached from the UI thread. If you need to show status in your UI,
read further and you'll discover how to do it very easily.

## Setup
Ensure that you have jcenter in your gradle build file:
```
repositories {
    jcenter()
}
```
then in your dependencies section add:

```
dependencies {
    compile 'com.alexbbb:uploadservice:1.4'
}
```

and do a project sync. To start using the library, you have to initialize it. I suggest you to do that in your Application subclass:
```java
public class Initializer extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // setup the broadcast action namespace string which will be used to notify
        // upload status
        UploadService.NAMESPACE = "com.yourcompany.yourapp";
    }
}
```
and now you're ready to rock!

## How to test upload
You have the following choices:
* Use your own server which handles HTTP/Multipart uploads
* Use one of the server implementations provided in the examples (read below)
* Use the excellent http://www.posttestserver.com/ (bear in mind that the data you post there is public!) for HTTP Multipart

## Examples <a name="examples"></a>
In the <b>examples</b> folder you will find:

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

* A simple Android application that uses this library

## HTTP Multipart upload example
For detailed explanation of each parameter, please check JavaDocs.

```java
public void uploadMultipart(final Context context) {
    final MultipartUploadRequest request = 
                        new MultipartUploadRequest(context,
                                                   "custom-upload-id",
                                                   "http://www.yoursite.com/yourscript");

    /*
     * parameter-name: is the name of the parameter that will contain file's data.
     * Pass "uploaded_file" if you're using the test PHP script
     *
     * custom-file-name.extension: is the file name seen by the server.
     * E.g. value of $_FILES["uploaded_file"]["name"] of the test PHP script
     */
    request.addFileToUpload("/absolute/path/to/your/file",
                            "parameter-name",
                            "custom-file-name.extension",
                            "content-type"));

    //You can add your own custom headers
    request.addHeader("your-custom-header", "your-custom-value");

    //and parameters
    request.addParameter("parameter-name", "parameter-value");

    //If you want to add a parameter with multiple values, you can do the following:
    request.addParameter("array-parameter-name", "value1");
    request.addParameter("array-parameter-name", "value2");
    request.addParameter("array-parameter-name", "valueN");

    //or
    String[] values = new String[] {"value1", "value2", "valueN"};
    request.addArrayParameter("array-parameter-name", values);

    //or
    List<String> valuesList = new ArrayList<String>();
    valuesList.add("value1");
    valuesList.add("value2");
    valuesList.add("valueN");
    request.addArrayParameter("array-parameter-name", valuesList);

    //configure the notification
    request.setNotificationConfig(android.R.drawable.ic_menu_upload,
                                  "notification title",
                                  "upload in progress text",
                                  "upload completed successfully text",
                                  "upload error text",
                                  false);
    
    // set a custom user agent string for the upload request
    // if you comment the following line, the system default user-agent will be used
    request.setCustomUserAgent("UploadServiceDemo/1.0");
 
    // set the intent to perform when the user taps on the upload notification.
    // currently tested only with intents that launches an activity
    // if you comment this line, no action will be performed when the user taps 
    // on the notification
    request.setNotificationClickIntent(new Intent(context, YourActivity.class));
    
    // set the maximum number of automatic upload retries on error
    request.setMaxRetries(2);

    try {
        //Start upload service and display the notification
        request.startUpload();

    } catch (Exception exc) {
        //You will end up here only if you pass an incomplete upload request
        Log.e("AndroidUploadService", exc.getLocalizedMessage(), exc);
    }
}
```
If you want to start uploads or retry them based on the remote server's reachability status, [Android Host Monitor](https://github.com/alexbbb/android-host-monitor) may be useful to you in combination with this library.

## Binary Upload
The binary upload uses a single file as the raw body of the upload request. To test this kind of upload, you can use the provided node.js server implementation in the [examples](#examples).

``` java
public void uploadBinary(final Context context) {
    final BinaryUploadRequest request = 
                        new BinaryUploadRequest(context,
                                                "custom-upload-id",
                                                "http://www.yoursite.com/yourscript");
    
    // you can pass some data as request header, but you should be extremely careful
    request.addHeader("your-custom-header", "your-custom-value");
    
    request.setFileToUpload("/absolute/path/to/your/file");
    
    //configure the notification
    request.setNotificationConfig(android.R.drawable.ic_menu_upload,
                                  "notification title",
                                  "upload in progress text",
                                  "upload completed successfully text",
                                  "upload error text",
                                  false);
                            
    // if you comment the following line, the system default user-agent will be used
    request.setCustomUserAgent("UploadServiceDemo/1.0");
    
    // set the intent to perform when the user taps on the upload notification.
    // currently tested only with intents that launches an activity
    // if you comment this line, no action will be performed when the user taps 
    // on the notification
    request.setNotificationClickIntent(new Intent(context, YourActivity.class));
    
    // set the maximum number of automatic upload retries on error
    request.setMaxRetries(2);
    
    try {
        request.startUpload();
    } catch (Exception exc) {
        //You will end up here only if you pass an incomplete upload request
        Log.e("AndroidUploadService", exc.getLocalizedMessage(), exc);
    }
}
```

## How to monitor upload status
Once the service is started, it publishes the upload status with broadcast intents.
For the sake of simplicity and to not bother you with the writing of a broadcast receiver,
an abstract broadcast receiver has been implemented for you and you just need to extend it and add your custom code.
So to listen for the status of the upload service in an Activity for example, you just need to do the following:

```java
public class YourActivity extends Activity {

    private static final String TAG = "AndroidUploadService";

    ...

    private final AbstractUploadServiceReceiver uploadReceiver =
    new AbstractUploadServiceReceiver() {

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
            Log.i(TAG, "Upload with ID "
                       + uploadId + " uploaded bytes: " + uploadedBytes 
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
                                String serverResponseMessage) {
            Log.i(TAG, "Upload with ID " + uploadId
                       + " has been completed with HTTP " + serverResponseCode
                       + ". Response from server: " + serverResponseMessage);
            
            //If your server responds with a JSON, you can parse it
            //from serverResponseMessage string using a library 
            //such as org.json (embedded in Android) or google's gson
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

If you want to monitor upload status in all of your activities, then just implement the BroadcastReceiver in your base activity class, from which all of your activities inherits and you're done.

## How to stop current upload
Call this method from anywhere you want to stop the currently active upload task.
```java
UploadService.stopCurrentUpload();
```
After that the upload task is cancelled, you will receive a <b>java.net.ProtocolException</b> in your broadcast receiver's <b>onError</b> method and the notification will display the error message that you have set.

## Using HTTPS connection with self-signed certificates
For security reasons, the library doesn't accept self-signed certificates by default when using HTTPS connections, but you can enable them by calling:

```java
AllCertificatesAndHostsTruster.apply();
```

before starting the upload service.

## License

    Copyright (C) 2013-2015 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

