FTP Upload for Android Upload Service
============================================

This module adds FTP upload ([RFC959](https://tools.ietf.org/html/rfc959)) capability to Android Upload Service. It wraps [Apache Commons Net FTP](https://commons.apache.org/proper/commons-net/dependency-info.html) library.

## Setup
```groovy
compile 'net.gotev:uploadservice-ftp:3.2.2'
```

## Minimal example
```java
public void uploadFTP(final Context context) {
    try {
        String uploadId =
          new FTPUploadRequest(context, "my.ftpserver.com", 21)
            .setUsernameAndPassword("ftpuser", "testpassword")
            .addFileToUpload("/absolute/path/to/file", "/remote/path")
            .setNotificationConfig(new UploadNotificationConfig())
            .setMaxRetries(4)
            .startUpload();
    } catch (Exception exc) {
        Log.e("AndroidUploadService", exc.getMessage(), exc);
    }
}
```
Refer to [FTPUploadRequest JavaDoc](http://gotev.github.io/android-upload-service/javadoc-ftp/net/gotev/uploadservice/ftp/FTPUploadRequest.html) for all the available features.

## Test FTP server
If you don't already have an FTP server to make tests, you can use OS X's integrated FTP server if you have a Mac, or you can start a ready to use linux virtual machine based on CentOS 7 + vsftpd, by following the instructions provided below.

First of all, check out `android-upload-service` (you need git. I'm not going to cover how to install that) and navigate to the `test-server` directory:
```
git clone https://github.com/gotev/android-upload-service.git
cd android-upload-service/uploadservice-ftp/test-server
```

### OS X
I'm assuming you've followed the instructions in the previous paragraph and you are already in a terminal in `android-upload-service/uploadservice-ftp/test-server` directory.
```
alex@mbp:~$ cd osx; ./start
```

Enter your password and you should see something like the following:

```
Now type:

ftp localhost

to check that the FTP server is running.
You can login with your system's user credentials
FTP server is listening on the following IP addresses:
127.0.0.1:21
192.168.1.180:21
```

The files will be uploaded in your user's home folder.

To stop the FTP server once you're done:
```
alex@mbp:~$ ./stop
```

### Linux Virtual Machine with vsftpd
I'm assuming you've followed the instructions in the "Test FTP server" paragraph.
You also need to install [Vagrant](https://www.vagrantup.com/), then:
```
alex@mbp:~$ cd linux; vagrant up
```

You will be asked with which physical interface you want to bridge the VM:
```
==> default: Available bridged network interfaces:
1) en0: Wi-Fi (AirPort)
2) en1: Thunderbolt 1
3) en2: Thunderbolt 2
4) p2p0
5) awdl0
==> default: When choosing an interface, it is usually the one that is
==> default: being used to connect to the internet.
    default: Which interface should the network bridge to?
```
Type the number corresponding to your primary network interface (in my case it's 1) and press ENTER. This way the VM will get an IP address on the same LAN on which your computer is connected.

After some waiting and plenty of console output (only the first time), if everything is ok, you should see something like the following:
```
==> default: Finished!
==> default: SSH credentials are
==> default:   Username: vagrant
==> default:   Password: vagrant
==> default: You can also access with: 'vagrant ssh' from your host,
==> default: but you have to be in the same directory where the Vagrantfile is
==> default: You can login with your FTP client with the following credentials:
==> default:   Username: ftpuser
==> default:   Password: testpassword
==> default:   Port: 21 (passive mode)
==> default: On one of the following IP addresses:
==> default: 127.0.0.1
==> default: 10.0.2.15
==> default: 192.168.1.23
==> default: And your files will be uploaded into /home/ftpuser
```

which instructs you how to use the FTP server you just created.

When you're done you can shut it down like this:
```
alex@mbp:~$ vagrant halt
```

To delete the VM:
```
alex@mbp:~$ vagrant destroy
```
