package com.accelerator.network.core.interceptor

import com.yml.network.core.interceptors.Interceptor
import com.yml.network.core.request.DataRequest

private const val RAW_HEADER_KEY = "test_header"
private const val RAW_HEADER_VALUE = "test_header_value"

private const val PARSED_HEADER_KEY = "test_header"
private const val PARSED_HEADER_VALUE = "test_header_value"

class MockInterceptor : Interceptor {
    override fun <RESPONSE : Any> onRawRequest(request: DataRequest<RESPONSE>): DataRequest<RESPONSE> {
        request.headers?.add(RAW_HEADER_KEY, RAW_HEADER_VALUE)
        return request
    }

    override fun onParsedRequest(request: DataRequest<String>): DataRequest<String> {
        request.headers?.add(PARSED_HEADER_KEY, PARSED_HEADER_VALUE)
        return request
    }
}
