package co.yml.network.android.engine.network

import androidx.annotation.VisibleForTesting
import co.yml.network.android.engine.network.body.UploadAwareFileRequestBody
import co.yml.network.core.Headers
import co.yml.network.core.Resource
import co.yml.network.core.constants.THREAD_COUNT_NOT_DEFINED
import co.yml.network.core.constants.TIMEOUT_NOT_DEFINED
import co.yml.network.core.engine.network.NetworkEngine
import co.yml.network.core.engine.network.config.NetworkEngineConfiguration
import co.yml.network.core.proxy.ProxyType
import co.yml.network.core.request.BasicRequestBody
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.FileRequestBody
import co.yml.network.core.request.FileTransferProgressCallback
import co.yml.network.core.request.FormRequestBody
import co.yml.network.core.request.Method
import co.yml.network.core.request.MultiPartRequestBody
import co.yml.network.core.response.DataResponse
import co.yml.network.core.response.DataSource
import co.yml.network.core.response.HttpStatusCode
import co.yml.network.core.response.HttpStatusCodeType
import co.yml.network.core.response.NoDataException
import co.yml.network.core.response.StatusCodeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import co.yml.network.core.request.RequestBody as DataRequestBody

private fun setupOkHttpClientBuilderTimeout(builder: OkHttpClient.Builder, timeout: Long) =
    builder.apply {
        if (timeout != TIMEOUT_NOT_DEFINED) {
            connectTimeout(timeout, TimeUnit.MILLISECONDS)
            callTimeout(timeout, TimeUnit.MILLISECONDS)
            readTimeout(timeout, TimeUnit.MILLISECONDS)
            writeTimeout(timeout, TimeUnit.MILLISECONDS)
        }
    }

class AndroidNetworkEngine(
    private val androidTrustManager: AndroidTrustManager? = null
) : NetworkEngine {

    // Instance of the OkHttpClient to make network calls.
    private lateinit var client: OkHttpClient

    private var fileTransferProgressCallback: FileTransferProgressCallback? = null

    override fun init(config: NetworkEngineConfiguration) {
        client = OkHttpClient.Builder().apply {
            followRedirects(false)
            followSslRedirects(false)


            // Check if proxy configurations are provided and setup proxy server accordingly
            config.proxyConfig?.let {

                //Create Proxy server
                val okHttpProxy = Proxy(
                    getProxyType(it.proxyType),
                    InetSocketAddress(it.proxyUrl, it.proxyPort)
                )

                //Pass it to OkHttp configs
                proxy(okHttpProxy)
            }

            androidTrustManager?.let { manager ->
                val trustManager = manager.getTrustManager()
                val socketFactory = manager.getSocketFactory(trustManager)

                sslSocketFactory(socketFactory, trustManager)
            }

            setupOkHttpClientBuilderTimeout(this, config.timeout)

            if (config.threadCount != THREAD_COUNT_NOT_DEFINED) {
                val customDispatcher = Dispatcher()
                customDispatcher.maxRequests = config.threadCount
                dispatcher(customDispatcher)
            }

            config.sslPinningConfig?.let { sslConfig ->
                val certificatePinnerBuilder = CertificatePinner.Builder()
                sslConfig.sslCertificates.entries.forEach { entry ->
                    // Using spread operator (*) to convert a Collection to vararg
                    // Ref: https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs
                    certificatePinnerBuilder.add(entry.key, *entry.value.toTypedArray())
                }
                certificatePinner(certificatePinnerBuilder.build())
            }
        }.build()
        fileTransferProgressCallback = config.fileTransferProgressCallback
    }

    override fun submit(request: DataRequest<String>): Flow<Resource<DataResponse<String>>> = flow {
        val uploadAwareFileRequestBodyList = mutableListOf<UploadAwareFileRequestBody>()
        val networkCall = createNetworkRequest(request, uploadAwareFileRequestBodyList)
        @Suppress("BlockingMethodInNonBlockingContext")
        try {
            val response = networkCall.execute()

            val statusCode = HttpStatusCode.of(response.code)
            val responseBody = response.body?.byteString()?.utf8()
            // For redirection, a server may not send a body, so fallback to a blank data.
                ?: if (statusCode.type == HttpStatusCodeType.REDIRECT) "" else null

            if (statusCode.type == HttpStatusCodeType.CLIENT_ERROR
                || statusCode.type == HttpStatusCodeType.SERVER_ERROR
            ) {
                throw StatusCodeException(statusCode, responseBody)
            }
            val mappedResponse =
                mapResponse(responseBody, response.headers, statusCode) ?: throw NoDataException()

            if (statusCode.type == HttpStatusCodeType.REDIRECT) {
                // For redirection, the data loading is not yet completed, hence the redirection
                // would be considered as Loading state specifying that the data is yet to be load.
                emit(Resource.Loading(mappedResponse))
            } else {
                // Notify all upload aware body that upload request has been succeed.
                uploadAwareFileRequestBodyList.forEach { it.onRequestComplete(isSuccess = true) }
                emit(Resource.Success(mappedResponse))
            }
        } catch (exception: Exception) {
            // Notify all upload aware body that upload request has been failed.
            uploadAwareFileRequestBodyList.forEach { it.onRequestComplete(isSuccess = false) }
            emit(Resource.Error(exception))
        }
    }

    @VisibleForTesting
    internal fun setOkHttpClient(client: OkHttpClient) {
        this.client = client
    }

    private fun createNetworkRequest(
        dataRequest: DataRequest<String>,
        uploadAwareFileRequestBodyList: MutableList<UploadAwareFileRequestBody>
    ): Call {
        val request = Request.Builder().apply {
            url(dataRequest.requestPath.build())
            val requestBody = dataRequest.body?.let {
                createRequestBody(
                    it,
                    dataRequest.fileTransferProgressCallback,
                    uploadAwareFileRequestBodyList
                )
            }
            when (dataRequest.method) {
                Method.POST -> post(
                    requestBody
                        ?: throw Exception("No data to make POST request. Url: ${dataRequest.requestPath}")
                )
                Method.GET -> get()
                Method.HEAD -> head()
                Method.DELETE -> delete(requestBody)
                Method.PUT -> put(
                    requestBody
                        ?: throw Exception("No data to make PUT request. Url: ${dataRequest.requestPath}")
                )
                Method.PATCH -> patch(
                    requestBody
                        ?: throw Exception("No data to make PATCH request. Url: ${dataRequest.requestPath}")
                )
            }
            dataRequest.headers?.forEach { headerName, value -> addHeader(headerName, value) }
        }.build()

        val httpClient =
            if (dataRequest.timeout == TIMEOUT_NOT_DEFINED) client
            else client.newBuilder()
                .apply { setupOkHttpClientBuilderTimeout(this, dataRequest.timeout) }
                .build()
        return httpClient.newCall(request)
    }

    private fun createRequestBody(
        requestBody: DataRequestBody,
        requestFileTransferProgressCallback: FileTransferProgressCallback?,
        uploadAwareFileRequestBodyList: MutableList<UploadAwareFileRequestBody>
    ): RequestBody =
        when (requestBody) {
            is BasicRequestBody<*> -> {
                val data = requestBody.data
                if (data is String) {
                    data.toRequestBody(requestBody.mimeType.toString().toMediaType())
                } else {
                    throw Exception("Data is not parsed to String. Expected data type: String, got ${data?.let { it::class.simpleName }}")
                }
            }
            is FileRequestBody -> {
                val callbackList =
                    listOfNotNull(
                        requestBody.fileTransferProgressCallback,
                        requestFileTransferProgressCallback,
                        fileTransferProgressCallback
                    )

                val uploadAwareFileRequestBody = UploadAwareFileRequestBody(
                    requestBody.filePath,
                    requestBody.filename,
                    requestBody.mimeType?.toString()?.toMediaType(),
                    callbackList
                )
                uploadAwareFileRequestBodyList.add(uploadAwareFileRequestBody)
                uploadAwareFileRequestBody
            }
            is MultiPartRequestBody -> {
                val builder =
                    MultipartBody.Builder().setType(requestBody.type.toString().toMediaType())
                requestBody.parts.forEach { part ->
                    builder.addPart(
                        okhttp3.Headers.Builder().apply {
                            // Add all headers in OkHttp Headers class.
                            part.headers?.forEach { name, value -> add(name, value) }
                        }.build(),
                        createRequestBody(
                            part.body,
                            requestFileTransferProgressCallback,
                            uploadAwareFileRequestBodyList
                        )
                    )
                }
                builder.build()
            }
            is FormRequestBody -> {
                val builder = FormBody.Builder()
                requestBody.entries.forEach {
                    if (it.isEncoded) {
                        builder.addEncoded(it.name, it.value)
                    } else {
                        builder.add(it.name, it.value)
                    }
                }
                builder.build()
            }
        }

    private fun mapResponse(
        responseBody: String?,
        headers: okhttp3.Headers,
        statusCode: HttpStatusCode
    ): DataResponse<String>? =
        if (responseBody != null) DataResponse(
            responseBody,
            Headers().apply { headers.forEach { add(it.first, it.second) } },
            DataSource.Network,
            statusCode
        ) else null

    /**
     * Get Android specific [Proxy.Type] for given local proxyType
     *
     * @param proxyType Local proxy type
     * @return [Proxy.Type] android specific proxy type
     */
    private fun getProxyType(proxyType: ProxyType) =
        when (proxyType) {
            ProxyType.HTTP -> Proxy.Type.HTTP
            ProxyType.DIRECT -> Proxy.Type.HTTP
            ProxyType.SOCKS -> Proxy.Type.HTTP
        }
}
