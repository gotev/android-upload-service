Android Upload Service
======================

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Upload%20Service-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2161) [![Build Status](https://travis-ci.org/gotev/android-upload-service.svg?branch=master)](https://travis-ci.org/gotev/android-upload-service) [ ![Download](https://api.bintray.com/packages/gotev/maven/android-upload-service/images/download.svg) ](https://bintray.com/gotev/maven/android-upload-service/_latestVersion) [![Javadocs](http://javadoc.io/badge/net.gotev/uploadservice.svg)](http://javadoc.io/doc/net.gotev/uploadservice) <a href="http://www.methodscount.com/?lib=net.gotev%3Auploadservice%3A3.4.1"><img src="https://img.shields.io/badge/Methods and size-core: 664 | deps: 18388 | 93 KB-e91e63.svg"/></a>
</a>

### If you're already using the library, upgrading to 3.4.2 is strongly recommended for security reasons! [Read here](https://github.com/gotev/android-upload-service/releases/tag/3.4

![compose-upload](https://user-images.githubusercontent.com/16792495/28752871-de82540e-7529-11e7-9037-de86b8f0ca27.gif)
![upload](https://user-images.githubusercontent.com/16792495/28752872-de9a8894-7529-11e7-823a-e51eda59f5b7.gif)

| :information_source: | [Get started!](https://github.com/gotev/android-upload-service/wiki/Setup) |
|--|--|
| :iphone: | [Download demo APK](https://github.com/gotev/android-upload-service/releases/download/3.4.2/uploadservice-demo-debug.apk)|

## Features
* tiny library
* upload files to a server with `FTP`, `HTTP multipart/form-data` or binary requests
* be able to easily implement other upload protocols as plugins
* handle multiple concurrent uploads in the background, even if the device is idle (Doze mode)
* automatically retry failed uploads, with a configurable exponential backoff
* possibility to automatically delete uploaded files when the upload is successful
* show status in the Android Notification Center (with support for [stacking notifications](http://developer.android.com/training/wearables/notifications/stacks.html)).
* be able to change the underlying HTTP stack. Currently `HttpURLConnection` (the default) and `OkHttp` are supported. You can also implement your own.
* be able to set library log level and to provide custom logger implementation
* easily customize the notification with text, icons and actions for the different states

At the core of the library there is a `Service` which handles multiple concurrent upload tasks in the background. It publishes broadcast intents to notify status. This way the logic is completely decoupled from the UI. Read further to learn how you can use it in your App.

## Powered by Android Upload Service <a name="powered"></a>
Apps and libraries powered by this library. To be included in the following list, simply create an issue and provide the app name and a link.

- [JIRA Cloud](https://play.google.com/store/apps/details?id=com.atlassian.android.jira.core)
- [Quora](https://play.google.com/store/apps/details?id=com.quora.android)
- [crystal.io](https://play.google.com/store/apps/details?id=net.igenius.crystal)
- [Fliiby](https://play.google.com/store/apps/details?id=flii.by.app)
- [Vydia](https://play.google.com/store/apps/details?id=com.vydia.app)
- [React Native Background Upload](https://github.com/Vydia/react-native-background-upload)
- [Background Upload Plugin for Cordova](https://www.npmjs.com/package/cordova-plugin-background-upload)
- [VoiSmart IP Communicator](https://play.google.com/store/apps/details?id=com.voismart.softphone)
- [Motolife](https://play.google.com/store/apps/details?id=bg.motolife.app)
- [NativeScript Background HTTP](https://www.npmjs.com/package/nativescript-background-http)
- [MyCyberLaw](https://play.google.com/store/apps/details?id=com.mycyberlaw)
- [Samajbook](https://play.google.com/store/apps/details?id=com.marothiatechs.samaj)

## Contributing <a name="contribute"></a>
* Do you have a new feature in mind?
* Do you know how to improve existing docs or code?
* Have you found a bug?

Contributions are welcome and encouraged! Just fork the project and then send a pull request. Be ready to discuss your code and design decisions.

## Do you like the project? <a name="donate"></a>
Put a star, spread the word and if you want to offer me a free beer, [![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alexgotev%40gmail%2ecom&lc=US&item_name=Android%20Upload%20Service&item_number=AndroidUploadService&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

## License <a name="license"></a>

    Copyright (C) 2013-2018 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
