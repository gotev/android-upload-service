## Contents
* [Contributing](#contribute)
* [Issues](#issues)
* [Asking for help and bug reports](#help)

### <a name="contribute"></a> Contributing
* Do you have a new feature in mind?
* Do you know how to improve existing docs or code?
* Have you found a bug?

Contributions are welcome and encouraged! Just fork the project and then send a pull request. Be ready to discuss your code and design decisions.

### <a name="issues"></a> Issues
Before opening a new issue, search the existing ones. If an issue is closed and you have the same problem, either reopen the issue or create a new one, filling in all the details required in the issue template. If you don't provide those data, you may not have a response in return for lack of relevant context.

Bugs are better explained with a test or a demo project. At least with some working code.

When posting code, logs or stack traces, please use the following Markdown syntax to improve readability:
<pre>
```kotlin
your code, log or stack trace
```
</pre>


#### Code Style
Android Upload Service enforces Kotlin standard coding style, using [ktlint](https://ktlint.github.io/).

To speed up development, it's highly recommended to install ktlint and to execute the following in `android-upload-service` root directory:

```
ktlint installGitPreCommitHook
cd examples/app/ && ktlint --android applyToIDEAProject -y && cd ../..
```
If you are using Atlassian SourceTree to commit, you may encounter the following problem with pre commit hook:

```
xargs ktlint: No such file or directory
```

To solve it, open `.git/hooks/pre_commit` with your favourite editor and add the following in the second line:

```bash
export PATH=/usr/local/bin:$PATH
```

#### Development
To develop Android Upload Service and its core modules, clone the project, then open `examples/app/build.gradle` from your Android Studio. In this way you can see all the modules and the demo app, make changes and deploy to your emulator or real device for testing.

**Working on your first Pull Request?** You can learn how from this *free* series [How to Contribute to an Open Source Project on GitHub](https://egghead.io/series/how-to-contribute-to-an-open-source-project-on-github)

### <a name="help"></a> Asking for help and bug reports
If you need help because you can't make a successful upload to your server, first check the [troubleshooting procedure](https://github.com/gotev/android-upload-service/wiki/Troubleshooting-Procedure). 

Bug reports without relevant details will be closed. Time is precious for everybody.

Good and precise bug reports help improve the library fast and make it easier for everybody.

> Complaints are not useful.

[Here](http://coenjacobs.me/2013/12/06/effective-bug-reports-on-github/) there's a nice blog post about effective bug reports.
If you already know how to fix the bug, you can directly send a pull request.
