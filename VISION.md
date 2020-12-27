# Android Upload Service Vision
This document has the purpose of illustrating the vision behind this library and help you know more about the direction in which is going.

## Goals
- Provide an easy way of uploading files on all different Android versions
- Make the upload service core as easy, performant and lightweight as possible, using latest technologies
- The core should not depend on anything more than androidx libraries and kotlin runtime
- Decouple library and protocol implementation from HTTP Stacks
- Allow to implement integrations with third party services using standard protocols, without having to depend on
proprietary or third party libraries
- Always support latest Android version
- Comply with Google best practices and recommendations
- Comply with latest security recommendations
- Use Kotlin best practices

## I wrote an integration with a third party service. And now?
If your integration is using third party dependencies (e.g. FTP plugin uses Apache Commons Net dependency) then your plugin is not suitable 
for integration in the main codebase, but you retain full authorship of your work and you're free to publish it on your own.

This is made on purpose to avoid turning this library into a massive monorepo with a bazillion of different third party dependencies 
to keep updated, which is really heavy to maintain.

Every author of something new which is an integration to a third party service is responsible for its maintenance, support and evolution.

Get in touch by opening an issue, describing what you've done and from when it can be downloaded.
Your work will be featured in the wiki with direct links.

If your third party integration makes some modifications to upload service core, you are encouraged to make a PR with only those modifications, while
keeping the rest of your work in your repo.

If your integration depends solely on Upload Service core, all your implementations have been done using the `HttpStack` abstraction and you have automatic tests
as well, then you can make a PR requesting it to be included in the main codebase. After a code review and approval, you will be the one responsible for the
support and maintenance of the newly added integration. You can obtail write access as a contributor as well.

## Things which are not going to happen
- Rewrite in Java. Starting from version 4.x the library has been completely rewritten in Kotlin. It has 100% interoperability 
with Java, but this is the language with which further development will be made.
- Porting for Android API < 21. Starting from 4.x, all the previous support for APIs 18, 19 and 20 has been dropped. The main motivation is security.
Android API < 21 does not guarantee TLS 1.2 and everything is left to single vendors. Moreover, minimum hardware device available for testing is an old
Android 5.0. If you or your company are intereseted in such support, feel free to fork the project, add it yourself and maintain it.

## This document is incomplete
As any document on earth, this document is incomplete. If you have an idea and you can't figure out if it complies with the vision by reading this file, 
get in touch and let's discuss about it :wink:
