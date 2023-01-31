package co.yml.network.core.response

/**
 * Enum for [HttpStatusCode] group type.
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status</a>
 */
enum class HttpStatusCodeType {
    INFORMATION,
    SUCCESS,
    REDIRECT,
    CLIENT_ERROR,
    SERVER_ERROR
}

/**
 * Enum for Http response status code.
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status</a>
 *
 * @property code representing integer value of the Status code.
 * @property message information representation of what the Status code meant.
 */
enum class HttpStatusCode(val code: Int, val message: String) {
    // region Success - 2xx status code

    /**
     * The request has succeeded
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/200">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/200</a>
     */
    OK(200, "Ok"),

    /**
     * The request has succeeded and a new resource has been created as a result. This is typically the response sent after POST requests, or some PUT requests.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/201">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/201</a>
     */
    CREATED(201, "Created"),

    /**
     * The request has been received but not yet acted upon. It is noncommittal, since there is no way in HTTP to later send an asynchronous response indicating the outcome of the request. It is intended for cases where another process or server handles the request, or for batch processing.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/202">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/202</a>
     */
    ACCEPTED(202, "Accepted"),

    /**
     * This response code means the returned meta-information is not exactly the same as is available from the origin server, but is collected from a local or a third-party copy. This is mostly used for mirrors or backups of another resource. Except for that specific case, the "200 OK" response is preferred to this status.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/203">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/203</a>
     */
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),

    /**
     * There is no content to send for this request, but the headers may be useful. The user-agent may update its cached headers for this resource with the new ones.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/204">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/204</a>
     */
    NO_CONTENT(204, "No Content"),

    /**
     * Tells the user-agent to reset the document which sent this request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/205">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/205</a>
     */
    RESET_CONTENT(205, "Reset Content"),

    /**
     * This response code is used when the Range header is sent from the client to request only part of a resource.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/206">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/206</a>
     */
    PARTIAL_CONTENT(206, "Partial Content"),

    // endregion

    // region Redirection - 3xx status code

    /**
     * The request has more than one possible response. The user-agent or user should choose one of them. (There is no standardized way of choosing one of the responses, but HTML links to the possibilities are recommended so the user can pick.)
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/300">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/300</a>
     */
    MULTIPLE_CHOICE(300, "Multiple Choice"),

    /**
     * The URL of the requested resource has been changed permanently. The new URL is given in the response.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/301">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/301</a>
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),

    /**
     * This response code means that the URI of requested resource has been changed temporarily. Further changes in the URI might be made in the future. Therefore, this same URI should be used by the client in future requests.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/302">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/302</a>
     */
    FOUND(302, "Found"),

    /**
     * The server sent this response to direct the client to get the requested resource at another URI with a GET request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/303">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/303</a>
     */
    SEE_OTHER(303, "See Other"),

    /**
     * This is used for caching purposes. It tells the client that the response has not been modified, so the client can continue to use the same cached version of the response.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/304">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/304</a>
     */
    NOT_MODIFIED(304, "Not Modified"),

    /**
     * Defined in a previous version of the HTTP specification to indicate that a requested response must be accessed by a proxy. It has been deprecated due to security concerns regarding in-band configuration of a proxy.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/305">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/305</a>
     */
    USE_PROXY(305, "Use Proxy"),

    /**
     * This response code is no longer used; it is just reserved. It was used in a previous version of the HTTP/1.1 specification.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/306">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/306</a>
     */
    UNUSED(306, "unused"),

    /**
     * The server sends this response to direct the client to get the requested resource at another URI with same method that was used in the prior request. This has the same semantics as the 302 Found HTTP response code, with the exception that the user agent must not change the HTTP method used: If a POST was used in the first request, a POST must be used in the second request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/307">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/307</a>
     */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),

    /**
     * This means that the resource is now permanently located at another URI, specified by the Location: HTTP Response header. This has the same semantics as the 301 Moved Permanently HTTP response code, with the exception that the user agent must not change the HTTP method used: If a POST was used in the first request, a POST must be used in the second request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/308">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/308</a>
     */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    // endregion

    // region Client Error - 4xx status code

    /**
     * The server could not understand the request due to invalid syntax.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/400">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/400</a>
     */
    BAD_REQUEST(400, "Bad Request"),

    /**
     * Although the HTTP standard specifies "unauthorized"semantically this response means "unauthenticated". That is, the client must authenticate itself to get the requested response.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/401">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/401</a>
     */
    UNAUTHORIZED(401, "Unauthorized"),

    /**
     * This response code is reserved for future use. The initial aim for creating this code was using it for digital payment systems, however this status code is used very rarely and no standard convention exists.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/402">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/402</a>
     */
    PAYMENT_REQUIRED(402, "Payment Required"),

    /**
     * The client does not have access rights to the content; that is, it is unauthorized, so the server is refusing to give the requested resource. Unlike 401, the client's identity is known to the server.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/403">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/403</a>
     */
    FORBIDDEN(403, "Forbidden"),

    /**
     * The server can not find the requested resource. In the browser, this means the URL is not recognized. In an API, this can also mean that the endpoint is valid but the resource itself does not exist. Servers may also send this response instead of 403 to hide the existence of a resource from an unauthorized client. This response code is probably the most famous one due to its frequent occurrence on the web.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/404">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/404</a>
     */
    NOT_FOUND(404, "Not Found"),

    /**
     * The request method is known by the server but is not supported by the target resource. For example, an API may forbid DELETE-ing a resource.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/405">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/405</a>
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    /**
     * This response is sent when the web server, after performing server-driven content negotiation, doesn't find any content that conforms to the criteria given by the user agent.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/406">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/406</a>
     */
    NOT_ACCEPTABLE(406, "Not Acceptable"),

    /**
     * This is similar to 401 but authentication is needed to be done by a proxy.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/407">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/407</a>
     */
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),

    /**
     * This response is sent on an idle connection by some servers, even without any previous request by the client. It means that the server would like to shut down this unused connection. This response is used much more since some browsers, like Chrome, Firefox 27+, or IE9, use HTTP pre-connection mechanisms to speed up surfing. Also note that some servers merely shut down the connection without sending this message.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/408">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/408</a>
     */
    REQUEST_TIMEOUT(408, "Request Timeout"),

    /**
     * This response is sent when a request conflicts with the current state of the server.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/409">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/409</a>
     */
    CONFLICT(409, "Conflict"),

    /**
     * This response is sent when the requested content has been permanently deleted from server, with no forwarding address. Clients are expected to remove their caches and links to the resource. The HTTP specification intends this status code to be used for "limited-time, promotional services". APIs should not feel compelled to indicate resources that have been deleted with this status code.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/410">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/410</a>
     */
    GONE(410, "Gone"),

    /**
     * Server rejected the request because the Content-Length header field is not defined and the server requires it.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/411">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/411</a>
     */
    LENGTH_REQUIRED(411, "Length Required"),

    /**
     * The client has indicated preconditions in its headers which the server does not meet.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/412">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/412</a>
     */
    PRECONDITION_FAILED(412, "Precondition Failed"),

    /**
     * Request entity is larger than limits defined by server; the server might close the connection or return an Retry-After header field.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/413">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/413</a>
     */
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),

    /**
     * The URI requested by the client is longer than the server is willing to interpret.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/414">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/414</a>
     */
    URI_TOO_LONG(414, "URI Too Long"),

    /**
     * The media format of the requested data is not supported by the server, so the server is rejecting the request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/415">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/415</a>
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    /**
     * The range specified by the Range header field in the request can't be fulfilled; it's possible that the range is outside the size of the target URI's data.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/416">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/416</a>
     */
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),

    /**
     * This response code means the expectation indicated by the Expect request header field can't be met by the server.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/417">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/417</a>
     */
    EXPECTATION_FAILED(417, "Expectation Failed"),

    /**
     * The server refuses the attempt to brew coffee with a teapot.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/418">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/418</a>
     */
    IM_A_TEAPOT(418, "I'm a teapot"),

    /**
     * The request was well-formed but was unable to be followed due to semantic errors.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/422">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/422</a>
     */
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),

    /**
     * Indicates that the server is unwilling to risk processing a request that might be replayed.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/425">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/425</a>
     */
    TOO_EARLY(425, "Too Early"),

    /**
     * The server refuses to perform the request using the current protocol but might be willing to do so after the client upgrades to a different protocol. The server sends an Upgrade header in a 426 response to indicate the required protocol(s).
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/426">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/426</a>
     */
    UPGRADE_REQUIRED(426, "Upgrade Required"),

    /**
     * The origin server requires the request to be conditional. This response is intended to prevent the 'lost update' problem, where a client GETs a resource's state, modifies it, and PUTs it back to the server, when meanwhile a third party has modified the state on the server, leading to a conflict.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/428">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/428</a>
     */
    PRECONDITION_REQUIRED(428, "Precondition Required"),

    /**
     * The user has sent too many requests in a given amount of time ("rate limiting").
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429</a>
     */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    /**
     * The server is unwilling to process the request because its header fields are too large. The request may be resubmitted after reducing the size of the request header fields.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/431">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/431</a>
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),

    /**
     * The user-agent requested a resource that cannot legally be provided, such as a web page censored by a government.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/451">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/451</a>
     */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

    // endregion

    // region Server Error - 5xx status code

    /**
     * The server has encountered a situation it doesn't know how to handle.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/500">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/500</a>
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

    /**
     * The request method is not supported by the server and cannot be handled. The only methods that servers are required to support (and therefore that must not return this code) are GET and HEAD.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/501">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/501</a>
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),

    /**
     * This error response means that the server, while working as a gateway to get a response needed to handle the request, got an invalid response.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/502">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/502</a>
     */
    BAD_GATEWAY(502, "Bad Gateway"),

    /**
     * The server is not ready to handle the request. Common causes are a server that is down for maintenance or that is overloaded. Note that together with this response, a user-friendly page explaining the problem should be sent. This response should be used for temporary conditions and the Retry-After: HTTP header should, if possible, contain the estimated time before the recovery of the service. The webmaster must also take care about the caching-related headers that are sent along with this response, as these temporary condition responses should usually not be cached.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/503">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/503</a>
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),

    /**
     * This error response is given when the server is acting as a gateway and cannot get a response in time.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/504">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/504</a>
     */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),

    /**
     * The HTTP version used in the request is not supported by the server.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/505">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/505</a>
     */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),

    /**
     * The server has an internal configuration error: the chosen variant resource is configured to engage in transparent content negotiation itself, and is therefore not a proper end point in the negotiation process.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/506">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/506</a>
     */
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),

    /**
     * The method could not be performed on the resource because the server is unable to store the representation needed to successfully complete the request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/507">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/507</a>
     */
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),

    /**
     * The server detected an infinite loop while processing the request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/508">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/508</a>
     */
    LOOP_DETECTED(508, "Loop Detected"),

    /**
     * Further extensions to the request are required for the server to fulfill it.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/510">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/510</a>
     */
    NOT_EXTENDED(510, "Not Extended"),

    /**
     * The 511 status code indicates that the client needs to authenticate to gain network access.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/511">https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/511</a>
     */
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

    // endregion

    val type = when (code) {
        in 100..199 -> HttpStatusCodeType.INFORMATION
        in 200..299 -> HttpStatusCodeType.SUCCESS
        in 300..399 -> HttpStatusCodeType.REDIRECT
        in 400..499 -> HttpStatusCodeType.CLIENT_ERROR
        in 500..599 -> HttpStatusCodeType.SERVER_ERROR
        else -> throw IllegalArgumentException("No HTTP category found for $this")
    }

    override fun toString() = "$code - $message"

    companion object {
        fun of(statusCode: Int) = values().find { it.code == statusCode }
            ?: throw NoSuchElementException("Status code $statusCode not found in ${HttpStatusCode::class.simpleName}")

        fun of(statusCode: String) = of(statusCode.toInt())
    }
}
