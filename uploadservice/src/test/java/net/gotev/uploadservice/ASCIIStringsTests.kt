package net.gotev.uploadservice

import net.gotev.uploadservice.extensions.isASCII
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ASCIIStringsTests {

    @Test
    fun `null string is not ASCII`() {
        val testString: String? = null
        assertFalse(testString.isASCII())
    }

    @Test
    fun `empty string is not ASCII`() {
        val testString = ""
        assertFalse(testString.isASCII())
    }

    @Test
    fun `numeric string is ASCII`() {
        val testString = "1234567890.0123"
        assertTrue(testString.isASCII())
    }

    @Test
    fun `ASCII string`() {
        val testString = "asa83254vhsdfu2385923asoasn2313214aSDDBCBXCB#++"
        assertTrue(testString.isASCII())
    }

    @Test
    fun `not ASCII string`() {
        val testString = "ç°§éàìòù*?"
        assertFalse(testString.isASCII())
    }

    @Test
    fun `not ASCII string 2`() {
        val testString = "ьАСДВВЕЯРТСДцзчьзц сдфсдф зфдсф сд"
        assertFalse(testString.isASCII())
    }

    @Test
    fun `not ASCII string 3`() {
        val testString = "asa83254vhsdfu2385923asoasn2313214aSDDBCBXCB#++💪"
        assertFalse(testString.isASCII())
    }
}
