package co.yml.network.core.engine.cache.demo

import co.yml.network.core.Resource
import co.yml.network.core.common.LinkedList
import co.yml.network.core.engine.cache.cacheErrorMethodNotAllowed
import co.yml.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import co.yml.network.core.engine.cache.CacheEngine
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.Method
import co.yml.network.core.response.DataResponse
import co.yml.network.core.response.HttpStatusCode
import co.yml.network.core.response.StatusCodeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private data class ResponseEntry(
    val response: Flow<Resource<DataResponse<String>>>,
    var remainingTimes: Int
)

class DemoCacheEngine : CacheEngine {

    private val unsupportedMethods =
        Method.values().filter { it != Method.GET && it != Method.PUT && it != Method.DELETE }

    private val responseMap = HashMap<Method, HashMap<String, LinkedList<ResponseEntry>>>()

    /**
     * Initialize the internal variables of the network engine.
     */
    init {
        // Initialize the response map for all types of response methods.
        Method.values().forEach {
            if (!unsupportedMethods.contains(it)) {
                responseMap[it] = HashMap()
            }
        }
    }

    override fun submit(request: DataRequest<String>): Flow<Resource<DataResponse<String>>> {
        if (unsupportedMethods.contains(request.method)) {
            return flowOf(
                Resource.Error(
                    StatusCodeException(
                        HttpStatusCode.METHOD_NOT_ALLOWED,
                        cacheErrorMethodNotAllowed(request.method)
                    )
                )
            )
        }
        try {
            responseMap[request.method]?.let { responseMap ->
                responseMap[request.cacheKey]?.let { responseList ->
                    responseList.first?.let { responseEntry ->
                        responseEntry.remainingTimes--
                        if (responseEntry.remainingTimes <= 0) {
                            responseList.removeFirst()
                        }
                        return responseEntry.response
                    }
                    // There is no first element in the list, i.e. list is empty.
                    // Hence removing the empty list from the map.
                    responseMap.remove(request.cacheKey)
                }
            }
        } catch (exception: NoSuchElementException) {
            // No-OP
        }
        return flowOf(Resource.Error(StatusCodeException(HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND)))
    }

    /**
     * Add a response to handle the request for respective cache key.
     *
     * @param method Http [Method] for which the response needs to be handled.
     * @param cacheKey for which the response needs to be handled.
     * @param response which needs to be sent for the respective [cacheKey] and [method]
     * @param times specifies for how many times the [response] should be sent for respective [cacheKey] and [method].
     *
     * @return [DemoCacheEngine]'s current instance for builder pattern.
     */
    fun on(
        method: Method,
        cacheKey: String,
        response: Resource<DataResponse<String>>,
        times: Int = 1
    ) =
        on(method, cacheKey, flowOf(response), times)


    /**
     * Add a response to handle the request for respective cache key.
     *
     * @param method Http [Method] for which the response needs to be handled.
     * @param cacheKey for which the response needs to be handled.
     * @param response which needs to be sent for the respective [cacheKey] and [method]
     * @param times specifies for how many times the [response] should be sent for respective [cacheKey] and [method].
     *
     * @return [DemoCacheEngine]'s current instance for builder pattern.
     */
    fun on(
        method: Method,
        cacheKey: String,
        response: Flow<Resource<DataResponse<String>>>,
        times: Int = 1
    ): DemoCacheEngine {
        responseMap[method]?.getOrPut(cacheKey) { LinkedList() }
            ?.add(ResponseEntry(response, times))
        return this
    }
}
