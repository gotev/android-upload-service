<p align="center">
  <img src="uploadservice-logo.png">
</p>

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Upload%20Service-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2161) [![Build Status](https://travis-ci.org/gotev/android-upload-service.svg?branch=master)](https://travis-ci.org/gotev/android-upload-service) [![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/) [ ![Download](https://api.bintray.com/packages/gotev/maven/android-upload-service/images/download.svg) ](https://bintray.com/gotev/maven/android-upload-service/_latestVersion) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

| :information_source: :new: | [Get started with 4.x](https://github.com/gotev/android-upload-service/wiki/Getting-Started-with-4.x) |
|--|--|
| :book: | [Wiki](https://github.com/gotev/android-upload-service/wiki) |
| :heart: | [Contributing](CONTRIBUTING.md) |
| :star2: | [Features](#features) |
| :raising_hand: | [Who is using Upload Service](#powered) |
| :mega: | [Credits](#credits)
| :scroll: | [License](#license)

![compose-upload](https://user-images.githubusercontent.com/16792495/28752871-de82540e-7529-11e7-9037-de86b8f0ca27.gif)
![upload](https://user-images.githubusercontent.com/16792495/28752872-de9a8894-7529-11e7-823a-e51eda59f5b7.gif)

## Features <a name="features"></a>
* Android 5.0 (API 21) to Android 10 (API 29) support
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
* Possibility to implement your own notification handler
* Lifecycle-Aware RequestObserver to monitor your uploads

At the core of the library there is a `Service` which handles multiple concurrent upload tasks in the background. It publishes broadcast intents to notify status. This way the logic is completely decoupled from the UI. Read further to learn how you can use it in your App.

## Powered by Android Upload Service <a name="powered"></a>
Apps and libraries powered by this library. To be included in the following list, simply create an issue and provide the app name and a link.

- [JIRA Cloud](https://play.google.com/store/apps/details?id=com.atlassian.android.jira.core)
- [Quora](https://play.google.com/store/apps/details?id=com.quora.android)
- [crystal.io](https://play.google.com/store/apps/details?id=net.igenius.crystal)
- [Vydia](https://play.google.com/store/apps/details?id=com.vydia.app)
- [React Native Background Upload](https://github.com/Vydia/react-native-background-upload)
- [Background Upload Plugin for Cordova](https://www.npmjs.com/package/cordova-plugin-background-upload)
- [VoiSmart IP Communicator](https://play.google.com/store/apps/details?id=com.voismart.softphone)
- [NativeScript Background HTTP](https://www.npmjs.com/package/nativescript-background-http)
- [Samajbook](https://play.google.com/store/apps/details?id=com.marothiatechs.samaj)
- [Codeaty](https://play.google.com/store/apps/details?id=com.saifraheem.BagoLearn)

## Credits <a name="credits"></a>
Created my free logo at [LogoMakr.com](https://logomakr.com)

## License <a name="license"></a>

    Copyright (C) 2013-2020 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
