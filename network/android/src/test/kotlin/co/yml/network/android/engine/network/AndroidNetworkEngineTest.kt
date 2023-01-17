package co.yml.network.android.engine.network

import co.yml.network.android.engine.network.body.UploadAwareFileRequestBody
import co.yml.network.android.engine.network.AndroidNetworkEngine
import co.yml.network.core.MimeType
import co.yml.network.core.Resource
import co.yml.network.core.constants.HeadersConstants
import co.yml.network.core.constants.TIMEOUT_NOT_DEFINED
import co.yml.network.core.engine.network.config.NetworkEngineConfiguration
import co.yml.network.core.request.BasicRequestBody
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.FileRequestBody
import co.yml.network.core.request.FormRequestBody
import co.yml.network.core.request.Method
import co.yml.network.core.request.MultiPartRequestBody
import co.yml.network.core.request.RequestBody
import co.yml.network.core.response.DataResponse
import co.yml.network.core.response.HttpStatusCode
import co.yml.network.core.response.NoDataException
import co.yml.network.core.response.StatusCodeException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit

private const val PATH = "https://www.yml.co/users"
private const val FILE_PATH = "/home/file.txt"
private const val ERROR_MESSAGE = "Error Response Received"

class AndroidNetworkEngineTest {

    private lateinit var engine: AndroidNetworkEngine

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        engine = AndroidNetworkEngine()
        engine.init(NetworkEngineConfiguration())
    }

    @Test
    fun verifyBasicGetCall() = runBlocking {
        engine.setOkHttpClient(mockOkhttpClientWithSuccess())
        val getRequestWithBody = createRequestBody()
        val responses = engine.submit(getRequestWithBody).toList()

        assertThat(responses, hasSize(1))
        assertThat(responses[0], instanceOf(Resource.Success::class.java))
        val body = (responses[0] as Resource.Success<DataResponse<String>>).data.body
        assertThat(body, `is`("Response Received"))
    }

    @Test
    fun verifyBasicGetCallWithNoResponseBody() = runBlocking {
        engine.setOkHttpClient(mockOkhttpClientWithSuccess(null))
        val getRequestWithBody = createRequestBody()
        val responses = engine.submit(getRequestWithBody).toList()

        assertThat(responses, hasSize(1))
        assertThat(responses[0], instanceOf(Resource.Error::class.java))
        val exception = (responses[0] as Resource.Error<DataResponse<String>>).error
        assertThat(exception, instanceOf(NoDataException::class.java))
    }

    @Test
    fun verifyEngineWithNoResponse() = runBlocking {
        engine.setOkHttpClient(mockOkhttpClientWithError())
        val getRequestWithBody = createRequestBody()
        val responses = engine.submit(getRequestWithBody).toList()

        assertThat(responses, hasSize(1))
        verifyErrorResponse(responses[0])
    }

    @Test
    fun verifyExceptionForPostWithNoBody() {
        val postRequestWithNullBody = createRequestBody(method = Method.POST, body = null)
        val exception = assertThrows<Exception> {
            runBlocking { engine.submit(postRequestWithNullBody).toList() }
        }

        assertThat(exception.message, `is`("No data to make ${Method.POST} request. Url: $PATH"))
    }

    @Test
    fun verifyExceptionForPutWithNoBody() {
        val putRequestWithNullBody = createRequestBody(method = Method.PUT, body = null)
        val exception = assertThrows<Exception> {
            runBlocking { engine.submit(putRequestWithNullBody).toList() }
        }

        assertThat(exception.message, `is`("No data to make ${Method.PUT} request. Url: $PATH"))
    }

    @Test
    fun verifyExceptionForPatchWithNoBody() {
        val patchRequestWithNullBody = createRequestBody(method = Method.PATCH, body = null)
        val exception = assertThrows<Exception> {
            runBlocking { engine.submit(patchRequestWithNullBody).toList() }
        }

        assertThat(exception.message, `is`("No data to make ${Method.PATCH} request. Url: $PATH"))
    }

    @Test
    fun verifyExceptionForUnitBody() {
        val exception = assertThrows<Exception> {
            runBlocking {
                engine.submit(createRequestBody(body = BasicRequestBody(Unit))).toList()
            }
        }

        assertThat(
            exception.message,
            `is`("Data is not parsed to String. Expected data type: String, got Unit")
        )
    }

    @Test
    fun verifyNetworkRequestExceptionForEmptyBasePath() {
        val getRequestWithoutBasePath = createRequestBody(url = "")
        val exception = assertThrows<IllegalArgumentException> {
            runBlocking { engine.submit(getRequestWithoutBasePath).toList() }
        }
        assertThat(exception.message, `is`("Base path is not specified"))
    }

    @Test
    fun verifyNetworkRequestExceptionForRelativeBasePath() {
        val getRequestWithoutBasePath = createRequestBody(url = "/users")
        val exception = assertThrows<IllegalArgumentException> {
            runBlocking { engine.submit(getRequestWithoutBasePath).toList() }
        }
        assertThat(exception.message, `is`("Base path is not specified"))
    }

    @Test
    fun verifyNetworkRequestWithCustomTimeout(): Unit = runBlocking {
        val client = mockk<OkHttpClient>()
        engine.setOkHttpClient(client)

        val clientBuilder = mockk<OkHttpClient.Builder>()
        every { clientBuilder.connectTimeout(any(), any()) } returns clientBuilder
        every { clientBuilder.callTimeout(any(), any()) } returns clientBuilder
        every { clientBuilder.readTimeout(any(), any()) } returns clientBuilder
        every { clientBuilder.writeTimeout(any(), any()) } returns clientBuilder

        every { clientBuilder.build() } returns mockOkhttpClientWithSuccess()

        every { client.newBuilder() } returns clientBuilder

        val timeoutInMs: Long = 30_000 // 30 Sec
        engine.submit(createRequestBody(timeout = timeoutInMs)).toList()
        verifySequence {
            clientBuilder.connectTimeout(timeoutInMs, TimeUnit.MILLISECONDS)
            clientBuilder.callTimeout(timeoutInMs, TimeUnit.MILLISECONDS)
            clientBuilder.readTimeout(timeoutInMs, TimeUnit.MILLISECONDS)
            clientBuilder.writeTimeout(timeoutInMs, TimeUnit.MILLISECONDS)
            clientBuilder.build()
        }
    }

    @Test
    fun verifyNetworkRequestWithFileBody(): Unit = runBlocking {
        val client = mockk<OkHttpClient>()
        engine.setOkHttpClient(client)

        val capturedRequests = mutableListOf<Request>()

        val call = mockk<Call>()
        every { client.newCall(capture(capturedRequests)) } returns call
        every { call.execute() } returns createSuccessResponse()

        engine.submit(
            createRequestBody(
                method = Method.POST,
                body = FileRequestBody(FILE_PATH)
            )
        ).toList()

        assertThat(capturedRequests, hasSize(1))
        val capturedRequest = capturedRequests[0]
        assertThat(
            capturedRequest.body,
            `is`(UploadAwareFileRequestBody(FILE_PATH, null, null, emptyList()))
        )
        assertThat(capturedRequest.url.toString(), `is`(PATH))
        assertThat(capturedRequest.method, `is`("POST"))
        assertThat(capturedRequest.headers.size, `is`(0))
    }

    @Test
    fun verifyNetworkRequestWithMultipartBody(): Unit = runBlocking {
        val client = mockk<OkHttpClient>()
        engine.setOkHttpClient(client)

        val capturedRequests = mutableListOf<Request>()

        val call = mockk<Call>()
        every { client.newCall(capture(capturedRequests)) } returns call
        every { call.execute() } returns createSuccessResponse()

        val nameFieldValue = "abc"
        val multipartBody = MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM)
            .addFormData("attachment", "file.txt", FileRequestBody(FILE_PATH))
            .addFormData("name", nameFieldValue)
            .build()

        engine.submit(createRequestBody(method = Method.POST, body = multipartBody)).toList()

        assertThat(capturedRequests, hasSize(1))
        val capturedRequest = capturedRequests[0]
        assertThat(capturedRequest.url.toString(), `is`(PATH))
        assertThat(capturedRequest.method, `is`("POST"))
        assertThat(capturedRequest.headers.size, `is`(0))
        assertThat(capturedRequest.body, `is`(instanceOf(MultipartBody::class.java)))

        val requestMultipartBody = capturedRequest.body as MultipartBody
        assertThat(requestMultipartBody.size, `is`(multipartBody.parts.size))
        assertThat(requestMultipartBody.parts.size, `is`(multipartBody.parts.size))

        val part0 = requestMultipartBody.parts[0]
        assertThat(part0.headers, `is`(notNullValue()))
        assertThat(part0.headers?.size, `is`(1))
        assertThat(
            part0.headers?.get(HeadersConstants.CONTENT_DISPOSITION),
            `is`(multipartBody.parts[0].headers?.get(HeadersConstants.CONTENT_DISPOSITION))
        )
        assertThat(part0.body, `is`(UploadAwareFileRequestBody(FILE_PATH, null, null, emptyList())))

        val part1 = requestMultipartBody.parts[1]
        assertThat(part1.headers, `is`(notNullValue()))
        assertThat(part1.headers?.size, `is`(1))
        assertThat(
            part1.headers?.get(HeadersConstants.CONTENT_DISPOSITION),
            `is`(multipartBody.parts[1].headers?.get(HeadersConstants.CONTENT_DISPOSITION))
        )
        assertThat(part1.body, `is`(instanceOf(okhttp3.RequestBody::class.java)))
        assertThat(
            part1.body.contentType(),
            `is`("${MimeType.TEXT_PLAIN}; charset=utf-8".toMediaType())
        )
        assertThat(part1.body.contentLength(), `is`(nameFieldValue.length.toLong()))
    }

    @Test
    fun verifyNetworkRequestWithFormBody(): Unit = runBlocking {
        val client = mockk<OkHttpClient>()
        engine.setOkHttpClient(client)

        val capturedRequests = mutableListOf<Request>()

        val call = mockk<Call>()
        every { client.newCall(capture(capturedRequests)) } returns call
        every { call.execute() } returns createSuccessResponse()

        val body = FormRequestBody.Builder().add("email", "abc@gmail.com").build()

        engine.submit(createRequestBody(method = Method.POST, body = body)).toList()

        assertThat(capturedRequests, hasSize(1))
        val capturedRequest = capturedRequests[0]
        assertThat(capturedRequest.url.toString(), `is`(PATH))
        assertThat(capturedRequest.method, `is`("POST"))
        assertThat(capturedRequest.headers.size, `is`(0))
        assertThat(capturedRequest.body, instanceOf(FormBody::class.java))
    }

    private fun mockOkhttpClientWithSuccess(
        body: ResponseBody? = "Response Received".toResponseBody("application/json".toMediaType())
    ) = mockOkHttpClient(createSuccessResponse(body))

    private fun createSuccessResponse(body: ResponseBody? = "Response Received".toResponseBody("application/json".toMediaType())) = Response.Builder()
        .request(Request.Builder().url(PATH).build())
        .code(200)
        .message("Response Received")
        .protocol(Protocol.HTTP_1_1)
        .body(body)
        .build()

    private fun mockOkhttpClientWithError() = mockOkHttpClient(
        Response.Builder()
            .request(Request.Builder().url(PATH).build())
            .code(HttpStatusCode.NOT_FOUND.code)
            .message("Error Response Received")
            .body(ERROR_MESSAGE.toResponseBody())
            .protocol(Protocol.HTTP_1_1)
            .build()
    )

    private fun mockOkHttpClient(response: Response) = mockk<OkHttpClient>().apply {
        val call = mockk<Call>()
        every { call.execute() } returns response

        every { newCall(any()) } returns call
    }

    private fun createRequestBody(
        url: String = PATH,
        method: Method = Method.GET,
        body: RequestBody? = BasicRequestBody(""),
        timeout: Long = TIMEOUT_NOT_DEFINED
    ): DataRequest<String> =
        DataRequest.Builder(url, method, String::class).setBody(body).setTimeout(timeout).build()

    private fun verifyErrorResponse(result: Resource<DataResponse<String>>) {
        assertThat(result, `is`(instanceOf(Resource.Error::class.java)))
        val exception = (result as Resource.Error<DataResponse<String>>).error
        assertThat(exception, `is`(instanceOf(StatusCodeException::class.java)))

        val statusCodeException = exception as StatusCodeException
        assertThat(statusCodeException.statusCode, `is`(HttpStatusCode.NOT_FOUND))
        assertThat(statusCodeException.errorMessage, `is`(ERROR_MESSAGE))
    }

}
