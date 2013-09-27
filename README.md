Android Upload Service
======================

WARNING!! This project is not stable yet because it's work in progress. So it's not recommended to use it in your own projects until you read this message.

## Installation

Check out the project and add android-upload-service to your project as an [Android Library Project](http://developer.android.com/guide/developing/projects/projects-eclipse.html#ReferencingLibraryProject).

Add the following to your project's AndroidManifest.xml file:

    <service
        android:name="com.alexbbb.uploadservice.UploadService"
        android:enabled="true"
        android:exported="true" >
        <intent-filter>
            <action android:name="com.alexbbb.uploadservice.action.upload"/>
        </intent-filter>
    </service>

## How to start android upload service to upload files
    public void updateSomething(final Context context) {
        //The full URL to your server side HTTP multipart upload script
        final String serverUrl = "http://www.yourcompany.com/your/upload/script";
        
        final ArrayList<FileToUpload> files = new ArrayList<FileToUpload>();
        files.add(new FileToUpload("/absolute/path/to/your/file", 
                                   "http-form-parameter-name", 
                                   "content-type")); //You can find many common content types defined as static constants in the ContentType class
    
        //This is optional. If you don't want to add any specific headers, you can just leave the list empty
        final ArrayList<NameValue> headers = new ArrayList<NameValue>();
        headers.add(new NameValue("additional-header-name", "additional-header-value"));
        
        final ArrayList<NameValue> parameters = new ArrayList<NameValue>();
        parameters.add(new NameValue("parameter-name", "parameter-value"));
        
        //If you want to add an array of strings, you can simply to the following:
        parameters.add(new NameValue("array-parameter-name", "value1"));
        parameters.add(new NameValue("array-parameter-name", "value2"));
        parameters.add(new NameValue("array-parameter-name", "valueN"));
        
        UploadNotificationConfig notificationConfig =
            new UploadNotificationConfig(
                android.R.drawable.ic_menu_upload, //Notification icon. You can use your own app's R.drawable.your_resource
                "notification title", //You can use your string resource with: context.getString(R.string.your_string)
                "upload in progress text",
                "upload completed successfully text",
                "upload error text",
                false); //Set this to true if you want the notification to be automatically cleared when upload is successful
        
        try {
            //Utility method that creates the intent and starts the upload service in the background
            UploadService.startUpload(context, notificationConfig, serverUrl, files, headers, parameters);
        
        } catch (Exception exc) {
            //You will end up here only if you pass null parameters or an invalid server URL
            Log.e("AndroidUploadService", exc.getLocalizedMessage(), exc);
        }
    }

## How to monitor upload status
Once the service is started, it publishes the upload status with broadcast intents. 
For the sake of simplicity and to not bother you with the writing of a broadcast receiver, 
an abstract broadcast receiver has been implemented for you and you just need to extend it and add your custom code.
So to listen for the status of the upload service in an Activity for example, you just need to do the following:

    public class YourActivity extends Activity {
    
        ...
        
        private final BroadcastReceiver uploadReceiver = new AbstractUploadServiceReceiver() {

            @Override
            public void onProgress(int progress) {
                Log.i("AndroidUploadService", "The progress is: " + progress);
            }

            @Override
            public void onError(Exception exception) {
                Log.e("AndroidUploadService", exception.getLocalizedMessage(), exception);
            }

            @Override
            public void onCompleted(int serverResponseCode, String serverResponseMessage) {
                Log.i("AndroidUploadService", "Upload completed: " + serverResponseCode + ", " + serverResponseMessage);
            }
        };
        
        @Override
        protected void onResume() {
            super.onResume();
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UploadService.BROADCAST_ACTION);
            registerReceiver(uploadReceiver, intentFilter);
        }
        
        @Override
        protected void onPause() {
            super.onPause();
            unregisterReceiver(uploadReceiver);
        }
    
    }
    
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
