package co.accelerator.network.core

import co.yml.network.core.constants.HeadersConstants
import co.yml.network.core.engine.network.demo.DemoNetworkEngine
import co.yml.network.core.interceptors.BasicRedirectionInterceptor
import co.yml.network.core.interceptors.Interceptor
import co.yml.network.core.interceptors.RedirectionState
import co.yml.network.core.parser.BasicDataParserFactory
import co.yml.network.core.parser.DataParser
import co.accelerator.network.core.parser.MockObjectParser
import co.yml.network.core.Headers
import co.yml.network.core.NetworkManagerBuilder
import co.yml.network.core.Resource
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.Method
import co.yml.network.core.request.RequestPath
import co.yml.network.core.response.DataResponse
import co.yml.network.core.response.DataSource
import co.yml.network.core.response.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DUMMY_MIME_TYPE = "text/object"
private const val API_1_PATH = "https://www.yml.co/api/users"
private const val API_2_PATH = "https://api.yml.co/users"
private const val API_3_PATH = "https://api.yml.co/v1/users"
private const val API_4_PATH = "https://api.yml.co/v2/users"
private const val API_5_PATH = "https://api.yml.co/v3/users"
private const val API_6_PATH = "https://api.yml.co/v4/users"

private val API_LIST = listOf(
    API_1_PATH,
    API_2_PATH,
    API_3_PATH,
    API_4_PATH,
    API_5_PATH,
    API_6_PATH
)

class NetworkManagerRedirectionTest {

    private lateinit var parser: DataParser
    private lateinit var networkEngine: DemoNetworkEngine
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        parser = MockObjectParser()
        networkEngine = DemoNetworkEngine()
        user = User(1, "John Doe")
    }

    @Test
    fun verifyBasicRedirection() = runBlocking {
        val target = createManager()
        setupRedirections(listOf(API_1_PATH, API_2_PATH))
        networkEngine.on(
            Method.GET,
            RequestPath(API_2_PATH),
            createSuccessResponse(parser.serialize(user))
        )

        val responses = target.submit(createGetBasicRequest(API_1_PATH)).asList()
        // Verify that the end developer/user doesn't have any effect/impact due to redirections.
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyMultipleRedirection() = runBlocking {
        val target = createManager()
        // Redirections are as follow:
        //  API_1_PATH -> API_2_PATH -> API_3_PATH -> API_4_PATH -> API_5_PATH -> API_6_PATH
        setupRedirections(API_LIST)
        networkEngine.on(
            Method.GET,
            RequestPath(API_6_PATH),
            createSuccessResponse(parser.serialize(user))
        )

        val responses = target.submit(createGetBasicRequest(API_1_PATH)).asList()
        // Verify that the end developer/user doesn't have any effect/impact due to redirections.
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)
    }

    @Test
    fun verifyRedirectionWithInterception() = runBlocking {
        val target = createManager()
        setupRedirections(API_LIST)
        networkEngine.on(
            Method.GET,
            RequestPath(API_6_PATH),
            createSuccessResponse(parser.serialize(user))
        )

        val redirectionInterceptor = createUrlBlockingRedirectionInterceptor(API_3_PATH)
        val responses =
            target.submit(createGetBasicRequest(API_1_PATH, listOf(redirectionInterceptor)))
                .asList()
        // Verify that the end developer/user doesn't have any effect/impact due to redirections.
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifyErrorResource(responses[1], RuntimeException::class, "Redirection Rejected")
    }

    @Test
    fun verifyRedirectionWithNonConflictingMultipleInterception() = runBlocking {
        val target = createManager()
        setupRedirections(API_LIST)

        networkEngine.on(
            Method.GET,
            RequestPath(API_6_PATH),
            createSuccessResponse(parser.serialize(user))
        )

        val loggerInterceptor = createNoOpRedirectionInterceptor()
        val urlBlockingInterceptor = createUrlBlockingRedirectionInterceptor(API_3_PATH)
        val urlModifyingInterceptor =
            createUrlModifyingRedirectionInterceptor(fromUrl = API_2_PATH, toUrl = API_4_PATH)
        val basicInterceptor = object : Interceptor {}

        val analyticsInterceptor = mockk<Interceptor>()
        every { analyticsInterceptor.onRawRequest<Any>(any()) } returnsArgument 0
        every { analyticsInterceptor.onParsedRequest(any()) } returnsArgument 0
        every { analyticsInterceptor.onRawResponse(any(), any()) } returnsArgument 0
        every { analyticsInterceptor.onParsedResponse<Any>(any(), any()) } returnsArgument 0

        every { analyticsInterceptor.onRedirect(any(), any()) } returns RedirectionState.NoOp()

        val responses = target.submit(
            createGetBasicRequest(
                API_1_PATH,
                listOf(
                    basicInterceptor,
                    loggerInterceptor,
                    urlBlockingInterceptor,
                    urlModifyingInterceptor,
                    analyticsInterceptor
                )
            )
        ).asList()
        // Verify that the end developer/user doesn't have any effect/impact due to redirections.
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifySuccessResource(responses[1], user)

        // Even though the `urlModifyingInterceptor` had handled/consumed for the `API_2` redirection,
        // the other interceptors still got the callback allowing them to perform their respective
        // operations.
        // API_1 -> (API_2 Modified to API_4) -> API_5 -> API_6  (3 http redirections)
        verify(exactly = 3) { analyticsInterceptor.onRedirect(any(), any()) }
    }

    @Test
    fun verifyRedirectionWithConflictingMultipleInterception() = runBlocking {
        val target = createManager()
        setupRedirections(API_LIST)

        networkEngine.on(
            Method.GET,
            RequestPath(API_6_PATH),
            createSuccessResponse(parser.serialize(user))
        )

        val loggerInterceptor = createNoOpRedirectionInterceptor()
        val urlBlockingInterceptor = createUrlBlockingRedirectionInterceptor(API_2_PATH)
        val urlModifyingInterceptor =
            createUrlModifyingRedirectionInterceptor(fromUrl = API_2_PATH, toUrl = API_4_PATH)

        val responses = target.submit(
            createGetBasicRequest(
                API_1_PATH,
                listOf(loggerInterceptor, urlBlockingInterceptor, urlModifyingInterceptor)
            )
        ).asList()
        // Verify that the end developer/user doesn't have any effect/impact due to redirections.
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        // When the interceptors are conflicted, the first operation generated by the list would be performed.
        // Here, the urlBlockingInterceptor produced first operation of blocking URL, hence redirection was rejected.
        verifyErrorResource(responses[1], RuntimeException::class, "Redirection Rejected")
    }

    @Test
    fun verifyBasicRedirectionWithoutFollowRedirectConfig() = runBlocking {
        val target = createManager(false)
        setupRedirections(listOf(API_1_PATH, API_2_PATH))
        networkEngine.on(
            Method.GET,
            RequestPath(API_2_PATH),
            createSuccessResponse(parser.serialize(user))
        )

        val responses = target.submit(createGetBasicRequest(API_1_PATH)).asList()
        assertThat(responses, hasSize(2))
        verifyLoadingResource(responses[0])
        verifyErrorResource(
            responses[1],
            RuntimeException::class,
            "Got redirection request. Current configuration doesn't support any redirection."
        )
    }

    private fun setupRedirections(paths: List<String>) {
        for (index in 1..paths.lastIndex) {
            networkEngine.on(
                Method.GET,
                RequestPath(paths[index - 1]),
                createRedirectionResponse(paths[index])
            )
        }
    }

    private fun createRedirectionResponse(
        url: String,
        dataSource: DataSource = DataSource.Network,
        statusCode: HttpStatusCode = HttpStatusCode.TEMPORARY_REDIRECT
    ): Resource<DataResponse<String>> = Resource.Loading(
        DataResponse(
            "",
            Headers().add(HeadersConstants.LOCATION, url),
            dataSource,
            statusCode
        )
    )

    private fun createSuccessResponse(
        response: String,
        dataSource: DataSource = DataSource.Network
    ): Resource<DataResponse<String>> = Resource.Success(
        DataResponse(
            response,
            Headers()
                .add(HeadersConstants.CONTENT_LENGTH, response.length.toString())
                .add(HeadersConstants.CONTENT_TYPE, DUMMY_MIME_TYPE),
            dataSource,
            HttpStatusCode.OK
        )
    )

    private fun createGetBasicRequest(url: String, interceptors: List<Interceptor>? = null) =
        DataRequest.Builder(url, Method.GET, User::class).setInterceptors(interceptors)

    private fun createManager(shouldFollowRedirect: Boolean = true) = NetworkManagerBuilder(
        networkEngine,
        BasicDataParserFactory(mapOf(DUMMY_MIME_TYPE to parser))
    )
        .setShouldFollowRedirect(shouldFollowRedirect)
        .build()

    private fun createNoOpRedirectionInterceptor() = object : Interceptor {
        override fun onRedirect(
            request: DataRequest<String>,
            response: DataResponse<String>
        ): RedirectionState<DataRequest<String>> = RedirectionState.NoOp()
    }

    private fun createUrlBlockingRedirectionInterceptor(blockedUrl: String) =
        object : BasicRedirectionInterceptor {
            override fun getUrlAndHeader(
                request: DataRequest<String>,
                response: DataResponse<String>
            ): RedirectionState<Pair<String, Headers?>> {
                val url = response.headers?.get(HeadersConstants.LOCATION)
                if (url == blockedUrl) {
                    return RedirectionState.Cancel()
                }
                return RedirectionState.NoOp()
            }
        }

    private fun createUrlModifyingRedirectionInterceptor(fromUrl: String, toUrl: String) =
        object : BasicRedirectionInterceptor {
            override fun getUrlAndHeader(
                request: DataRequest<String>,
                response: DataResponse<String>
            ): RedirectionState<Pair<String, Headers?>> {
                val url = response.headers?.get(HeadersConstants.LOCATION)
                if (url == fromUrl) {
                    return RedirectionState.Allowed(toUrl to request.headers)
                }
                return RedirectionState.NoOp()
            }
        }
}
