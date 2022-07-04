package com.accelerator.network.core

import com.accelerator.network.core.config.NetworkManagerConfiguration
import com.accelerator.network.core.constants.HeadersConstants
import com.accelerator.network.core.engine.cache.CacheEngine
import com.accelerator.network.core.engine.network.NetworkEngine
import com.accelerator.network.core.engine.network.config.NetworkEngineConfiguration
import com.accelerator.network.core.interceptors.BasicRedirectionInterceptor
import com.accelerator.network.core.interceptors.RedirectionState
import com.accelerator.network.core.request.BasicRequestBody
import com.accelerator.network.core.request.CachePolicy
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.FileRequestBody
import com.accelerator.network.core.request.FormRequestBody
import com.accelerator.network.core.request.Method
import com.accelerator.network.core.request.MultiPartRequestBody
import com.accelerator.network.core.request.RequestBody
import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.HttpStatusCodeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass
import kotlin.reflect.cast

internal const val EMPTY_JSON_BODY = "{}"

/**
 * Class responsible for managing the networking.
 *
 * NOTE: Please use [NetworkManagerBuilder] to create instance of this class.
 *
 * @constructor creates the NetworkManager and configure it's engines
 * @property configuration for initiating the network manager and it's engine
 */
class NetworkManager internal constructor(private val configuration: NetworkManagerConfiguration) {

    private lateinit var basicRedirectionInterceptor: BasicRedirectionInterceptor
    private var basePath = configuration.basePath

    init {
        configuration.networkEngine.init(
            NetworkEngineConfiguration(
                configuration.timeout,
                configuration.threadCount,
                configuration.sslPinningConfiguration,
                configuration.proxyConfig,
                configuration.fileTransferProgressCallback
            )
        )
    }

    /**
     * Setter for setting [basePath].
     *
     * @param basePath for setting up the base URL path.
     */
    fun setBasePath(basePath: String?) {
        this.basePath = basePath
    }

    /**
     * Submit a data request to the [NetworkEngine] or [CacheEngine] to get a data response.
     *
     * @param requestBuilder a [DataRequest.Builder] containing the request data.
     *
     * @return [NetworkCall] containing the response data.
     */
    fun <RESPONSE : Any> submit(requestBuilder: DataRequest.Builder<RESPONSE>): NetworkCall<RESPONSE> =
        submit(requestBuilder.build())

    /**
     * Submit a data request to the [NetworkEngine] or [CacheEngine] to get a data response.
     *
     * @param request a [DataRequest] containing the request data.
     *
     * @return [NetworkCall] containing the response data.
     */
    fun <RESPONSE : Any> submit(request: DataRequest<RESPONSE>): NetworkCall<RESPONSE> {
        basePath?.let {
            if (request.requestPath.basePath == null) {
                request.requestPath.setBasePath(it)
            }
        }
        var rawRequest = request
        configuration.headers?.let {
            val headers = Headers(request.headers).append(it)
            rawRequest = request.copy(headers = headers)
        }

        val interceptors =
            (configuration.interceptors ?: emptyList()) + (request.interceptors ?: emptyList())
        interceptors.forEach { rawRequest = it.onRawRequest(rawRequest) }

        var parsedRequest = try {
            parseRequestData(rawRequest)
        } catch (exception: Exception) {
            return NetworkCall(flowOf(Resource.Error(exception)))
        }
        interceptors.forEach { parsedRequest = it.onParsedRequest(parsedRequest) }

        val cachePolicy = parsedRequest.cachePolicy ?: configuration.defaultCachePolicy
        val flow = wrapResponseFlow(makeRequest(cachePolicy, parsedRequest)).map { value ->
            var rawResponse = value
            interceptors.forEach { rawResponse = it.onRawResponse(rawResponse, parsedRequest) }

            var mappedValue = try {
                mapResponse(rawResponse, parsedRequest, request.responseClass)
            } catch (exception: Exception) {
                Resource.Error(exception)
            }

            interceptors.forEach { mappedValue = it.onParsedResponse(mappedValue, parsedRequest) }

            return@map mappedValue
        }
        return NetworkCall(flow)
    }

    private fun makeRequest(
        cachePolicy: CachePolicy,
        request: DataRequest<String>
    ): Flow<Resource<DataResponse<String>>> {
        return when (cachePolicy) {
            CachePolicy.NetworkOnly -> makeNetworkRequest(request)

            CachePolicy.CacheOnly -> configuration.cacheEngine?.submit(request)
                ?: throw IllegalArgumentException("${CacheEngine::class.simpleName} is not provided to the ${NetworkManager::class.simpleName}")

            CachePolicy.CacheAndNetworkParallel -> flow {
                configuration.cacheEngine?.submit(request)
                    ?.collect { cacheValue ->
                        emit(
                            // For cache and network in parallel, the cache response would be considered as Loading state.
                            // Only network response would be considered as complete success/error state.
                            when (cacheValue) {
                                is Resource.Success -> Resource.Loading(cacheValue.data)
                                is Resource.Error -> Resource.Loading(error = cacheValue.error)
                                is Resource.Loading -> cacheValue
                            }
                        )
                    }
                makeNetworkRequest(request).collect(::emit)
            }

            CachePolicy.CacheFailsThenNetwork -> configuration.cacheEngine?.let {
                flow {
                    var hasExecutedNetworkRequest = false
                    it.submit(request).collect { cacheValue ->
                        if (cacheValue is Resource.Error) {
                            // In case cache request fails, make network request.
                            if (!hasExecutedNetworkRequest) {
                                hasExecutedNetworkRequest = true
                                makeNetworkRequest(request).collect(::emit)
                            }
                        } else emit(cacheValue)
                    }
                }
            } ?: makeNetworkRequest(request)
        }
    }

    private fun makeNetworkRequest(request: DataRequest<String>): Flow<Resource<DataResponse<String>>> =
        flow {
            configuration.networkEngine.submit(request)
                .collect { resource ->
                    if (resource is Resource.Success) {
                        updateCache(request, resource.data)
                    } else if (resource is Resource.Loading) {
                        resource.data?.let { response ->
                            if (response.statusCode?.type == HttpStatusCodeType.REDIRECT) {
                                handleRedirection(request, response).collect(::emit)
                                return@collect
                            }
                        }
                    }
                    emit(resource)
                }
        }

    /**
     * THIS FUNCTION IS ONLY FOR TESTS USE, DO NOT USE THIS FUNCTION.
     *
     * Set the [BasicRedirectionInterceptor] for tests mocking.
     * @param basicRedirectionInterceptor mock instance of [BasicRedirectionInterceptor].
     */
    internal fun setBasicRedirectionInterceptor(basicRedirectionInterceptor: BasicRedirectionInterceptor) {
        this.basicRedirectionInterceptor = basicRedirectionInterceptor
    }

    /**
     * Wrap the resource flow to emit Loading State first.
     *
     * Emit the loading state for the resource flow first to allow developers to consume loading state.
     * This allow developers to keep all the UI state (Loading, Success, Failure) under one block of code.
     */
    private fun <T> wrapResponseFlow(resourceFlow: Flow<Resource<T>>): Flow<Resource<T>> = flow {
        emit(Resource.Loading())
        resourceFlow.collect { resourceData -> emit(resourceData) }
    }

    @Throws(Exception::class)
    private fun <RESPONSE : Any> parseRequestData(request: DataRequest<RESPONSE>): DataRequest<String> =
        request.cloneWithBody(String::class, parseRequestBody(request.body, request))

    /**
     * Parse/Serialize the request body.
     *
     * @param body which needs to be parsed or serialized.
     * @param request [DataRequest] for which the body needs to be parsed.
     *
     * @throws [Exception] when the parser for specific data is not present or parser failed to parse the data
     *
     * NOTE: For multi-part request, this function would be called recursively,
     *      hence passed the [body] explicitly.
     */
    @Throws(Exception::class)
    private fun parseRequestBody(body: RequestBody?, request: DataRequest<*>): RequestBody? =
        body?.let {
            // Adding all branches of when condition to avoid missing any branch/condition by mistake
            // while adding a new request body.
            when (it) {
                is BasicRequestBody<*> -> {
                    val dataParser = getDataParser(it.mimeType.toString(), request, null)
                    BasicRequestBody(dataParser.serialize(it.data as Any), it.mimeType)
                }
                is FileRequestBody,
                is MultiPartRequestBody,
                is FormRequestBody -> it // No data serialisation is required.
            }
        }

    private fun <RESPONSE : Any> mapResponse(
        resource: Resource<DataResponse<String>>,
        request: DataRequest<String>,
        kClass: KClass<RESPONSE>
    ): Resource<DataResponse<RESPONSE>> = when (resource) {
        is Resource.Success -> Resource.Success(parseResponseData(request, resource.data, kClass))
        is Resource.Error -> Resource.Error(resource.error)
        is Resource.Loading -> Resource.Loading(
            resource.data?.let { parseResponseData(request, it, kClass) },
            resource.error
        )
    }

    private fun <RESPONSE : Any> parseResponseData(
        request: DataRequest<String>,
        response: DataResponse<String>,
        kClass: KClass<RESPONSE>
    ): DataResponse<RESPONSE> {
        val rawBody = response.body
        val parsedBody: RESPONSE = if (rawBody.isNullOrEmpty() || rawBody == EMPTY_JSON_BODY) {
            if (kClass != String::class && kClass != Unit::class) {
                throw IllegalArgumentException("empty body cannot be type casted to ${kClass.simpleName}")
            }
            kClass.cast(rawBody)
        } else {
            if (kClass == String::class || kClass == Unit::class) {
                // There is no need for parsing
                kClass.cast(rawBody)
            } else {
                val contentType = response.headers?.get(HeadersConstants.CONTENT_TYPE)
                    ?: throw IllegalArgumentException("No content type received from Server")
                val dataParser = getDataParser(contentType, request, response)
                dataParser.deserialize(rawBody, kClass)
            }
        }
        return DataResponse(parsedBody, response.headers, response.source, response.statusCode)
    }

    private fun getDataParser(
        contentType: String,
        request: DataRequest<*>,
        response: DataResponse<*>?
    ) = request.dataParser ?: configuration.dataParserFactory.getParser(
        contentType,
        request.requestPath,
        request.headers,
        response?.headers
    )

    private suspend fun updateCache(request: DataRequest<String>, response: DataResponse<String>) {
        if (request.method == Method.GET) {
            configuration.cacheEngine?.submit(
                request.copy(
                    method = Method.PUT,
                    body = BasicRequestBody(response.body),
                    headers = response.headers
                )
            )?.collect()
        } else if (request.method == Method.DELETE) {
            configuration.cacheEngine?.submit(request)?.collect()
        }
    }

    private fun handleRedirection(
        request: DataRequest<String>,
        response: DataResponse<String>
    ): Flow<Resource<DataResponse<String>>> = if (!configuration.shouldFollowRedirect)
        flowOf(Resource.Error(RuntimeException("Got redirection request. Current configuration doesn't support any redirection.")))
    else flow {
        val interceptors =
            (request.interceptors ?: emptyList()) + (configuration.interceptors ?: emptyList())

        var currentOperation: RedirectionState<DataRequest<String>> = RedirectionState.NoOp()
        interceptors.forEach { interceptor ->
            val newOperation = interceptor.onRedirect(request, response)
            if (currentOperation is RedirectionState.NoOp && newOperation !is RedirectionState.NoOp) {
                currentOperation = newOperation
            }
        }

        if (currentOperation is RedirectionState.NoOp) {
            // There were no custom interceptor to handle the redirection.
            // So, perform basic redirection.
            if (!::basicRedirectionInterceptor.isInitialized) {
                basicRedirectionInterceptor = object : BasicRedirectionInterceptor {}
            }
            currentOperation = basicRedirectionInterceptor.onRedirect(request, response)
        }

        when (currentOperation) {
            is RedirectionState.NoOp -> emit(Resource.Error(IllegalStateException("No interceptor was able to handle the redirection request.\nRequest: $request\nResponse: $response")))
            is RedirectionState.Cancel -> emit(Resource.Error(RuntimeException("Redirection Rejected")))
            is RedirectionState.Allowed -> {
                makeNetworkRequest((currentOperation as RedirectionState.Allowed).data)
                    .collect(::emit)
            }
        }
    }
}
