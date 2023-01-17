package co.accelerator.network.core.request

import co.yml.network.core.Headers
import co.yml.network.core.MimeType
import co.yml.network.core.constants.HeadersConstants
import co.yml.network.core.request.BasicRequestBody
import co.yml.network.core.request.FileRequestBody
import co.yml.network.core.request.MultiPartRequestBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MultipartRequestBodyTest {

    @Test
    fun verifyAddPartWithHeader() {
        val dataRequestBody = BasicRequestBody("data")
        // Verify multipart request build without any Headers (null Headers)
        var body =
            MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addPart(dataRequestBody).build()
        assertThat(body.parts, hasSize(1))

        // Verify multipart request build with empty Headers
        val headers = Headers()
        body =
            MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addPart(dataRequestBody, headers)
                .build()
        assertThat(body.parts, hasSize(1))

        // Verify multipart request build with some Headers
        headers.add(HeadersConstants.ACCEPT, MimeType.JSON.toString())
        headers.add(HeadersConstants.LOCATION, "https://website.com/")
        body =
            MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addPart(dataRequestBody, headers)
                .build()
        assertThat(body.parts, hasSize(1))

        // Verify multipart request build failure for blocked Headers
        val blockedHeaders1 =
            Headers(headers).add(HeadersConstants.CONTENT_TYPE, MimeType.JSON.toString())
        val exception1 = assertThrows<IllegalArgumentException> {
            MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM)
                .addPart(dataRequestBody, blockedHeaders1).build()
        }
        assertThat(exception1.message, `is`("Unexpected header: Content-Type"))

        // Verify multipart request build failure for blocked Headers
        val blockedHeaders2 =
            Headers(headers).add(HeadersConstants.CONTENT_LENGTH, 2048.toString())
        val exception2 = assertThrows<IllegalArgumentException> {
            MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM)
                .addPart(dataRequestBody, blockedHeaders2).build()
        }
        assertThat(exception2.message, `is`("Unexpected header: Content-Length"))
    }

    @Test
    fun verifyAddFormData() {
        val fieldName = "name"
        val fieldValue = "value"
        val body =
            MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addFormData(fieldName, fieldValue)
                .build()
        assertThat(body.parts, hasSize(1))
        val headers = body.parts[0].headers
        val requestBody = body.parts[0].body

        assertThat(
            headers?.get(HeadersConstants.CONTENT_DISPOSITION),
            `is`("form-data; name=\"$fieldName\"")
        )

        assertThat(requestBody, `is`(instanceOf(BasicRequestBody::class.java)))
        val basicRequestBody = requestBody as BasicRequestBody<*>
        assertThat(basicRequestBody.data, `is`(fieldValue))
        assertThat(basicRequestBody.mimeType, `is`(MimeType.TEXT_PLAIN))
    }

    @Test
    fun verifyAddFileFormData() {
        val fieldName = "name"
        val fileName = "file.txt"
        val fileBody = FileRequestBody("/home/file.txt")
        val body = MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addFormData(fieldName, fileName, fileBody).build()
        assertThat(body.parts, hasSize(1))
        val headers = body.parts[0].headers
        val requestBody = body.parts[0].body

        assertThat(
            headers?.get(HeadersConstants.CONTENT_DISPOSITION),
            `is`("form-data; name=\"$fieldName\"; filename=\"$fileName\"")
        )
        assertThat(requestBody, `is`(fileBody))
    }

    @Test
    fun verifyAddFileFormDataWithMultilineFileName() {
        val fieldName = "name"
        val fileName = "file \n name \n \"quoted\".txt"
        val fileBody = FileRequestBody("/home/file.txt", fileName, MimeType.TEXT_PLAIN)
        val body = MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addFormData(fieldName, fileName, fileBody).build()
        assertThat(body.type, `is`(MimeType.MULTIPART_FORM))
        assertThat(body.parts, hasSize(1))
        val headers = body.parts[0].headers
        val requestBody = body.parts[0].body

        assertThat(
            headers?.get(HeadersConstants.CONTENT_DISPOSITION),
            `is`("form-data; name=\"$fieldName\"; filename=\"file %0A name %0A %22quoted%22.txt\"")
        )
        assertThat(requestBody, `is`(fileBody))
    }

    @Test
    fun verifyAddFileFormDataWithMultilineFileNameWithReturnCarriage() {
        val fieldName = "name"
        val fileName = "file \r\n name \r\n \"quoted\".txt"
        val fileBody = FileRequestBody("/home/file.txt")
        val body = MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM).addFormData(fieldName, fileName, fileBody).build()
        assertThat(body.parts, hasSize(1))
        val headers = body.parts[0].headers
        val requestBody = body.parts[0].body

        assertThat(
            headers?.get(HeadersConstants.CONTENT_DISPOSITION),
            `is`("form-data; name=\"$fieldName\"; filename=\"file %0D%0A name %0D%0A %22quoted%22.txt\"")
        )
        assertThat(requestBody, `is`(fileBody))
    }
}