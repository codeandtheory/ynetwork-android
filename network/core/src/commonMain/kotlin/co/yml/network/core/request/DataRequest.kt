package co.yml.network.core.request

import co.yml.network.core.Headers
import co.yml.network.core.constants.TIMEOUT_NOT_DEFINED
import co.yml.network.core.interceptors.Interceptor
import co.yml.network.core.parser.DataParser
import kotlin.reflect.KClass

/**
 * Class holding request data.
 *
 * NOTE: Please use [DataRequest.Builder] for creating the instance.
 *
 * @property requestPath full path with relative url and query parameters if specified
 * @property method request methods to indicate the action to be performed for a given request data
 * @property responseClass Response class cast to type with data parser
 * @property body data body to be sent to the server
 * @property headers  HTTP header that can be used in an HTTP request
 * @property cachePolicy type of cache mechanism to fetch data
 * @property cacheKey key that identifies the object in cache
 * @property dataParser parsing configs to parse data
 * @property timeout time for request timeout
 * @property interceptors list of interceptor implemented
 * @property fileTransferProgressCallback Callback for providing file upload progress for files present in the body of this request.
 */
data class DataRequest<RESPONSE : Any> internal constructor(
    val requestPath: RequestPath,
    val method: Method,
    val responseClass: KClass<RESPONSE>,
    val body: RequestBody?,
    val headers: Headers?,
    val cachePolicy: CachePolicy?,
    val cacheKey: String?,
    val dataParser: DataParser?,
    val timeout: Long,
    val interceptors: List<Interceptor>?,
    val fileTransferProgressCallback: FileTransferProgressCallback?
) {

    /**
     * This function is a replica of [copy] function, but the existing [copy] function cannot change the generics type.
     *
     * @param responseClass Response class cast to type with data parser
     * @param body data body to be sent to the server
     * @return a copy of [DataRequest] with the passed argument data.
     */
    fun <RES : Any> cloneWithBody(
        responseClass: KClass<RES>,
        body: RequestBody?
    ): DataRequest<RES> = DataRequest(
        requestPath,
        method,
        responseClass,
        body,
        headers,
        cachePolicy,
        cacheKey,
        dataParser,
        timeout,
        interceptors,
        fileTransferProgressCallback
    )

    class Builder<RESPONSE : Any>(
        private val requestPath: RequestPath,
        private val method: Method,
        private val responseClass: KClass<RESPONSE>
    ) {
        private var body: RequestBody? = null
        private var cacheKey: String? = null
        private var cachePolicy: CachePolicy? = null
        private var dataParser: DataParser? = null
        private var fileTransferProgressCallback: FileTransferProgressCallback? = null
        private var headers: Headers? = null
        private var interceptors: List<Interceptor>? = null
        private var timeout: Long = TIMEOUT_NOT_DEFINED

        constructor(url: String, method: Method, responseClass: KClass<RESPONSE>)
                : this(RequestPath(url), method, responseClass)

        /**
         * Set the provided [body] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setBody(body: RequestBody?) = apply { this.body = body }

        /**
         * Set the provided [cacheKey] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setCacheKey(cacheKey: String?) = apply { this.cacheKey = cacheKey }

        /**
         * Set the provided [cachePolicy] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setCachePolicy(cachePolicy: CachePolicy?) = apply { this.cachePolicy = cachePolicy }

        /**
         * Set the provided [dataParser] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setDataParser(dataParser: DataParser?) = apply { this.dataParser = dataParser }

        /**
         * Set the provided [fileTransferProgressCallback] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setFileTransferProgressCallback(fileTransferProgressCallback: FileTransferProgressCallback?) =
            apply { this.fileTransferProgressCallback = fileTransferProgressCallback }

        /**
         * Set the provided [headers] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setHeaders(headers: Headers?) = apply { this.headers = headers }

        /**
         * Set the provided [interceptors] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setInterceptors(interceptors: List<Interceptor>?) =
            apply { this.interceptors = interceptors }

        /**
         * Set the provided [timeout] to the [Builder] instance.
         * @return [Builder]'s current instance for builder pattern.
         */
        fun setTimeout(timeout: Long) = apply { this.timeout = timeout }

        /**
         * Build and return the [DataRequest] instance
         * @return [DataRequest]'s current instance
         */
        fun build() = DataRequest(
            requestPath,
            method,
            responseClass,
            body,
            headers,
            cachePolicy,
            cacheKey,
            dataParser,
            timeout,
            interceptors,
            fileTransferProgressCallback
        )
    }

    companion object {
        /**
         * Utility function for making [Method.GET] request.
         *
         * @param url String url on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> get(url: String, responseClass: KClass<RESPONSE>) =
            Builder(url, Method.GET, responseClass)

        /**
         * Utility function for making [Method.GET] request.
         *
         * @param requestPath url path on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> get(requestPath: RequestPath, responseClass: KClass<RESPONSE>) =
            Builder(requestPath, Method.GET, responseClass)

        /**
         * Utility function for making [Method.POST] request.
         *
         * @param url String url on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @param body containing the request data body.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> post(
            url: String,
            responseClass: KClass<RESPONSE>,
            body: RequestBody?
        ) = Builder(url, Method.POST, responseClass).setBody(body)

        /**
         * Utility function for making [Method.POST] request.
         *
         * @param requestPath url path on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @param body containing the request data body.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> post(
            requestPath: RequestPath,
            responseClass: KClass<RESPONSE>,
            body: RequestBody?
        ) = Builder(requestPath, Method.POST, responseClass).setBody(body)

        /**
         * Utility function for making [Method.PATCH] request.
         *
         * @param url String url on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @param body containing the request data body.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> patch(
            url: String,
            responseClass: KClass<RESPONSE>,
            body: RequestBody?
        ) = Builder(url, Method.PATCH, responseClass).setBody(body)

        /**
         * Utility function for making [Method.PATCH] request.
         *
         * @param requestPath url path on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @param body containing the request data body.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> patch(
            requestPath: RequestPath,
            responseClass: KClass<RESPONSE>,
            body: RequestBody?
        ) =
            Builder(requestPath, Method.PATCH, responseClass).setBody(body)

        /**
         * Utility function for making [Method.HEAD] request.
         *
         * @param url String url on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> head(url: String, responseClass: KClass<RESPONSE>) =
            Builder(url, Method.HEAD, responseClass)

        /**
         * Utility function for making [Method.HEAD] request.
         *
         * @param requestPath url path on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> head(requestPath: RequestPath, responseClass: KClass<RESPONSE>) =
            Builder(requestPath, Method.HEAD, responseClass)

        /**
         * Utility function for making [Method.PUT] request.
         *
         * @param url String url on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @param body containing the request data body.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> put(url: String, responseClass: KClass<RESPONSE>, body: RequestBody?) =
            Builder(url, Method.PUT, responseClass).setBody(body)

        /**
         * Utility function for making [Method.PUT] request.
         *
         * @param requestPath url path on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @param body containing the request data body.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> put(
            requestPath: RequestPath,
            responseClass: KClass<RESPONSE>,
            body: RequestBody?
        ) = Builder(requestPath, Method.PUT, responseClass).setBody(body)

        /**
         * Utility function for making [Method.DELETE] request.
         *
         * @param url String url on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> delete(url: String, responseClass: KClass<RESPONSE>) =
            Builder(url, Method.DELETE, responseClass)

        /**
         * Utility function for making [Method.DELETE] request.
         *
         * @param requestPath url path on which the request needs to be made.
         * @param responseClass Type of the response expected.
         * @return [Builder]'s instance containing the request data.
         */
        fun <RESPONSE : Any> delete(requestPath: RequestPath, responseClass: KClass<RESPONSE>) =
            Builder(requestPath, Method.DELETE, responseClass)
    }
}
