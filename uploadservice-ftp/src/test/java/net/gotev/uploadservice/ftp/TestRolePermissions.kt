package net.gotev.uploadservice.ftp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TestRolePermissions {

    @Test
    fun `no permissions`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('0')
        assertFalse(permissions.read)
        assertFalse(permissions.write)
        assertFalse(permissions.execute)
    }

    @Test
    fun `no permissions serialization`() {
        val permissions = UnixPermissions.RolePermissions()
        assertEquals("0", permissions.toString())
    }

    @Test
    fun `only execute`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('1')
        assertFalse(permissions.read)
        assertFalse(permissions.write)
        assertTrue(permissions.execute)
    }

    @Test
    fun `only execute serialization`() {
        val permissions = UnixPermissions.RolePermissions(execute = true)
        assertEquals("1", permissions.toString())
    }

    @Test
    fun `only write`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('2')
        assertFalse(permissions.read)
        assertTrue(permissions.write)
        assertFalse(permissions.execute)
    }

    @Test
    fun `only write serialization`() {
        val permissions = UnixPermissions.RolePermissions(write = true)
        assertEquals("2", permissions.toString())
    }

    @Test
    fun `write and execute`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('3')
        assertFalse(permissions.read)
        assertTrue(permissions.write)
        assertTrue(permissions.execute)
    }

    @Test
    fun `write and execute serialization`() {
        val permissions = UnixPermissions.RolePermissions(write = true, execute = true)
        assertEquals("3", permissions.toString())
    }

    @Test
    fun `read only`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('4')
        assertTrue(permissions.read)
        assertFalse(permissions.write)
        assertFalse(permissions.execute)
    }

    @Test
    fun `read only serialization`() {
        val permissions = UnixPermissions.RolePermissions(read = true)
        assertEquals("4", permissions.toString())
    }

    @Test
    fun `read and execute`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('5')
        assertTrue(permissions.read)
        assertFalse(permissions.write)
        assertTrue(permissions.execute)
    }

    @Test
    fun `read and execute serialization`() {
        val permissions = UnixPermissions.RolePermissions(read = true, execute = true)
        assertEquals("5", permissions.toString())
    }

    @Test
    fun `read and write`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('6')
        assertTrue(permissions.read)
        assertTrue(permissions.write)
        assertFalse(permissions.execute)
    }

    @Test
    fun `read and write serialization`() {
        val permissions = UnixPermissions.RolePermissions(read = true, write = true)
        assertEquals("6", permissions.toString())
    }

    @Test
    fun `all permissions`() {
        val permissions = UnixPermissions.RolePermissions.fromChar('7')
        assertTrue(permissions.read)
        assertTrue(permissions.write)
        assertTrue(permissions.execute)
    }

    @Test
    fun `all permissions serialization`() {
        val permissions = UnixPermissions.RolePermissions(read = true, write = true, execute = true)
        assertEquals("7", permissions.toString())
    }

    @Test
    fun `default UnixPermissions`() {
        val permissions = UnixPermissions()
        assertEquals("644", permissions.toString())

        assertTrue(permissions.owner.read)
        assertTrue(permissions.owner.write)
        assertFalse(permissions.owner.execute)

        assertTrue(permissions.group.read)
        assertFalse(permissions.group.write)
        assertFalse(permissions.group.execute)

        assertTrue(permissions.world.read)
        assertFalse(permissions.world.write)
        assertFalse(permissions.world.execute)
    }

    @Test
    fun `permission 754`() {
        val permissions = UnixPermissions("754")
        assertEquals("754", permissions.toString())

        assertTrue(permissions.owner.read)
        assertTrue(permissions.owner.write)
        assertTrue(permissions.owner.execute)

        assertTrue(permissions.group.read)
        assertFalse(permissions.group.write)
        assertTrue(permissions.group.execute)

        assertTrue(permissions.world.read)
        assertFalse(permissions.world.write)
        assertFalse(permissions.world.execute)
    }

    @Test
    fun `invalid string`() {
        try {
            UnixPermissions("7541")
            fail("this should throw an exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun `invalid string 2`() {
        try {
            UnixPermissions("75")
            fail("this should throw an exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun `invalid string 3`() {
        try {
            UnixPermissions("7")
            fail("this should throw an exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun `invalid string 4`() {
        try {
            UnixPermissions("asdmn2")
            fail("this should throw an exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun `invalid string 5`() {
        try {
            UnixPermissions("")
            fail("this should throw an exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }
}
