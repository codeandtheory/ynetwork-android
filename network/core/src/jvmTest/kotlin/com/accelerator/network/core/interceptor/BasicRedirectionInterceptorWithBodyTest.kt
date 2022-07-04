package com.accelerator.network.core.interceptor

import com.accelerator.network.core.Headers
import com.accelerator.network.core.MimeType
import com.accelerator.network.core.constants.HeadersConstants
import com.accelerator.network.core.interceptors.BasicRedirectionInterceptor
import com.accelerator.network.core.interceptors.RedirectionState
import com.accelerator.network.core.request.BasicRequestBody
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.Method
import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.DataSource
import com.accelerator.network.core.response.HttpStatusCode
import com.accelerator.network.core.response.HttpStatusCodeType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

private const val API_1_PATH = "https://www.yml.co/api/users"
private const val API_2_PATH = "https://api.yml.co/users"
private const val BODY = "REQUEST BODY"

class BasicRedirectionInterceptorWithBodyTest() {

    companion object {
        @JvmStatic
        fun data() = ArrayList<Arguments>().apply {
            HttpStatusCode.values()
                .filter { it.type == HttpStatusCodeType.REDIRECT }
                .forEach { statusCode ->
                    Method.values().forEach { method ->
                        add(
                            Arguments.of(
                                method,
                                statusCode,
                                // Maintain body only for temporary or permanent redirection.
                                statusCode == HttpStatusCode.TEMPORARY_REDIRECT
                                        || statusCode == HttpStatusCode.PERMANENT_REDIRECT
                            )
                        )
                    }
                }
        }.stream()
    }

    private lateinit var target: BasicRedirectionInterceptor

    @BeforeEach
    fun setup() {
        target = object : BasicRedirectionInterceptor {}
    }

    @ParameterizedTest
    @MethodSource("data")
    fun verifyBasicRedirection(
        method: Method,
        statusCode: HttpStatusCode,
        shouldMaintainBodyForRedirection: Boolean
    ) {
        val request = createBasicRequest(API_1_PATH, method)
        val redirectionState = target.onRedirect(
            createBasicRequest(API_1_PATH, method),
            createRedirectionResponse(API_2_PATH, statusCode)
        )
        assertThat(redirectionState, `is`(instanceOf(RedirectionState.Allowed::class.java)))
        val redirectionRequest = (redirectionState as RedirectionState.Allowed).data
        assertThat(redirectionRequest.requestPath.build(), `is`(API_2_PATH))

        if (shouldMaintainBodyForRedirection) {
            assertThat(redirectionRequest.body, `is`(request.body))
            assertThat(redirectionRequest.headers?.size, `is`(4))
            assertHeaderPresence(redirectionRequest.headers, HeadersConstants.CONTENT_LENGTH, true)
            assertHeaderPresence(redirectionRequest.headers, HeadersConstants.CONTENT_TYPE, true)
            assertHeaderPresence(
                redirectionRequest.headers,
                HeadersConstants.TRANSFER_ENCODING,
                true
            )
            assertHeaderPresence(redirectionRequest.headers, HeadersConstants.USER_AGENT, true)
        } else {
            assertThat(redirectionRequest.body, `is`(nullValue()))
            assertThat(redirectionRequest.headers?.size, `is`(1))
            assertHeaderPresence(redirectionRequest.headers, HeadersConstants.CONTENT_LENGTH, false)
            assertHeaderPresence(redirectionRequest.headers, HeadersConstants.CONTENT_TYPE, false)
            assertHeaderPresence(
                redirectionRequest.headers,
                HeadersConstants.TRANSFER_ENCODING,
                false
            )
            assertHeaderPresence(redirectionRequest.headers, HeadersConstants.USER_AGENT, true)
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    fun verifyBasicRedirectionWithoutRedirectionUrl(method: Method, statusCode: HttpStatusCode) {
        val redirectionState = target.onRedirect(
            createBasicRequest(API_1_PATH, method),
            createRedirectionResponse(null, statusCode)
        )
        assertThat(redirectionState, `is`(instanceOf(RedirectionState.Cancel::class.java)))
    }

    private fun assertHeaderPresence(headers: Headers?, headerName: String, isPresent: Boolean) {
        val entry = headers?.get(headerName)
        assertThat(entry, `is`(if (isPresent) notNullValue() else nullValue()))
    }

    private fun createRedirectionResponse(url: String?, statusCode: HttpStatusCode) = DataResponse(
        "",
        Headers().apply { url?.let { add(HeadersConstants.LOCATION, it) } },
        DataSource.Network,
        statusCode
    )

    private fun createBasicRequest(url: String, method: Method) =
        DataRequest.Builder(
            url,
            method,
            String::class
        )
            .setBody(BasicRequestBody(BODY))
            .setHeaders(Headers().apply {
                add(HeadersConstants.CONTENT_TYPE, MimeType.JSON.toString())
                add(HeadersConstants.CONTENT_LENGTH, BODY.length.toString())
                add(HeadersConstants.TRANSFER_ENCODING, "gzip")
                add(HeadersConstants.USER_AGENT, "test")
            })
            .build()
}
