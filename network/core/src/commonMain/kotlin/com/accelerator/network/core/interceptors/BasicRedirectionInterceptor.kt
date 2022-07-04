package com.accelerator.network.core.interceptors

import com.accelerator.network.core.Headers
import com.accelerator.network.core.constants.HeadersConstants
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.RequestPath
import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.HttpStatusCode

/**
 * Basic implementation of [Interceptor] containing the boilerplate code for handling basic
 * redirection operation.
 *
 * This interface could be used by the developers to just manipulate/extract the URL and headers
 * without worrying about the additional boilerplate code.
 */
interface BasicRedirectionInterceptor : Interceptor {

    override fun onRedirect(
        request: DataRequest<String>,
        response: DataResponse<String>
    ) = when (val redirectionState = getUrlAndHeader(request, response)) {
        is RedirectionState.NoOp -> RedirectionState.NoOp()
        is RedirectionState.Cancel -> RedirectionState.Cancel()
        is RedirectionState.Allowed -> {
            val (url, header) = redirectionState.data
            val headers = Headers(header) // Clone headers
            var body = request.body
            body?.let {
                // Maintain body only for temporary and permanent redirection.
                val maintainBody = response.statusCode == HttpStatusCode.TEMPORARY_REDIRECT
                        || response.statusCode == HttpStatusCode.PERMANENT_REDIRECT
                if (!maintainBody) {
                    body = null
                    headers.apply {
                        removeAll(HeadersConstants.CONTENT_TYPE)
                        removeAll(HeadersConstants.CONTENT_LENGTH)
                        removeAll(HeadersConstants.TRANSFER_ENCODING)
                    }
                }
            }
            // When redirecting across hosts, drop all authentication headers. This
            // is potentially annoying to the application layer since they have no
            // way to retain them.
            // TODO: Verify user domain, port, schema to keep/remove Authorization header
            RedirectionState.Allowed(
                request.copy(requestPath = RequestPath(url), headers = headers, body = body)
            )
        }
    }

    /**
     * Extract the URL and header data for redirection.
     *
     * @param request [DataRequest] for which redirection URL and headers needs to be extracted.
     * @param response [DataResponse] for which redirection URL and headers needs to be extracted.
     * @return [RedirectionState] containing redirection URL and headers.
     */
    fun getUrlAndHeader(
        request: DataRequest<String>,
        response: DataResponse<String>
    ): RedirectionState<Pair<String, Headers?>> {
        val url = response.headers?.get(HeadersConstants.LOCATION)
        return if (url == null) RedirectionState.Cancel() else RedirectionState.Allowed(url to request.headers)
    }
}
