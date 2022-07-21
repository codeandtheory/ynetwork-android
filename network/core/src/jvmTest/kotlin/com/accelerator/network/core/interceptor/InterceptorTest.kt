package com.accelerator.network.core.interceptor

import com.yml.network.core.Headers
import com.accelerator.network.core.User
import com.yml.network.core.request.DataRequest
import com.yml.network.core.request.RequestPath
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

private const val API_BASE_PATH = "https://www.yml.co/api"
private const val API_USERS = "/users"

class InterceptorTest {

    private val mockRequestInterceptor = MockInterceptor()

    @Test
    fun addHeadersInRawRequest() {
        val interceptedRequest = mockRequestInterceptor.onRawRequest(createRawRequest())
        assertThat(interceptedRequest.headers!!.size, `is`(1))
    }

    @Test
    fun addHeadersInParsedRequest() {
        val interceptedRequest = mockRequestInterceptor.onParsedRequest(createParsedRequest())
        assertThat(interceptedRequest.headers!!.size, `is`(1))
    }

    private fun createRawRequest() =
        DataRequest.get(RequestPath(API_USERS).setBasePath(API_BASE_PATH), User::class)
            .setHeaders(Headers()).build()

    private fun createParsedRequest() =
        DataRequest.get(RequestPath(API_USERS).setBasePath(API_BASE_PATH), String::class)
            .setHeaders(Headers()).build()
}
