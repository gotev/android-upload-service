Android Upload Service
======================

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

Check out the project and add android-upload-service to your project as an [Android Library Project](http://developer.android.com/guide/developing/projects/projects-eclipse.html#ReferencingLibraryProject).

Add the following to your project's AndroidManifest.xml file:


    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

And before the tag:

    </ application>

add the following (by changing <b>com.yourcompany.yourapp</b> to your custom app namespace):

    <service
        android:name="com.alexbbb.uploadservice.UploadService"
        android:enabled="true"
        android:exported="false" >
        <intent-filter>
            <action android:name="com.yourcompany.yourapp.uploadservice.action.upload"/>
        </intent-filter>
    </service>

In your application's initialization code (for example in the onCreate method of your android.app.Application subclass), add:

    UploadService.NAMESPACE = "com.yourcompany.yourapp";


## Examples
In the <b>examples</b> folder you will find:

* a demo server-side php script that handles multipart form upload
* a simple demo application that uses this library

To be able to compile and deploy the demo application, you also need to have <b>appcompat_v7</b> library. You may need to change the path to that library in the demo application's properties.

## How to start android upload service to upload files
For detailed explanation of each parameter, please check JavaDocs.

    public void upload(final Context context) {
        final UploadRequest request = new UploadRequest(context,
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
                                      "upload completed successfully text"
                                      "upload error text",
                                      false);

        try {
            //Start upload service and display the notification
            UploadService.startUpload(request);

        } catch (Exception exc) {
            //You will end up here only if you pass an incomplete UploadRequest
            Log.e("AndroidUploadService", exc.getLocalizedMessage(), exc);
        }
    }

## How to monitor upload status
Once the service is started, it publishes the upload status with broadcast intents.
For the sake of simplicity and to not bother you with the writing of a broadcast receiver,
an abstract broadcast receiver has been implemented for you and you just need to extend it and add your custom code.
So to listen for the status of the upload service in an Activity for example, you just need to do the following:

    public class YourActivity extends Activity {

        private static final String TAG = "AndroidUploadService";

        ...

        private final AbstractUploadServiceReceiver uploadReceiver =
        new AbstractUploadServiceReceiver() {

            @Override
            public void onProgress(String uploadId, int progress) {
                Log.i(TAG, "The progress of the upload with ID "
                           + uploadId + " is: " + progress);
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
                           + " is completed: " + serverResponseCode
                           + ", " + serverResponseMessage);
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

If you want to monitor upload status in all of your activities, then just implement the BroadcastReceiver in your base activity class, from which all of your activities inherits and you're done.

## Using HTTPS connection with self-signed certificates
For security reasons, the library doesn't accept self-signed certificates by default when using HTTPS connections, but you can enable them by calling:

    AllCertificatesAndHostsTruster.apply();

before starting the upload service.

## Do you use Android Upload Service in your project?
Let me know, and I'll be glad to include a link in the following list :)

- [VoiSmart IP Communicator](https://play.google.com/store/apps/details?id=com.voismart.softphone)

## License

    Copyright (C) 2013 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
