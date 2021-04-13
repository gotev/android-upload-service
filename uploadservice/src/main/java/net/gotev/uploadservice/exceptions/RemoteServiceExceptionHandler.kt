package net.gotev.uploadservice.exceptions

import net.gotev.uploadservice.logger.UploadServiceLogger

class RemoteServiceExceptionHandler(private val defaultExceptionHandler: Thread.UncaughtExceptionHandler?) :
    Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        when {
            exception.message?.contains("Context.startForegroundService() did not then call") == true -> {
                UploadServiceLogger.error(
                    component = "AndroidSystem",
                    uploadId = "N/A",
                    exception = BuggedAndroidServiceAPIException(exception),
                    message = {
                        "CRASH PREVENTED. You have started an upload while your app " +
                            "was in the background on API 26+.\nAndroid system was unable to call " +
                            "the service's onStartCommand in less than the 5 seconds time limit. " +
                            "It's a known Android bug which is not going to be fixed.\n" +
                            "More details here: https://issuetracker.google.com/issues/76112072"
                    }
                )
            }

            defaultExceptionHandler != null -> {
                defaultExceptionHandler.uncaughtException(thread, exception)
            }

            else -> {
                throw exception
            }
        }
    }
}
