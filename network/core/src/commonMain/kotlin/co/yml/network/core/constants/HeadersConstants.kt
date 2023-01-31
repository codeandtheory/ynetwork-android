package co.yml.network.core.constants

object HeadersConstants {

    // region Caching
    /**
     * The time, in seconds, that the object has been in a proxy cache.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Age">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Age</a>
     */
    const val AGE = "Age"

    /**
     * Directives for caching mechanisms in both requests and responses.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control</a>
     */
    const val CACHE_CONTROL = "Cache-Control"

    /**
     * Clears browsing data (e.g. cookies, storage, cache) associated with the requesting website.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Clear-Site-Data">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Clear-Site-Data</a>
     */
    const val CLEAR_SITE_DATA = "Clear-Site-Data"

    /**
     * The date/time after which the response is considered stale.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expires">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expires</a>
     */
    const val EXPIRES = "Expires"

    /**
     * Implementation-specific header that may have various effects anywhere along the request-response chain. Used for backwards compatibility with HTTP/1.0 caches where the Cache-Control header is not yet present.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Pragma">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Pragma</a>
     */
    const val PRAGMA = "Pragma"

    /**
     * General warning information about possible problems.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Warning">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Warning</a>
     */
    const val WARNING = "Warning"
    // endregion

    // region Conditionals
    /**
     * The last modification date of the resource, used to compare several versions of the same resource. It is less accurate than ETag, but easier to calculate in some environments. Conditional requests using If-Modified-Since and If-Unmodified-Since use this value to change the behavior of the request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified</a>
     */
    const val LAST_MODIFIED = "Last-Modified"

    /**
     * A unique string identifying the version of the resource. Conditional requests using If-Match and If-None-Match use this value to change the behavior of the request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag</a>
     */
    const val ETAG = "ETag"

    /**
     * Makes the request conditional, and applies the method only if the stored resource matches one of the given ETags.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Match">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Match</a>
     */
    const val IF_MATCH = "If-Match"

    /**
     * Makes the request conditional, and applies the method only if the stored resource doesn't match any of the given ETags. This is used to update caches (for safe requests), or to prevent to upload a new resource when one already exists.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-None-Match">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-None-Match</a>
     */
    const val IF_NONE_MATCH = "If-None-Match"

    /**
     * Makes the request conditional, and expects the resource to be transmitted only if it has been modified after the given date. This is used to transmit data only when the cache is out of date.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Modified-Since">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Modified-Since</a>
     */
    const val IF_MODIFIED_SINCE = "If-Modified-Since"

    /**
     * Makes the request conditional, and expects the resource to be transmitted only if it has not been modified after the given date. This ensures the coherence of a new fragment of a specific range with previous ones, or to implement an optimistic concurrency control system when modifying existing documents.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Unmodified-Since">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Unmodified-Since</a>
     */
    const val IF_UNMODIFIED_SINCE = "If-Unmodified-Since"

    /**
     * Determines how to match request headers to decide whether a cached response can be used rather than requesting a fresh one from the origin server.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Vary">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Vary</a>
     */
    const val VARY = "Vary"
    // endregion

    // region Connection management
    /**
     * Controls whether the network connection stays open after the current transaction finishes.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Connection">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Connection</a>
     */
    const val CONNECTION = "Connection"

    /**
     * Controls how long a persistent connection should stay open.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive</a>
     */
    const val KEEP_ALIVE = "Keep-Alive"
    // endregion

    // region Content negotiation
    /**
     * Informs the server about the types of data that can be sent back.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept</a>
     */
    const val ACCEPT = "Accept"

    /**
     * The encoding algorithm, usually a compression algorithm, that can be used on the resource sent back.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Encoding">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Encoding</a>
     */
    const val ACCEPT_ENCODING = "Accept-Encoding"

    /**
     * Informs the server about the human language the server is expected to send back. This is a hint and is not necessarily under the full control of the user: the server should always pay attention not to override an explicit user choice (like selecting a language from a dropdown).
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language</a>
     */
    const val ACCEPT_LANGUAGE = "Accept-Language"
    // endregion

    // region Cookies
    /**
     * Contains stored HTTP cookies previously sent by the server with the Set-Cookie header.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie</a>
     */
    const val COOKIE = "Cookie"

    /**
     * Send cookies from the server to the user-agent.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie</a>
     */
    const val SET_COOKIE = "Set-Cookie"
    // endregion

    // region CORS
    /**
     * Indicates whether the response can be shared.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Origin</a>
     */
    const val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"

    /**
     * Indicates whether the response to the request can be exposed when the credentials flag is true.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Credentials</a>
     */
    const val ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"

    /**
     * Used in response to a preflight request to indicate which HTTP headers can be used when making the actual request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Headers</a>
     */
    const val ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"

    /**
     * Specifies the methods allowed when accessing the resource in response to a preflight request.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Methods">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Allow-Methods</a>
     */
    const val ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"

    /**
     * Indicates which headers can be exposed as part of the response by listing their names.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Expose-Headers</a>
     */
    const val ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers"

    /**
     * Indicates how long the results of a preflight request can be cached.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Max-Age</a>
     */
    const val ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age"

    /**
     * Used when issuing a preflight request to let the server know which HTTP headers will be used when the actual request is made.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Headers">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Headers</a>
     */
    const val ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers"

    /**
     * Used when issuing a preflight request to let the server know which HTTP method will be used when the actual request is made.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Method">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Access-Control-Request-Method</a>
     */
    const val ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method"

    /**
     * Indicates where a fetch originates from.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin</a>
     */
    const val ORIGIN = "Origin"

    /**
     * Specifies origins that are allowed to see values of attributes retrieved via features of the Resource Timing API, which would otherwise be reported as zero due to cross-origin restrictions.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Timing-Allow-Origin">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Timing-Allow-Origin</a>
     */
    const val TIMING_ALLOW_ORIGIN = "Timing-Allow-Origin"
    // endregion

    // region Downloads
    /**
     * Indicates if the resource transmitted should be displayed inline (default behavior without the header), or if it should be handled like a download and the browser should present a “Save As” dialog.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition</a>
     */
    const val CONTENT_DISPOSITION = "Content-Disposition"
    // endregion

    // region Message body information
    /**
     * The size of the resource, in decimal number of bytes.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Length">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Length</a>
     */
    const val CONTENT_LENGTH = "Content-Length"

    /**
     * Indicates the media type of the resource.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type</a>
     */
    const val CONTENT_TYPE = "Content-Type"

    /**
     * Used to specify the compression algorithm.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding</a>
     */
    const val CONTENT_ENCODING = "Content-Encoding"

    /**
     * Describes the human language(s) intended for the audience, so that it allows a user to differentiate according to the users' own preferred language.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Language">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Language</a>
     */
    const val CONTENT_LANGUAGE = "Content-Language"

    /**
     * Indicates an alternate location for the returned data.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Location">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Location</a>
     */
    const val CONTENT_LOCATION = "Content-Location"
    // endregion

    // region Redirects
    /**
     * Indicates the URL to redirect a page to.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Location">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Location</a>
     */
    const val LOCATION = "Location"
    // endregion

    // region Request context
    /**
     * Contains an Internet email address for a human user who controls the requesting user agent.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/From">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/From</a>
     */
    const val FROM = "From"

    /**
     * Specifies the domain name of the server (for virtual hosting), and (optionally) the TCP port number on which the server is listening.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Host">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Host</a>
     */
    const val HOST = "Host"

    /**
     * The address of the previous web page from which a link to the currently requested page was followed.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer</a>
     */
    const val REFERER = "Referer"

    /**
     * Governs which referrer information sent in the Referer header should be included with requests made.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy</a>
     */
    const val REFERRER_POLICY = "Referrer-Policy"

    /**
     * Contains a characteristic string that allows the network protocol peers to identify the application type, operating system, software vendor or software version of the requesting software user agent. See also the Firefox user agent string reference.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent</a>
     */
    const val USER_AGENT = "User-Agent"
    // endregion

    // region Range requests
    /**
     * Indicates if the server supports range requests, and if so in which unit the range can be expressed.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges</a>
     */
    const val ACCEPT_RANGES = "Accept-Ranges"

    /**
     * Indicates the part of a document that the server should return.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range</a>
     */
    const val RANGE = "Range"

    /**
     * Creates a conditional range request that is only fulfilled if the given etag or date matches the remote resource. Used to prevent downloading two ranges from incompatible version of the resource.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Range">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Range</a>
     */
    const val IF_RANGE = "If-Range"

    /**
     * Indicates where in a full body message a partial message belongs.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range</a>
     */
    const val CONTENT_RANGE = "Content-Range"
    // endregion

    // region Security
    /**
     * Allows a server to declare an embedder policy for a given document.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Embedder-Policy">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Embedder-Policy</a>
     */
    const val CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy"

    /**
     * Prevents other domains from opening/controlling a window.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Opener-Policy">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Opener-Policy</a>
     */
    const val CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy"

    /**
     * Prevents other domains from reading the response of the resources to which this header is applied.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Resource-Policy">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cross-Origin-Resource-Policy</a>
     */
    const val CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy"

    /**
     * Controls resources the user agent is allowed to load for a given page.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy</a>
     */
    const val CONTENT_SECURITY_POLICY = "Content-Security-Policy"

    /**
     * Allows web developers to experiment with policies by monitoring, but not enforcing, their effects. These violation reports consist of JSON documents sent via an HTTP POST request to the specified URI.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy-Report-Only</a>
     */
    const val CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only"

    /**
     * Allows sites to opt in to reporting and/or enforcement of Certificate Transparency requirements, which prevents the use of misissued certificates for that site from going unnoticed. When a site enables the Expect-CT header, they are requesting that Chrome check that any certificate for that site appears in public CT logs.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT</a>
     */
    const val EXPECT_CT = "Expect-CT"

    /**
     * Provides a mechanism to allow and deny the use of browser features in its own frame, and in iframes that it embeds.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Feature-Policy">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Feature-Policy</a>
     */
    const val FEATURE_POLICY = "Feature-Policy"

    /**
     * Force communication using HTTPS instead of HTTP.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security</a>
     */
    const val STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security"

    /**
     * Sends a signal to the server expressing the client’s preference for an encrypted and authenticated response, and that it can successfully handle the upgrade-insecure-requests directive.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade-Insecure-Requests">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade-Insecure-Requests</a>
     */
    const val UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests"

    /**
     * Disables MIME sniffing and forces browser to use the type given in Content-Type.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options</a>
     */
    const val X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options"

    /**
     * Indicates whether a browser should be allowed to render a page in a <frame>, <iframe>, <embed> or <object>.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options</a>
     */
    const val X_FRAME_OPTIONS = "X-Frame-Options"

    /**
     * Enables cross-site scripting filtering.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection</a>
     */
    const val X_XSS_PROTECTION = "X-XSS-Protection"
    // endregion

    // region HTTP Public Key Pinning
    /**
     * Associates a specific cryptographic public key with a certain web server to decrease the risk of MITM attacks with forged certificates.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Public-Key-Pins">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Public-Key-Pins</a>
     */
    const val PUBLIC_KEY_PINS = "Public-Key-Pins"

    /**
     * Sends reports to the report-uri specified in the header and does still allow clients to connect to the server even if the pinning is violated.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Public-Key-Pins-Report-Only">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Public-Key-Pins-Report-Only</a>
     */
    const val PUBLIC_KEY_PINS_REPORT_ONLY = "Public-Key-Pins-Report-Only"
    // endregion

    // region Transfer coding

    /**
     * Specifies the form of encoding used to safely transfer the resource to the user.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding">https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding</a>
     */
    const val TRANSFER_ENCODING = "Transfer-Encoding"
    // endregion

}
