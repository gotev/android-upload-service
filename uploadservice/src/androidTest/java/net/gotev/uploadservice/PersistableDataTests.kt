package net.gotev.uploadservice

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import net.gotev.uploadservice.persistence.PersistableData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.lang.IllegalArgumentException

class PersistableDataTests {

    private fun assertEqualsBundle(expected: Bundle, actual: Bundle) {
        val expectedKeys = expected.keySet().toList()
        val actualKeys = actual.keySet().toList()
        assertEquals("bundles should contain the same key set", expectedKeys, actualKeys)

        expected.keySet().forEach { key ->
            assertEquals("should contain the same value for key $key", expected[key], actual[key])
        }
    }

    fun Parcelable.toParcel() = Parcel.obtain().also { parcel ->
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
    }

    val data = PersistableData().apply {
        putBoolean("boolKey", true)
        putBoolean("boolKey2", false)
        putDouble("doubleValue", Double.MAX_VALUE)
        putInt("intValue", Int.MAX_VALUE)
        putLong("longValue", Long.MAX_VALUE)
        putString("Str", "somestring")
        putData(
            "nested",
            PersistableData().apply {
                putString("Str", "nestedstr")
                putBoolean("boolKey", false)
            }
        )
    }

    val expectedBundle = Bundle().apply {
        putBoolean("boolKey", true)
        putBoolean("boolKey2", false)
        putDouble("doubleValue", Double.MAX_VALUE)
        putInt("intValue", Int.MAX_VALUE)
        putLong("longValue", Long.MAX_VALUE)
        putString("Str", "somestring")
        putString("nested\$Str", "nestedstr")
        putBoolean("nested\$boolKey", false)
    }

    fun assertGettingData(data: PersistableData) {
        with(data) {
            assertEquals(true, getBoolean("boolKey"))
            assertEquals(false, getBoolean("boolKey2"))
            assertEquals(kotlin.Double.MAX_VALUE, getDouble("doubleValue"), 0.00001)
            assertEquals(kotlin.Int.MAX_VALUE, getInt("intValue"))
            assertEquals(kotlin.Long.MAX_VALUE, getLong("longValue"))
            assertEquals("somestring", getString("Str"))
        }
    }

    @Test
    fun persistableDataGettingValues() {
        assertGettingData(data)
    }

    @Test
    fun persistableDataAsBundle() {
        assertEqualsBundle(expectedBundle, data.toBundle())
    }

    @Test
    fun persistableDataAsParcel() {
        val expectedParcel = expectedBundle.toParcel()
        val actualParcel = data.toBundle().toParcel()

        assertEqualsBundle(expectedParcel.readBundle()!!, actualParcel.readBundle()!!)
    }

    @Test
    fun persistableDataAsParcelAndBackToBundle() {
        val parcel = data.toParcel()
        assertEqualsBundle(expectedBundle, parcel.readBundle()!!)
    }

    @Test
    fun persistableDataAsParcelAndGetValues() {
        val parcel = data.toParcel()
        val newData = PersistableData.createFromParcel(parcel)
        assertGettingData(newData)
    }

    @Test
    fun putInvalidBooleanKey() {
        try {
            PersistableData().apply {
                putBoolean("boolKey", true)
                putBoolean("boolKey$", false)
            }
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun putInvalidIntKey() {
        try {
            PersistableData().apply {
                putInt("key$", 1)
            }
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun putInvalidStringKey() {
        try {
            PersistableData().apply {
                putString("key$", "string")
            }
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun putInvalidLongKey() {
        try {
            PersistableData().apply {
                putLong("key$", 1)
            }
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun putInvalidDoubleKey() {
        try {
            PersistableData().apply {
                putDouble("key$", 1.0)
            }
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun invalidKeyInParcelable() {
        val bundle = Bundle().apply {
            putBoolean("boolKey", true)
            putInt("key$", 1)
        }

        val parcel = bundle.toParcel()
        val data = PersistableData.createFromParcel(parcel)

        assertTrue(data.getBoolean("boolKey"))
        try {
            assertEquals(1, data.getInt("key$"))
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun invalidKeyInJson() {
        val json = JSONObject().apply {
            put("boolKey", true)
            put("key$", 1)
        }.toString()

        val data = PersistableData.fromJson(json)

        assertTrue(data.getBoolean("boolKey"))
        try {
            assertEquals(1, data.getInt("key$"))
            fail("exception should be thrown")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    @Test
    fun testEquality() {
        val data = PersistableData().apply {
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        val data2 = PersistableData().apply {
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        assertEquals(data, data2)
    }

    @Test
    fun testInequality() {
        val data = PersistableData().apply {
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        val data2 = PersistableData().apply {
            putString("Heys", "hoData")
            putInt("23", 231)
        }

        assertNotEquals(data, data2)
    }

    @Test
    fun putDataInData() {
        val nestedData = PersistableData().apply {
            putString("Hey", "ho")
            putInt("23", 23)
        }

        val data = PersistableData().apply {
            putData("nested", nestedData)
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        val expectedBundle = Bundle().apply {
            putString("nested\$Hey", "ho")
            putInt("nested\$23", 23)
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        val extractedData = data.getData("nested")

        assertEquals(nestedData, extractedData)
        assertEqualsBundle(expectedBundle, data.toBundle())
    }

    @Test
    fun putArrayDataInData() {
        val nestedData = PersistableData().apply {
            putString("Hey", "ho")
            putInt("23", 23)
        }

        val nestedData2 = PersistableData().apply {
            putString("Heys", "hos")
            putInt("233", 233)
        }

        val nestedList = listOf(nestedData, nestedData2)

        val data = PersistableData().apply {
            putArrayData("nested", nestedList)
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        val expectedBundle = Bundle().apply {
            putString("nested\$0\$Hey", "ho")
            putInt("nested\$0\$23", 23)
            putString("nested\$1\$Heys", "hos")
            putInt("nested\$1\$233", 233)
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        assertEqualsBundle(expectedBundle, data.toBundle())

        val extractedData = data.getArrayData("nested")
        assertEquals(nestedList, extractedData)
    }

    @Test
    fun getNonExistingKey() {
        val empty = PersistableData()

        try {
            empty.getBoolean("nonexistent")
            fail("should throw exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }

        assertEquals(PersistableData(), empty.getData("nonexistent"))

        assertEquals(emptyList<PersistableData>(), empty.getArrayData("nonexistent"))

        try {
            empty.getString("nonexistent")
            fail("should throw exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }

        try {
            empty.getInt("nonexistent")
            fail("should throw exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }

        try {
            empty.getLong("nonexistent")
            fail("should throw exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }

        try {
            empty.getDouble("nonexistent")
            fail("should throw exception")
        } catch (exc: Throwable) {
            assertTrue(exc is IllegalArgumentException)
        }
    }

    private fun JSONObject.sortedKeys() = ArrayList<String>().also { list ->
        keys().forEach { list.add(it) }
    }.sorted()

    private fun JSONObject.assertEquals(other: JSONObject) {
        val thisKeys = sortedKeys()
        val otherKeys = other.sortedKeys()

        assertEquals(thisKeys, otherKeys)

        thisKeys.forEach { key ->
            assertEquals("value for key $key must be equal", get(key), other.get(key))
        }
    }

    @Test
    fun dataAsJson() {
        val expectedJson = JSONObject().apply {
            put("nested\$0\$Hey", "ho")
            put("nested\$0\$23", 23)
            put("nested\$1\$Heys", "hos")
            put("nested\$1\$233", 233)
            put("Hey", "hoData")
            put("23", 231)
        }

        val nestedData = PersistableData().apply {
            putString("Hey", "ho")
            putInt("23", 23)
        }

        val nestedData2 = PersistableData().apply {
            putString("Heys", "hos")
            putInt("233", 233)
        }

        val nestedList = listOf(nestedData, nestedData2)

        val actualJson = PersistableData().apply {
            putArrayData("nested", nestedList)
            putString("Hey", "hoData")
            putInt("23", 231)
        }.toJson()

        expectedJson.assertEquals(JSONObject(actualJson))
    }

    @Test
    fun dataFromJson() {
        val nestedData = PersistableData().apply {
            putString("Hey", "ho")
            putInt("23", 23)
        }

        val nestedData2 = PersistableData().apply {
            putString("Heys", "hos")
            putInt("233", 233)
        }

        val nestedList = listOf(nestedData, nestedData2)

        val expectedData = PersistableData().apply {
            putArrayData("nested", nestedList)
            putString("Hey", "hoData")
            putInt("23", 231)
        }

        assertEquals(expectedData, PersistableData.fromJson(expectedData.toJson()))
    }
}
