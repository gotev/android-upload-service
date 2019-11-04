package net.gotev.uploadservice

import net.gotev.uploadservice.extensions.APPLICATION_OCTET_STREAM
import net.gotev.uploadservice.extensions.VIDEO_MP4
import net.gotev.uploadservice.extensions.autoDetectMimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MimeTypeTests {

    @Test
    fun `unknown extension resolves to application octet stream`() {
        assertEquals(APPLICATION_OCTET_STREAM, "some name".autoDetectMimeType())
    }

    @Test
    fun `mp4 file`() {
        assertEquals(VIDEO_MP4, "myfile.mp4".autoDetectMimeType())
    }

    @Test
    fun `jpg file should trigger android specific implementation`() {
        try {
            assertEquals(VIDEO_MP4, "myfile.jpg".autoDetectMimeType())
            fail("This should never be reached")
        } catch (exc: Throwable) {
            assertTrue(exc is RuntimeException)
            assertTrue(exc.message?.startsWith("Method getSingleton in android.webkit.MimeTypeMap not mocked") ?: false)
        }
    }
}
