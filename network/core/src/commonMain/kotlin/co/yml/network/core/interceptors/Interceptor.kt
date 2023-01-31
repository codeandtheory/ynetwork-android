package co.yml.network.core.interceptors

import co.yml.network.core.Resource
import co.yml.network.core.request.DataRequest
import co.yml.network.core.response.DataResponse

/**
 * A mechanism to handle interception for network calls, observe, modify requests and corresponding
 * responses. Typical use case for this is to modify headers for request and response
 *
 * Implementation of this can allow to override only the required methods.
 * By default all the super methods will return default value.
 */
interface Interceptor {
    /**
     * @param request raw request data
     * @return by default passed request argument will be returned
     */
    fun <RESPONSE : Any> onRawRequest(request: DataRequest<RESPONSE>): DataRequest<RESPONSE> =
        request

    /**
     * @param request parsed request data
     * @return by default passed request argument will be returned
     */
    fun onParsedRequest(request: DataRequest<String>): DataRequest<String> = request

    /**
     * @param response raw response data from the network call
     * @param request parsed request data
     * @return by default parsed response argument will be returned
     */
    fun onRawResponse(
        response: Resource<DataResponse<String>>,
        request: DataRequest<String>
    ): Resource<DataResponse<String>> = response

    /**
     * @param response parsed response data
     * @param request parsed request data
     * @return by default parsed response argument will be returned
     */
    fun <RESPONSE : Any> onParsedResponse(
        response: Resource<DataResponse<RESPONSE>>,
        request: DataRequest<String>
    ): Resource<DataResponse<RESPONSE>> = response

    /**
     * This method intercepts the redirection call and allow developers to decide whether the
     * redirection should continue or not.
     *
     * @param request [DataRequest] for which redirection needs to be done.
     * @param response [DataResponse] containing redirection data.
     * @return [RedirectionState] containing the data containing redirection decision.
     */
    fun onRedirect(
        request: DataRequest<String>,
        response: DataResponse<String>
    ): RedirectionState<DataRequest<String>> = RedirectionState.NoOp()
}
