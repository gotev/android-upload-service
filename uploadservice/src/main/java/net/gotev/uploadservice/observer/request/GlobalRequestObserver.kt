package net.gotev.uploadservice.observer.request

import android.app.Application
import net.gotev.uploadservice.data.UploadInfo

class GlobalRequestObserver @JvmOverloads constructor(
    application: Application,
    delegate: RequestObserverDelegate,
    shouldAcceptEventsFrom: (uploadInfo: UploadInfo) -> Boolean = { true }
) : BaseRequestObserver(application, delegate, shouldAcceptEventsFrom) {
    init {
        register()
    }
}
