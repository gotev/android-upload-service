package net.gotev.uploadservice

import net.gotev.uploadservice.extensions.isValidHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HTTPURLTests {
    @Test
    fun `valid http url`() {
        assertTrue("http://alex.gotev.net".isValidHttpUrl())
    }

    @Test
    fun `valid https url`() {
        assertTrue("https://gotev.net".isValidHttpUrl())
    }

    @Test
    fun `invalid https url`() {
        assertFalse("https:/gotev.net".isValidHttpUrl())
    }

    @Test
    fun `invalid http url`() {
        assertFalse("http:/gotev.net".isValidHttpUrl())
    }

    @Test
    fun `invalid http url 2`() {
        assertFalse("htp://gotev.net/something".isValidHttpUrl())
    }

    @Test
    fun `missing protocol url`() {
        assertFalse("gotev.net".isValidHttpUrl())
    }
}
