package co.yml.chat.logger

import android.util.Log
import co.yml.network.core.Resource
import co.yml.network.core.interceptors.Interceptor
import co.yml.network.core.interceptors.RedirectionState
import co.yml.network.core.request.DataRequest
import co.yml.network.core.response.DataResponse

private val TAG = LoggerInterceptor::class.simpleName

/**
 * [Interceptor] to intercept and log all outgoing requests and incoming responses.
 */
class LoggerInterceptor : Interceptor {
    override fun <RESPONSE : Any> onRawRequest(request: DataRequest<RESPONSE>): DataRequest<RESPONSE> {
        Log.d(TAG, "Got ${request.method} request for ${request.requestPath}")
        return super.onRawRequest(request)
    }

    override fun onParsedRequest(request: DataRequest<String>): DataRequest<String> {
        Log.d(TAG, "Parsed ${request.method} request body for ${request.requestPath}")
        return super.onParsedRequest(request)
    }

    override fun onRawResponse(
        response: Resource<DataResponse<String>>,
        request: DataRequest<String>
    ): Resource<DataResponse<String>> {
        Log.d(TAG, "Got ${getResourceType(response)} response for ${request.method} ${request.requestPath} $response")
        return super.onRawResponse(response, request)
    }

    override fun <RESPONSE : Any> onParsedResponse(
        response: Resource<DataResponse<RESPONSE>>,
        request: DataRequest<String>
    ): Resource<DataResponse<RESPONSE>> {
        Log.d(TAG, "Parsed ${getResourceType(response)} response for ${request.method} ${request.requestPath} $response")
        return super.onParsedResponse(response, request)
    }

    override fun onRedirect(
        request: DataRequest<String>,
        response: DataResponse<String>
    ): RedirectionState<DataRequest<String>> {
        Log.d(TAG, "Got redirection response for ${request.method} ${request.requestPath}")
        return super.onRedirect(request, response)
    }

    private fun <RESPONSE : Any> getResourceType(response: Resource<DataResponse<RESPONSE>>) =
        when (response) {
            is Resource.Success -> "Success"
            is Resource.Loading -> "Loading"
            is Resource.Error -> "Error"
        }
}
