package net.gotev.uploadservice.testcore

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import org.junit.After
import org.junit.Before

open class UploadServiceTestSuite {

    val appContext: Application
        get() = InstrumentationRegistry.getInstrumentation().context.applicationContext as Application

    val mockWebServer = newSSLMockWebServer()

    @Before
    fun setup() {
        mockWebServer.start(8080)
        UploadServiceConfig.initialize(appContext, appContext.createTestNotificationChannel(), true)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        appContext.deleteTestNotificationChannel()
        UploadService.stop(appContext, true)
    }
}
