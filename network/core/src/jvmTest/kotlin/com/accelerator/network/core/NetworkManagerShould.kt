package com.accelerator.network.core

import com.yml.network.core.constants.HeadersConstants
import com.yml.network.core.engine.cache.CACHE_ERROR_NOT_FOUND
import com.yml.network.core.engine.cache.CacheEngine
import com.yml.network.core.engine.cache.demo.DemoCacheEngine
import com.yml.network.core.engine.network.demo.DemoNetworkEngine
import com.yml.network.core.parser.BasicDataParserFactory
import com.yml.network.core.parser.DataParser
import com.accelerator.network.core.parser.MockObjectParser
import com.yml.network.core.Headers
import com.yml.network.core.MimeType
import com.yml.network.core.NetworkManager
import com.yml.network.core.NetworkManagerBuilder
import com.yml.network.core.Resource
import com.yml.network.core.request.BasicRequestBody
import com.yml.network.core.request.CachePolicy
import com.yml.network.core.request.DataRequest
import com.yml.network.core.request.Method
import com.yml.network.core.request.MultiPartRequestBody
import com.yml.network.core.request.RequestPath
import com.yml.network.core.response.DataResponse
import com.yml.network.core.response.DataSource
import com.yml.network.core.response.HttpStatusCode
import com.yml.network.core.response.StatusCodeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.Serializable

private const val DUMMY_MIME_TYPE = "text/object"
private const val API_BASE_PATH = "https://www.yml.co/api"
private const val API_USERS = "/users"
private const val CACHE_USERS_KEY = "users"
private const val API_REGISTER = "/register"

data class User(val id: Int, val name: String) : Serializable

class NetworkManagerShould {

    private lateinit var parser: DataParser
    private lateinit var networkEngine: DemoNetworkEngine
    private lateinit var cacheEngine: DemoCacheEngine
    private lateinit var user: User
    private lateinit var cachedUser: User

    @BeforeEach
    fun setup() {
        parser = MockObjectParser()
        networkEngine = DemoNetworkEngine().setBasePath(API_BASE_PATH)
        cacheEngine = DemoCacheEngine()
        user = User(1, "John Doe")
        cachedUser = User(1, "Cached John Doe")
    }

    @Test
    fun verifyForRequestWithoutCachePolicy() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses = target.submit(createGetBasicRequest()).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyForNetworkOnlyRequest() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.NetworkOnly)).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyForCacheAndNetworkParallelRequestWithoutCacheEngine() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheAndNetworkParallel))
                .asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyForCacheAndNetworkParallelRequestWithCacheEngine() = runBlocking {
        val target = createNetworkManager(cacheEngine)
        val cacheResponse = createSuccessResponse(parser.serialize(cachedUser))
        cacheEngine.on(Method.GET, CACHE_USERS_KEY, cacheResponse)
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheAndNetworkParallel))
                .asList()
        assertThat(responses, hasSize(3))
        verifyLoadingResource(responses[0])
        verifyLoadingResource(
            responses[1],
            data = createDataResponse(cachedUser, parser.serialize(cachedUser).length)
        )
        verifySuccessResource(responses[2], user)
    }

    @Test
    fun verifyForCacheAndNetworkParallelRequestWithCacheEngineFailure() = runBlocking {
        val target = createNetworkManager(cacheEngine)
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheAndNetworkParallel))
                .asList()
        assertThat(responses, hasSize(3))
        verifyLoadingResource(responses[0])
        verifyLoadingResource(responses[1], error = StatusCodeException(HttpStatusCode.NOT_FOUND, CACHE_ERROR_NOT_FOUND))
        verifySuccessResource(responses[2], user)
    }

    @Test
    fun verifyForCacheFailsThenNetworkRequestWithoutCacheEngine() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheFailsThenNetwork))
                .asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyForCacheFailsThenNetworkRequestWithCacheEngine() = runBlocking {
        val target = createNetworkManager(cacheEngine)
        cacheEngine.on(
            Method.GET,
            CACHE_USERS_KEY,
            createSuccessResponse(parser.serialize(cachedUser), DataSource.Cache)
        )
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheFailsThenNetwork))
                .asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], cachedUser, DataSource.Cache)
    }

    @Test
    fun verifyForCacheFailsThenNetworkRequestWithCacheEngineFailure() = runBlocking {
        val target = createNetworkManager(cacheEngine)
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheFailsThenNetwork))
                .asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyExceptionForNetworkOnlyRequestWithoutContentHeader(): Unit = runBlocking {
        val target = createNetworkManager()
        val response = Resource.Success(
            DataResponse(
                parser.serialize(user),
                null,
                DataSource.Network,
                HttpStatusCode.OK
            )
        )
        networkEngine.on(Method.GET, RequestPath(API_USERS), response)

        val responses = target.submit(createGetBasicRequest()).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifyErrorResource(
            responses[1],
            IllegalArgumentException::class,
            "No content type received from Server"
        )
    }

    @Test
    fun verifyExceptionForCacheOnlyRequestWithoutCacheEngine(): Unit = runBlocking {
        val target = createNetworkManager()
        val exception = assertThrows<IllegalArgumentException> {
            runBlocking {
                target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheOnly)).asList()
            }
        }
        assertThat(
            exception.message,
            `is`("${CacheEngine::class.simpleName} is not provided to the ${NetworkManager::class.simpleName}")
        )
    }

    @Test
    fun verifyCacheOnlyRequestWithCacheEngine() = runBlocking {
        val target = createNetworkManager(cacheEngine)
        cacheEngine.on(
            Method.GET,
            CACHE_USERS_KEY,
            createSuccessResponse(parser.serialize(cachedUser), DataSource.Cache)
        )
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses =
            target.submit(createGetRequestWithCachePolicy(CachePolicy.CacheOnly)).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], cachedUser, DataSource.Cache)
    }

    @Test
    fun verifyRequestBodyParsing(): Unit = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(Method.PUT, RequestPath(API_USERS), createSuccessResponse(""))

        val responses =
            target.submit(
                DataRequest.put(
                    RequestPath(API_USERS).setBasePath(API_BASE_PATH),
                    String::class,
                    BasicRequestBody(user, DUMMY_MIME_TYPE)
                )
            ).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], "")
    }

    @Test
    fun verifyForRequestConsumptionAsFlow() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responses = target.submit(createGetBasicRequest()).asFlow().toList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    //@Test
    /*fun verifyForRequestConsumptionAsBlockedExecution() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val response = target.submit(createGetBasicRequest()).execute()
        verifySuccessResource(response, user)
    }*/

    @Test
    fun verifyForRequestConsumptionAsAsync() = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        val responseAsync =
            target.submit(createGetBasicRequest()).asAsync(CoroutineScope(Dispatchers.IO))
        val response = responseAsync.await()
        verifySuccessResource(response, user)
    }

    /*@Test
    fun verifyForRequestConsumptionAsCallback() {
        val target = createNetworkManager()
        val mockCallback = mockk<(Resource<DataResponse<User>>) -> Unit>()
        networkEngine.on(
            Method.GET,
            RequestPath(API_USERS),
            createSuccessResponse(parser.serialize(user))
        )

        every { mockCallback(any()) } returns Unit

        val networkCall = target.submit(createGetBasicRequest())
        networkCall.addObserver(mockCallback)

        val headers = Headers()
            .add(HeadersConstants.CONTENT_TYPE, DUMMY_MIME_TYPE)
            .add(HeadersConstants.CONTENT_LENGTH, parser.serialize(user).length.toString())

        verify { mockCallback(Resource.Loading()) }
        verify {
            mockCallback(
                Resource.Success(DataResponse(user, headers, DataSource.Network, HttpStatusCode.OK))
            )
        }
    }*/

    @Test
    fun verifyMultipartRequestBodyParsing(): Unit = runBlocking {
        val target = createNetworkManager()
        networkEngine.on(Method.POST, RequestPath(API_REGISTER), createSuccessResponse(""))

        val responses =
            target.submit(
                DataRequest.post(
                    RequestPath(API_REGISTER).setBasePath(API_BASE_PATH),
                    String::class,
                    MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM)
                        .addFormData("email", "abc@gmail.com")
                        .addFormData("address", "Address Line 123")
                        .build()
                )
            ).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], "")
    }

    private fun createSuccessResponse(
        response: String,
        dataSource: DataSource = DataSource.Network
    ): Resource<DataResponse<String>> =
        Resource.Success(createDataResponse(response, response.length, dataSource))

    private fun <RESPONSE> createDataResponse(
        response: RESPONSE,
        contentLength: Int,
        dataSource: DataSource = DataSource.Network
    ): DataResponse<RESPONSE> = DataResponse(
        response,
        Headers()
            .add(HeadersConstants.CONTENT_TYPE, DUMMY_MIME_TYPE)
            .add(HeadersConstants.CONTENT_LENGTH, contentLength.toString()),
        dataSource,
        HttpStatusCode.OK
    )

    private fun createGetBasicRequest() =
        DataRequest.get(RequestPath(API_USERS).setBasePath(API_BASE_PATH), User::class)

    private fun createGetRequestWithCachePolicy(cachePolicy: CachePolicy) =
        DataRequest.get(RequestPath(API_USERS).setBasePath(API_BASE_PATH), User::class)
            .setCachePolicy(cachePolicy)
            .setCacheKey(CACHE_USERS_KEY)

    private fun createNetworkManager(cacheEngine: CacheEngine? = null) = NetworkManagerBuilder(
        networkEngine,
        BasicDataParserFactory(mapOf(DUMMY_MIME_TYPE to parser))
    )
        .setBasePath(API_BASE_PATH)
        .setCacheEngine(cacheEngine)
        .build()
}
