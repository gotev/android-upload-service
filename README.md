Android Upload Service
======================

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Upload%20Service-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2161) [ ![Download](https://api.bintray.com/packages/gotev/maven/android-upload-service/images/download.svg) ](https://bintray.com/gotev/maven/android-upload-service/_latestVersion) [![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alexgotev%40gmail%2ecom&lc=US&item_name=Android%20Upload%20Service&item_number=AndroidUploadService&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted)

![Upload Notification](http://gotev.github.io/android-upload-service/upload.gif)

Easily upload files in the background with automatic Android Notification Center progress indication.

### Purpose
* upload files to a server with HTTP `multipart/form-data` or binary requests
* handle uploads in the background, even if the device is idle
* automatically retry failed uploads, with an exponential backoff
* possibility to automatically delete successfully uploaded files from the device
* show status in the Android Notification Center.

At the core of the library there is a `Service` which handles multiple concurrent upload tasks in the background. It publishes broadcast intents to notify status. This way the logic is completely decoupled from the UI. Read further to learn how you can use it in your App.

### Setup <a name="setup"></a>
#### Maven

```
<dependency>
  <groupId>net.gotev</groupId>
  <artifactId>uploadservice</artifactId>
  <version>2.0.1</version>
  <type>aar</type>
</dependency>
```

#### Gradle

```
dependencies {
    compile 'net.gotev:uploadservice:2.0.1@aar'
}
```

and do a project sync. [Read this page](https://github.com/gotev/android-upload-service/wiki/Setup) if you're upgrading from a previous release and for setup troubleshooting.

[Check the wiki](https://github.com/gotev/android-upload-service/wiki) for full setup instructions and examples to get up and running fast. If something isn't covered there, search in the [issues](https://github.com/gotev/android-upload-service/issues). Your question could already have been answered in the past. There are also [some checks you can do yourself to resolve common setup mistakes](https://github.com/gotev/android-upload-service/wiki/Resolve%20common%20setup%20issues).

<a name="new-issues"></a>
> When you post a new issue regarding a possible bug in the library, make sure to add as many details as possible to be able to reproduce the error you encountered. Thank you :)

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
