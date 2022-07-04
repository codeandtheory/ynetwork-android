package com.accelerator.network.core.engine.network.demo

import com.accelerator.network.core.Resource
import com.accelerator.network.core.common.LinkedList
import com.accelerator.network.core.common.TreeMap
import com.accelerator.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import com.accelerator.network.core.engine.network.NetworkEngine
import com.accelerator.network.core.engine.network.config.NetworkEngineConfiguration
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.FileRequestBody
import com.accelerator.network.core.request.FileTransferInfo
import com.accelerator.network.core.request.FileTransferProgressCallback
import com.accelerator.network.core.request.Method
import com.accelerator.network.core.request.MultiPartRequestBody
import com.accelerator.network.core.request.RequestBody
import com.accelerator.network.core.request.RequestPath
import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.HttpStatusCode
import com.accelerator.network.core.response.StatusCodeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private data class ResponseEntry(
    val response: Flow<Resource<DataResponse<String>>>,
    var remainingTimes: Int
)

class FileTransferProgressCallbackHelper(val callbacks: MutableList<FileTransferProgressCallback> = mutableListOf()) {

    fun emit(fileInfo: FileTransferInfo, transferredBytesResource: Resource<Long>) {
        callbacks.forEach { it(fileInfo, transferredBytesResource) }
    }
}

class DemoNetworkEngine : NetworkEngine {

    private var basePath: String? = null
    private var networkManagerFileTransferProgressCallback: FileTransferProgressCallback? = null

    private var comparator = Comparator<RequestPath> { requestPath1, requestPath2 ->
        val pathResult = requestPath1.buildPathWithoutParams()
            .compareTo(requestPath2.buildPathWithoutParams())
        if (pathResult != 0) {
            return@Comparator pathResult
        }

        val req1Params = requestPath1.queryParams
        val req2Params = requestPath2.queryParams
        val paramResult = req1Params.size.compareTo(req2Params.size)
        if (paramResult != 0) {
            return@Comparator paramResult
        }

        val unCommonParam2 = mutableListOf<Pair<String, String>>()
        unCommonParam2.addAll(req2Params)
        val unCommonParam1 = req1Params.filter {
            if (unCommonParam2.contains(it)) {
                unCommonParam2.remove(it)
                return@filter false
            }
            return@filter true
        }

        if (unCommonParam1.isEmpty()) {
            return@Comparator 0
        }
        unCommonParam1.forEachIndexed { index, param1Item ->
            val param2Item = unCommonParam2.getOrNull(index) ?: return@Comparator 1
            val keyCompareResult = param1Item.first.compareTo(param2Item.first)
            if (keyCompareResult != 0) {
                return@Comparator keyCompareResult
            }
            val valueCompareResult = param1Item.second.compareTo(param2Item.second)
            if (valueCompareResult != 0) {
                return@Comparator valueCompareResult
            }
        }
        return@Comparator 0
    }

    private val responseMap = HashMap<Method, TreeMap<RequestPath, LinkedList<ResponseEntry>>>()
    private val fileResponseMap = HashMap<String, FileTransferProgressCallbackHelper>()

    /**
     * Initialize the internal variables of the network engine.
     */
    init {
        val requestPathComparator =
            Comparator<RequestPath> { path1, path2 -> comparator.compare(path1, path2) }

        // Initialize the response map for all types of response methods.
        Method.values().forEach { responseMap[it] = TreeMap(requestPathComparator) }
    }

    override fun init(config: NetworkEngineConfiguration) {
        networkManagerFileTransferProgressCallback = config.fileTransferProgressCallback
    }

    fun setBasePath(basePath: String) = apply {
        this.basePath = basePath
    }

    /**
     * Reset the response map.
     *
     * This method could be used to reset the mocks after tests execution.
     */
    fun reset() {
        responseMap.clear()
        fileResponseMap.clear()
    }

    override fun submit(request: DataRequest<String>): Flow<Resource<DataResponse<String>>> {
        request.body?.let { handleRequestBody(it, request.fileTransferProgressCallback) }

        setupBasePath(request.requestPath)
        val list = responseMap[request.method]?.get(request.requestPath)
        if (list != null) {
            try {
                val first = list.first
                if (first != null) {
                    first.remainingTimes--
                    if (first.remainingTimes <= 0) {
                        list.removeFirst()
                    }
                    return first.response
                }
            } catch (exception: NoSuchElementException) {
                // No-OP
            }
            // There is no first element in the list, i.e. list is empty.
            // Hence removing the empty list from the map.
            responseMap[request.method]?.remove(request.requestPath)
        }
        return flowOf(Resource.Error(StatusCodeException(HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND)))
    }

    /**
     * Add a response to handle the request for respective request path.
     *
     * @param method Http [Method] for which the response needs to be handled.
     * @param requestPath [String] URL for which the response needs to be handled.
     * @param response which needs to be sent for the respective [requestPath] and [method]
     * @param times specifies for how many times the [response] should be sent for respective [requestPath] and [method].
     *
     * @return [DemoNetworkEngine]'s current instance for builder pattern.
     */
    fun on(
        method: Method,
        requestPath: String,
        response: Resource<DataResponse<String>>,
        times: Int = 1
    ) = on(method, RequestPath(requestPath), response, times)

    /**
     * Add a response to handle the request for respective request path.
     *
     * @param method Http [Method] for which the response needs to be handled.
     * @param requestPath [RequestPath] for which the response needs to be handled.
     * @param response which needs to be sent for the respective [requestPath] and [method]
     * @param times specifies for how many times the [response] should be sent for respective [requestPath] and [method].
     *
     * @return [DemoNetworkEngine]'s current instance for builder pattern.
     */
    fun on(
        method: Method,
        requestPath: RequestPath,
        response: Resource<DataResponse<String>>,
        times: Int = 1
    ) = on(method, requestPath, flowOf(response), times)

    /**
     * Add a response to handle the request for respective request path.
     *
     * @param method Http [Method] for which the response needs to be handled.
     * @param requestPath [String] URL for which the response needs to be handled.
     * @param response which needs to be sent for the respective [requestPath] and [method]
     * @param times specifies for how many times the [response] should be sent for respective [requestPath] and [method].
     *
     * @return [DemoNetworkEngine]'s current instance for builder pattern.
     */
    fun on(
        method: Method,
        requestPath: String,
        response: Flow<Resource<DataResponse<String>>>,
        times: Int = 1
    ): DemoNetworkEngine = on(method, RequestPath(requestPath), response, times)

    /**
     * Add a response to handle the request for respective request path.
     *
     * @param method Http [Method] for which the response needs to be handled.
     * @param requestPath [RequestPath] for which the response needs to be handled.
     * @param response which needs to be sent for the respective [requestPath] and [method]
     * @param times specifies for how many times the [response] should be sent for respective [requestPath] and [method].
     *
     * @return [DemoNetworkEngine]'s current instance for builder pattern.
     */
    fun on(
        method: Method,
        requestPath: RequestPath,
        response: Flow<Resource<DataResponse<String>>>,
        times: Int = 1
    ): DemoNetworkEngine = apply {
        setupBasePath(requestPath)
        responseMap[method]?.getOrPut(requestPath) { LinkedList() }
            ?.add(ResponseEntry(response, times))
    }

    /**
     * Add a mock for file progress response.
     *
     * @param filePath File path for mocking information regarding the file
     * @param callbackHelper Helper to trigger callback data.
     *
     * @return [DemoNetworkEngine]'s current instance for builder pattern.
     */
    fun onFile(
        filePath: String,
        callbackHelper: FileTransferProgressCallbackHelper
    ): DemoNetworkEngine = apply {
        fileResponseMap[filePath] = callbackHelper
    }

    /**
     * Set the comparator of [RequestPath]. This comparator is used internally to map the request
     * path and response data correctly.
     * This comparator allow the developers to compare the [RequestPath] properly to adapt the
     * [DemoNetworkEngine] with their use case.
     *
     * E.g.: In case for a specific API vendor, the developers needs to send the timestamp as a query param.
     * Now due to this, the [RequestPath] generated during setup and [RequestPath] generated during
     * functionality execution would be considered as different since they may have different timestamp value.
     * To avoid such issue, developer can send their custom comparator which ignores the timestamp query param
     * while comparing the [RequestPath].
     *
     * @param comparator [Comparator<RequestPath>] for comparing [RequestPath]
     *
     * @return [DemoNetworkEngine]'s current instance for builder pattern.
     */
    fun setRequestPathComparator(comparator: Comparator<RequestPath>): DemoNetworkEngine {
        this.comparator = comparator
        return this;
    }

    private fun setupBasePath(requestPath: RequestPath) {
        basePath?.let {
            if (requestPath.basePath == null) {
                requestPath.setBasePath(it)
            }
        }
    }

    private fun handleRequestBody(
        requestBody: RequestBody?,
        requestFileTransferProgressCallback: FileTransferProgressCallback?
    ) {
        if (requestBody is FileRequestBody) {
            val callbackList = listOfNotNull(
                requestBody.fileTransferProgressCallback,
                requestFileTransferProgressCallback,
                networkManagerFileTransferProgressCallback
            )
            if (callbackList.isEmpty()) {
                return
            }
            val mockedResponse = fileResponseMap[requestBody.filePath]
            if (mockedResponse != null) {
                mockedResponse.callbacks.addAll(callbackList)
            } else {
                callbackList.forEach {
                    it(
                        FileTransferInfo(
                            requestBody.filePath,
                            requestBody.filename,
                            requestBody.mimeType,
                            true,
                            0
                        ),
                        Resource.Error(RuntimeException("No mocked callback for file $requestBody"))
                    )
                }
            }
        } else if (requestBody is MultiPartRequestBody) {
            requestBody.parts.forEach {
                handleRequestBody(
                    it.body,
                    requestFileTransferProgressCallback
                )
            }
        }
    }
}
