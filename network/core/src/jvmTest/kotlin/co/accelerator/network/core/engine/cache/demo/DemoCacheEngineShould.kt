package co.accelerator.network.core.engine.cache.demo

import co.yml.network.core.Resource
import co.yml.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.Method
import co.yml.network.core.response.DataResponse
import co.yml.network.core.response.DataSource
import co.yml.network.core.response.HttpStatusCode
import co.yml.network.core.response.StatusCodeException
import co.accelerator.network.core.verifyStatusCodeErrorResource
import co.accelerator.network.core.verifySuccessResource
import co.yml.network.core.engine.cache.demo.DemoCacheEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test

internal const val USER_CACHE_KEY = "user"
internal const val USER_API = "/api/getUser"
internal const val FIRST_USER_DATA = "{\"id\": 1, \"name\": \"John Doe\"}"
internal const val SECOND_USER_DATA = "{\"id\": 2, \"name\": \"John Doe 2\"}"

class DemoCacheEngineShould {

    @Test
    fun verifySuccessResponse() = runBlocking {
        val target = DemoCacheEngine()
            .on(Method.GET, USER_CACHE_KEY, createSuccessResponse(FIRST_USER_DATA))

        val results = target.submit(createRequest()).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA, DataSource.Cache)
    }

    @Test
    fun verifyMultipleResponses() = runBlocking {
        val target = DemoCacheEngine()
            .on(Method.GET, USER_CACHE_KEY, createSuccessResponse(FIRST_USER_DATA))
            .on(Method.GET, USER_CACHE_KEY, createSuccessResponse(SECOND_USER_DATA))

        val results = target.submit(createRequest()).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA, DataSource.Cache)

        // For making another request, we get second user's data
        val results2 = target.submit(createRequest()).toList()
        assertThat(results2, hasSize(1))
        verifySuccessResource(results2[0], SECOND_USER_DATA, DataSource.Cache)
    }

    @Test
    fun verifyResponsesForMultipleSetup() = runBlocking {
        val target = DemoCacheEngine()
            .on(Method.GET, USER_CACHE_KEY, createSuccessResponse(FIRST_USER_DATA), 2)
            .on(Method.GET, USER_CACHE_KEY, createSuccessResponse(SECOND_USER_DATA))

        val results = target.submit(createRequest()).toList()
        assertThat(results, hasSize(1))
        verifySuccessResource(results[0], FIRST_USER_DATA, DataSource.Cache)

        // Making another request
        val results2 = target.submit(createRequest()).toList()
        assertThat(results2, hasSize(1))
        verifySuccessResource(results2[0], FIRST_USER_DATA, DataSource.Cache)

        // Making another request
        val results3 = target.submit(createRequest()).toList()
        assertThat(results3, hasSize(1))
        verifySuccessResource(results3[0], SECOND_USER_DATA, DataSource.Cache)
    }

    @Test
    fun verifyFailureResponse() = runBlocking {
        val errorMessage = "Bad request, missing data"
        val target = DemoCacheEngine()
            .on(
                Method.GET,
                USER_CACHE_KEY,
                Resource.Error(StatusCodeException(HttpStatusCode.BAD_REQUEST, errorMessage))
            )
        val results = target.submit(createRequest()).toList()
        assertThat(results, hasSize(1))
        verifyStatusCodeErrorResource(results[0], HttpStatusCode.BAD_REQUEST, errorMessage)
    }

    @Test
    fun verifyResponseForNoSetup() = runBlocking {
        val target = DemoCacheEngine()
        val results = target.submit(createRequest()).toList()
        assertThat(results, hasSize(1))
        verifyStatusCodeErrorResource(results[0], HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND)
    }

    private fun createSuccessResponse(body: String) =
        Resource.Success(DataResponse(body, null, DataSource.Cache, HttpStatusCode.OK))

    private fun createRequest(method: Method = Method.GET) =
        DataRequest.Builder(USER_API, method, String::class).setCacheKey(USER_CACHE_KEY).build()
}
