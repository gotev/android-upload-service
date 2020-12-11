package net.gotev.uploadservice

import net.gotev.uploadservice.data.HttpUploadTaskParameters
import net.gotev.uploadservice.data.NameValue
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.persistence.PersistableData
import org.junit.Assert.assertEquals
import org.junit.Test

class DataTypesPersistableTests {
    @Test
    fun nameValue() {
        val data = NameValue(name = "key", value = "someval")
        val persistableData = data.toPersistableData()

        assertEquals(data, NameValue.createFromPersistableData(persistableData))
    }

    @Test
    fun uploadFile() {
        val data = UploadFile(
            path = "/path/to/file",
            properties = LinkedHashMap<String, String>().apply {
                put("some prop", "some val")
                put("other prop", "other val")
                put("third", "vals")
            }
        )

        val persistableData = data.toPersistableData()

        assertEquals(data, UploadFile.createFromPersistableData(persistableData))
    }

    @Test
    fun httpUploadTaskParameters() {
        val params = HttpUploadTaskParameters()
        params.requestHeaders.add(NameValue("somename", "someval"))
        params.requestHeaders.add(NameValue("somename2", "someval2"))
        params.requestParameters.add(NameValue("someparam", "someparamval"))
        params.requestParameters.add(NameValue("someparam2", "someparamval2"))
        params.requestParameters.add(NameValue("someparam3", "someparamval3"))

        val data = params.toPersistableData()

        assertEquals(params, HttpUploadTaskParameters.createFromPersistableData(data))
    }

    @Test
    fun uploadTaskParameters() {
        val params = UploadTaskParameters(
            taskClass = "net.gotev.someclass",
            id = "abcd",
            serverUrl = "https://server.url",
            maxRetries = 3,
            autoDeleteSuccessfullyUploadedFiles = false,
            files = arrayListOf(
                UploadFile(
                    path = "/path/to/file",
                    properties = linkedMapOf(
                        "prop" to "val",
                        "otherprop" to "otherval"
                    )
                ),
                UploadFile(
                    path = "/path/to/file/wow",
                    properties = linkedMapOf(
                        "propwow" to "valwow",
                        "otherpropwow" to "othervalwow"
                    )
                )
            ),
            additionalParameters = PersistableData()
        )

        val data = params.toPersistableData()

        assertEquals(params, UploadTaskParameters.createFromPersistableData(data))
    }

    @Test
    fun uploadTaskParameters2() {
        val params = UploadTaskParameters(
            taskClass = "net.gotev.someclass",
            id = "abcd",
            serverUrl = "https://server.url",
            maxRetries = 3,
            autoDeleteSuccessfullyUploadedFiles = false,
            files = arrayListOf(
                UploadFile(
                    path = "/path/to/file",
                    properties = linkedMapOf(
                        "prop" to "val",
                        "otherprop" to "otherval"
                    )
                ),
                UploadFile(
                    path = "/path/to/file/wow",
                    properties = linkedMapOf(
                        "propwow" to "valwow",
                        "otherpropwow" to "othervalwow"
                    )
                )
            ),
            additionalParameters = PersistableData().apply {
                putString("id", "hey")
                putString("maxRetries", "some string")
            }
        )

        val data = params.toPersistableData()

        assertEquals(params, UploadTaskParameters.createFromPersistableData(data))
    }
}
