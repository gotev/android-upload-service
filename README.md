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

and do a project sync. If you're upgrading to 2.0 from 1.6, read [2.0 migration notes](https://github.com/alexbbb/android-upload-service/releases/tag/2.0).
If you're upgrading to 2.0 from 1.5 or older releases, [read 1.6 migration notes first](https://github.com/alexbbb/android-upload-service/releases/tag/1.6).

[Check the wiki](https://github.com/alexbbb/android-upload-service/wiki) for full setup instructions and examples to get up and running fast. If something isn't covered there, search in the [issues](https://github.com/alexbbb/android-upload-service/issues). Your question could already have been answered in the past. There are also [some checks you can do yourself to resolve common setup mistakes](#help).

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
* [Check the wiki](https://github.com/alexbbb/android-upload-service/wiki), in which you can find how to setup everything and make it work
* [Check JavaDocs](http://alexbbb.github.io/android-upload-service/javadoc/) for full class and methods docs
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
