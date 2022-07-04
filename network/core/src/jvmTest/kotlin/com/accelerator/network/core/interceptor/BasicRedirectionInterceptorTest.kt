package com.accelerator.network.core.interceptor

import com.accelerator.network.core.Headers
import com.accelerator.network.core.constants.HeadersConstants
import com.accelerator.network.core.interceptors.BasicRedirectionInterceptor
import com.accelerator.network.core.interceptors.RedirectionState
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.DataSource
import com.accelerator.network.core.response.HttpStatusCode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val API_1_PATH = "https://www.yml.co/api/users"
private const val API_2_PATH = "https://api.yml.co/users"

class BasicRedirectionInterceptorTest {

    private lateinit var target: BasicRedirectionInterceptor

    @BeforeEach
    fun setup() {
        target = object : BasicRedirectionInterceptor {}
    }

    @Test
    fun verifyBasicGetRedirection() {
        val redirectionState = target.onRedirect(
            createGetBasicRequest(API_1_PATH),
            createRedirectionResponse(API_2_PATH)
        )
        assertThat(redirectionState, `is`(instanceOf(RedirectionState.Allowed::class.java)))
        val redirectionRequest = (redirectionState as RedirectionState.Allowed).data
        assertThat(redirectionRequest.requestPath.build(), `is`(API_2_PATH))
    }

    @Test
    fun verifyBasicGetRedirectionWithoutRedirectionUrl() {
        val redirectionState = target.onRedirect(
            createGetBasicRequest(API_1_PATH),
            createRedirectionResponse(null)
        )
        assertThat(redirectionState, `is`(instanceOf(RedirectionState.Cancel::class.java)))
    }

    private fun createRedirectionResponse(
        url: String?,
        dataSource: DataSource = DataSource.Network,
        statusCode: HttpStatusCode = HttpStatusCode.TEMPORARY_REDIRECT
    ): DataResponse<String> = DataResponse(
        "",
        Headers().apply { url?.let { add(HeadersConstants.LOCATION, it) } },
        dataSource,
        statusCode

    )

    private fun createGetBasicRequest(url: String) = DataRequest.get(url, String::class).build()
}
