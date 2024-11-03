<p align="center">
  <img src="uploadservice-logo.png"><br>
  10 years! Since 2013
</p>

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Upload%20Service-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2161) [![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/) ![Maven Central](https://img.shields.io/maven-central/v/net.gotev/uploadservice) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

| :information_source: | [Get started](https://github.com/gotev/android-upload-service/wiki/Getting-Started-with-4.x) |
|--|--|
| :book: | [Check the Wiki](https://github.com/gotev/android-upload-service/wiki) to learn how to use the library and get help |
| :point_right: | Try it out in action, [Get the demo APK](https://github.com/gotev/android-upload-service/releases/latest) |
| :collision: | Not working? Keep calm and follow the [troubleshooting procedure](https://github.com/gotev/android-upload-service/wiki/Troubleshooting-Procedure) |
| :gift: | Find this library useful? Consider sponsoring its development by pressing the Sponsor button on the top right of this page. |
| Still using 3.x ? | It's not maintained or supported. You may have security issues and problems with newer Androids. Switch to 4.x |
| :heart: | [Contributing](CONTRIBUTING.md) |
| :star2: | [Features](#features) |
| :raising_hand: | [Who is using Upload Service](#powered) |
| :mega: | [Credits](#credits)
| :scroll: | [License](#license)

Screencasts from the example app included in this repository:

![compose-upload](https://user-images.githubusercontent.com/16792495/28752871-de82540e-7529-11e7-9037-de86b8f0ca27.gif)
![upload](https://user-images.githubusercontent.com/16792495/28752872-de9a8894-7529-11e7-823a-e51eda59f5b7.gif)

At the core of the library there is a `Service` which handles multiple concurrent upload tasks in the background. It publishes broadcast intents to notify status. This way the logic is completely decoupled from the UI. You are safe launching upload requests from your fragments, activities and services without worrying about locking the thread in which you are. [Check the wiki](https://github.com/gotev/android-upload-service/wiki) to learn how you can use it in your App.

You are also safe if your app is put in the background. All the uploads will continue to be executed also when your device is idle.

Bear in mind that if you kill your app, the service gets killed as well, as it's attached to your app's process and all the currently running uploads will be terminated abruptly.

## Features <a name="features"></a>
* Android 5.0 (API 21) to Android 14 (API 34) support.
  * *Android 13 Note, for apps targeting API 33 or newer*:
    * Due to new behavior changes, you are [required to request POST_NOTIFICATIONS permission at runtime in your app](https://developer.android.com/develop/ui/views/notifications/notification-permission) or else the upload progress won't be shown. To see an example, please look at the BaseActivity in the `examples/app` folder.
  * *Android 12 Note, for apps targeting API 31 or newer*:
    * What's supported: uploads initiated while the app is in foreground, with progress indication notification
    * What's NOT supported: uploads started while the app is in the background or uploads without progress indication notification. This is due to the Service limitations imposed by Google, which requires all background services to display a notification to the user. Current architecture cannot support this. For support of those use-cases, WorkManager is the only option.
* 100% Kotlin and fully interoperable with Java
* upload files to a server with `FTP`, `HTTP multipart/form-data` or `Binary` data requests
* upload requests can be serialized and executed later
* handle multiple concurrent uploads in the background, even if the device is idle (Doze mode)
* automatically retry failed uploads, with a configurable exponential backoff
* possiblity implement other upload protocols as plugins
* possibility to automatically delete uploaded files when the upload is successful
* show status in the Android Notification Center.
* change the underlying HTTP stack. Currently `HttpURLConnection` (the default) and `OkHttp` are supported. You can also implement your own.
* set library log level and provide custom logger implementation
* easily customize the notification with text, icons and actions for the different states
* Possibility to implement your own notification handler
* Lifecycle-Aware RequestObserver to monitor your uploads

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
- [Capacitor Background Upload](https://github.com/Cap-go/capacitor-uploader)

## Credits <a name="credits"></a>
Created my free logo at [LogoMakr.com](https://logomakr.com)

## License <a name="license"></a>

    Copyright (C) 2013-2023 Aleksandar Gotev

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
