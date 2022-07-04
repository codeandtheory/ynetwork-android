package com.accelerator.network.core.engine.network.demo

import com.accelerator.network.core.Resource
import com.accelerator.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.Method
import com.accelerator.network.core.request.RequestPath
import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.DataSource
import com.accelerator.network.core.response.HttpStatusCode
import com.accelerator.network.core.response.StatusCodeException
import com.accelerator.network.core.verifyStatusCodeErrorResource
import com.accelerator.network.core.verifySuccessResource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test

const val USER_API = "/api/getUser"
const val FIRST_USER_DATA = "{\"id\": 1, \"name\": \"John Doe\"}"
const val SECOND_USER_DATA = "{\"id\": 2, \"name\": \"John Doe 2\"}"

class DemoNetworkEngineShould {

    @Test
    fun verifySuccessResponse() = runBlocking {
        val target = DemoNetworkEngine()
            .on(Method.GET, RequestPath(USER_API), createSuccessResponse(FIRST_USER_DATA))

        val results = target.submit(createFetchUserRequest()).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA)
    }

    @Test
    fun verifyMultipleResponses() = runBlocking {
        val target = DemoNetworkEngine()
            .on(Method.GET, RequestPath(USER_API), createSuccessResponse(FIRST_USER_DATA))
            .on(Method.GET, RequestPath(USER_API), createSuccessResponse(SECOND_USER_DATA))

        val results = target.submit(createFetchUserRequest()).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA)

        // For making another request, we get second user's data
        val results2 = target.submit(createFetchUserRequest()).toList()
        assertThat(results2, hasSize(1))
        verifySuccessResource(results2[0], SECOND_USER_DATA)
    }

    @Test
    fun verifyResponsesForMultipleSetup() = runBlocking {
        val target = DemoNetworkEngine()
            .on(Method.GET, RequestPath(USER_API), createSuccessResponse(FIRST_USER_DATA), 2)
            .on(Method.GET, RequestPath(USER_API), createSuccessResponse(SECOND_USER_DATA))

        val results = target.submit(createFetchUserRequest()).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA)

        // Making another request
        val results2 = target.submit(createFetchUserRequest()).toList()
        assertThat(results2, hasSize(1))
        verifySuccessResource(results2[0], FIRST_USER_DATA)

        // Making another request
        val results3 = target.submit(createFetchUserRequest()).toList()
        assertThat(results3, hasSize(1))
        verifySuccessResource(results3[0], SECOND_USER_DATA)
    }

    @Test
    fun verifySuccessResponseWithQueryParam() = runBlocking {
        val target = DemoNetworkEngine()
            .on(
                Method.GET,
                RequestPath(USER_API).addParams("k1", "v1").addParams("k2", "v2"),
                createSuccessResponse(FIRST_USER_DATA)
            )
        val results =
            target.submit(
                DataRequest.get(
                    RequestPath(USER_API).addParams("k2", "v2").addParams("k1", "v1"),
                    String::class
                ).build()
            ).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA)
    }

    @Test
    fun verifyFailureResponse() = runBlocking {
        val badRequestErrorMessage = "Bad request, missing data"
        val target = DemoNetworkEngine()
            .on(
                Method.GET,
                RequestPath(USER_API),
                Resource.Error(
                    StatusCodeException(
                        HttpStatusCode.BAD_REQUEST,
                        badRequestErrorMessage
                    )
                )
            )
        val results = target.submit(createFetchUserRequest()).toList()
        assertThat(results, hasSize(1))
        verifyStatusCodeErrorResource(
            results[0],
            HttpStatusCode.BAD_REQUEST,
            badRequestErrorMessage
        )
    }

    @Test
    fun verifyResponseForNoSetup() = runBlocking {
        val target = DemoNetworkEngine()
        val results = target.submit(createFetchUserRequest()).toList()
        assertThat(results, hasSize(1))
        verifyStatusCodeErrorResource(results[0], HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND)
    }

    @Test
    fun verifyFailureResponseForQueryParamMismatch() = runBlocking {
        val target = DemoNetworkEngine()
            .on(
                Method.GET,
                RequestPath(USER_API).addParams("k1", "v1").addParams("k2", "v2"),
                createSuccessResponse(FIRST_USER_DATA)
            )
        val results =
            target.submit(
                DataRequest.get(
                    RequestPath(USER_API).addParams("k2", "v2"), // Url without k1 query param
                    String::class
                ).build()
            ).toList()
        assertThat(results, hasSize(1))
        verifyStatusCodeErrorResource(results[0], HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND)
    }

    private fun createFetchUserRequest() = DataRequest.get(USER_API, String::class).build()

    private fun createSuccessResponse(body: String) =
        Resource.Success(DataResponse(body, null, DataSource.Network, HttpStatusCode.OK))
}
