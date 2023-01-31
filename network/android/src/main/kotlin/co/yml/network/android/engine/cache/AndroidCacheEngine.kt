package co.yml.network.android.engine.cache

import androidx.annotation.VisibleForTesting
import co.yml.network.android.engine.cache.disklrucache.DiskLruCache
import co.yml.network.android.engine.cache.disklrucache.headersToString
import co.yml.network.android.engine.cache.disklrucache.streamToHeaders
import co.yml.network.core.Headers
import co.yml.network.core.Resource
import co.yml.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import co.yml.network.core.engine.cache.CACHE_ERROR_NOT_IMPLEMENTED
import co.yml.network.core.engine.cache.CacheEngine
import co.yml.network.core.engine.cache.cacheErrorMethodNotAllowed
import co.yml.network.core.request.BasicRequestBody
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.Method
import co.yml.network.core.response.DataResponse
import co.yml.network.core.response.DataSource
import co.yml.network.core.response.HttpStatusCode
import co.yml.network.core.response.StatusCodeException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.io.IOException

/**
 * The cache value is stored as an Entry and each Entry can have array of value with it.
 * Here, this valueCount indicates that with each entry, we are going to store 2 values (i.e. header and body).
 * At 0th index of entry, we will store Header data. Also, at 1st index of entry, we will store body data.
 */
private const val VALUE_COUNT = 2
private const val HEADER_DATA_INDEX = 0
private const val BODY_DATA_INDEX = 1

/**
 * @constructor creates the Android CacheEngine which add data and fetch data from cache
 *
 * @param cacheSize the maximum number of bytes this cache should use to store.
 * @param directory a writable directory.
 * @param appVersion the version number that will be written in the journal
 *
 * @property mDiskLruCache responsible for reading , writing and deleting cache entry from disk
 */
class AndroidCacheEngine(cacheSize: Long, directory: File, appVersion: Int) : CacheEngine {

    constructor(cacheKey: Long, filePath: String, appVersion: Int)
            : this(cacheKey, File(filePath), appVersion)

    @VisibleForTesting
    internal var mDiskLruCache: DiskLruCache? = try {
        DiskLruCache.open(directory, appVersion, VALUE_COUNT, cacheSize)
    } catch (e: IOException) {
        null
    }

    /**
     * Submit  data  to the [DiskLruCache] tp perform read write operations.
     *
     * @param request a [DataRequest] containing the request data.
     *
     * @return [Resource] containing the response data.
     */
    override fun submit(request: DataRequest<String>) =
        request.cacheKey?.let { key ->
            flow {
                try {
                    when (request.method) {
                        Method.GET -> emit(
                            get(key)
                                ?: Resource.Error(
                                    StatusCodeException(
                                        HttpStatusCode.NOT_FOUND,
                                        CACHE_ERROR_NOT_FOUND
                                    )
                                )
                        )
                        Method.PUT -> emit(
                            put(key, request)
                        )
                        Method.DELETE -> {
                            if (remove(key)) {
                                emit(createSuccessResponse(null, null))
                            } else {
                                emit(
                                    Resource.Error(
                                        StatusCodeException(
                                            HttpStatusCode.NOT_FOUND,
                                            CACHE_ERROR_NOT_FOUND
                                        )
                                    )
                                )
                            }
                        }
                        else -> emit(
                            Resource.Error(
                                StatusCodeException(
                                    HttpStatusCode.METHOD_NOT_ALLOWED,
                                    cacheErrorMethodNotAllowed(request.method)
                                )
                            )
                        )
                    }
                } catch (exception: Exception) {
                    emit(Resource.Error(exception))
                }
            }
        } ?: flowOf(
            Resource.Error(StatusCodeException(HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND))
        )

    private fun get(key: String) = mDiskLruCache?.get(key)?.use {
        val headers = streamToHeaders(it.getInputStream(HEADER_DATA_INDEX))
        val value = it.getString(BODY_DATA_INDEX)
        return@use createSuccessResponse(value, headers)
    }

    private fun put(
        cacheKey: String,
        request: DataRequest<String>
    ): Resource<DataResponse<String>> {
        val body = request.body
        if (body is BasicRequestBody<*>) {
            val data = body.data
            if (data is String) {
                mDiskLruCache?.let {
                    it.edit(cacheKey)?.apply {
                        try {
                            set(HEADER_DATA_INDEX, headersToString(request.headers))
                            set(BODY_DATA_INDEX, data)
                            commit()
                        } catch (exception: Throwable) {
                            abort()
                            throw exception
                        }
                    }
                    it.flush()
                }
                return createSuccessResponse(null, null)
            }
        }
        return Resource.Error(
            StatusCodeException(HttpStatusCode.NOT_IMPLEMENTED, CACHE_ERROR_NOT_IMPLEMENTED)
        )
    }

    private fun remove(key: String) = mDiskLruCache?.let {
        val isRemoveSuccess = it.remove(key)
        it.flush()
        return isRemoveSuccess
    } ?: false

    private fun createSuccessResponse(body: String?, headers: Headers?) =
        Resource.Success(DataResponse(body, headers, DataSource.Cache, HttpStatusCode.OK))

    @VisibleForTesting
    internal fun setDiskLruCache(diskLruCache: DiskLruCache?) {
        mDiskLruCache = diskLruCache
    }

}
