package co.accelerator.network.core.engine.cache.demo

import co.yml.network.core.Resource
import co.yml.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import co.yml.network.core.engine.cache.cacheErrorMethodNotAllowed
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class DemoCacheEngineMethodsShould() {

    companion object {
        @JvmStatic
        fun data() = Method.values()
            .map { Arguments.of(it, it == Method.GET || it == Method.PUT || it == Method.DELETE) }
            .stream()
    }

    @ParameterizedTest
    @MethodSource("data")
    fun verifySuccessResponse(method: Method, isSupported: Boolean) = runBlocking {
        val target = DemoCacheEngine()
            .on(
                method,
                USER_CACHE_KEY,
                Resource.Success(
                    DataResponse(
                        FIRST_USER_DATA,
                        null,
                        DataSource.Cache,
                        HttpStatusCode.OK
                    )
                )
            )
        val results = target.submit(createRequest(method)).toList()
        assertThat(results, hasSize(1))
        if (isSupported) {
            verifySuccessResource(results[0], FIRST_USER_DATA, DataSource.Cache)
        } else {
            verifyStatusCodeErrorResource(
                results[0],
                HttpStatusCode.METHOD_NOT_ALLOWED,
                cacheErrorMethodNotAllowed(method)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    fun verifyErrorResponse(method: Method, isSupported: Boolean) = runBlocking {
        val badRequestErrorMessage = "Bad request, missing data"
        val target = DemoCacheEngine()
            .on(
                method,
                USER_CACHE_KEY,
                Resource.Error(StatusCodeException(HttpStatusCode.BAD_REQUEST, badRequestErrorMessage))
            )
        val results = target.submit(createRequest(method)).toList()
        assertThat(results, hasSize(1))
        if (isSupported) {
            verifyStatusCodeErrorResource(
                results[0],
                HttpStatusCode.BAD_REQUEST,
                badRequestErrorMessage
            )
        } else {
            verifyStatusCodeErrorResource(
                results[0],
                HttpStatusCode.METHOD_NOT_ALLOWED,
                cacheErrorMethodNotAllowed(method)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    fun verifyErrorResponseForNoSetup(method: Method, isSupported: Boolean) = runBlocking {
        val target = DemoCacheEngine()
        val results = target.submit(createRequest(method)).toList()
        assertThat(results, hasSize(1))
        if (isSupported) {
            verifyStatusCodeErrorResource(
                results[0],
                HttpStatusCode.NOT_FOUND,
                CACHE_ERROR_NOT_FOUND
            )
        } else {
            verifyStatusCodeErrorResource(
                results[0],
                HttpStatusCode.METHOD_NOT_ALLOWED,
                cacheErrorMethodNotAllowed(method)
            )
        }
    }

    private fun createRequest(method: Method) = DataRequest.Builder(
        USER_API,
        method,
        String::class
    )
        .setCacheKey(USER_CACHE_KEY)
        .build()
}
