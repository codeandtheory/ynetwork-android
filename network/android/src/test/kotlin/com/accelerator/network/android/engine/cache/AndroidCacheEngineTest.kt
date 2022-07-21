package com.accelerator.network.android.engine.cache

import com.accelerator.network.android.engine.cache.disklrucache.DiskLruCache
import com.yml.network.core.Resource
import com.yml.network.core.request.BasicRequestBody
import com.yml.network.core.request.CachePolicy
import com.yml.network.core.request.DataRequest
import com.yml.network.core.request.Method
import com.yml.network.core.request.RequestBody
import com.yml.network.core.response.DataResponse
import com.yml.network.core.response.DataSource
import com.yml.network.core.response.HttpStatusCode
import com.yml.network.core.response.StatusCodeException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.hamcrest.core.Is
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.io.Serializable
import kotlin.reflect.KClass

data class User(val id: Int, val name: String) : Serializable

class AndroidCacheEngineTest {
    private lateinit var engine: AndroidCacheEngine
    private val requestBody = BasicRequestBody("")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        engine = AndroidCacheEngine(100, "DataCache.txt", 1)
    }

    @Test
    fun verifyCacheErrorFromGETMethod() {
        runBlocking {
            val request = mockRequestBody(
                body = BasicRequestBody(User(2, "Unique")),
                cacheKey = "uniquecache"
            )
            val responses = engine.submit(request).toList()
            assertThat(responses, hasSize(1))
            verifyStatusCodeErrorResponse(responses[0])
        }
    }

    @Test
    fun verifyCacheFromGETMethod() {
        runBlocking {
            val putRequest = mockRequestBody(
                body = BasicRequestBody(User(1, "username").toString()),
                cacheKey = "userdata",
                method = Method.PUT
            )
            engine.submit(putRequest).toList()
            val getrequest = mockRequestBody(cacheKey = "userdata")
            val result = engine.submit(getrequest).toList()
            assertThat(result, hasSize(1))
            verifySuccessResponse(result[0])
        }
    }

    @Test
    fun verifyCacheToPUTMethod() {
        runBlocking {
            val request = mockRequestBody(method = Method.PUT)
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            verifySuccessResponse(result[0])
        }
    }

    @Test
    fun verifyCacheErrorToPUTMethod() {
        runBlocking {
            val request = mockRequestBody(
                method = Method.PUT,
                body = BasicRequestBody(null)
            )
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            val exception = (result[0] as Resource.Error).error
            assertThat(exception, `is`(instanceOf(StatusCodeException::class.java)))
        }
    }

    @Test
    fun verifyExceptionToPUTMethod() {
        runBlocking {
            val editor = mockk<DiskLruCache.Editor>()
            every { editor[any()] = any() } throws IOException("Unable to write")
            every { editor.abort() } returns Unit
            val lruCache = mockk<DiskLruCache>()
            every { lruCache.edit(any()) } returns editor
            engine.setDiskLruCache(lruCache)
            val request = mockRequestBody(
                method = Method.PUT,
                body = BasicRequestBody("")
            )
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            verifyErrorResource(result[0], IOException::class, "Unable to write")
        }

    }

    @Test
    fun verifyExceptionFromGETMethod() {
        runBlocking {
            val lruCache = mockk<DiskLruCache>()
            every { lruCache[any()] } throws IOException("Unable to read")
            engine.setDiskLruCache(lruCache)
            val request = mockRequestBody(
                body = BasicRequestBody(User(2, "Unique")),
                cacheKey = "uniquecache"
            )
            val responses = engine.submit(request).toList()
            assertThat(responses, hasSize(1))
            verifyErrorResource(responses[0], IOException::class, "Unable to read")
        }
    }

    @Test
    fun verifyErrorWithNullBody() {
        runBlocking {
            val request = mockRequestBody(
                method = Method.PUT,
                body = null
            )
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            verifyStatusCodeErrorResponse(result[0], HttpStatusCode.NOT_IMPLEMENTED)
        }
    }

    @Test
    fun verifyErrorForNullKey() {
        runBlocking {
            val request = mockRequestBody(cacheKey = null)
            runBlocking {
                val result = engine.submit(request).toList()
                assertThat(result, hasSize(1))
                verifyStatusCodeErrorResponse(result[0])
            }
        }
    }

    @Test
    fun verifyCacheErrorToPOSTMethod() {
        runBlocking {
            val request = mockRequestBody(method = Method.POST)
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            verifyStatusCodeErrorResponse(result[0], HttpStatusCode.METHOD_NOT_ALLOWED)
        }
    }

    @Test
    fun verifyCacheErrorToRemoveMethod() {
        runBlocking {
            val request = mockRequestBody(method = Method.DELETE, cacheKey = "delete_key")
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            verifyStatusCodeErrorResponse(result[0], HttpStatusCode.NOT_FOUND)
        }
    }

    @Test
    fun verifyCacheSuccessToRemoveMethod() {
        runBlocking {
            val putRequest = mockRequestBody(method = Method.PUT, cacheKey = "delete_key")
            engine.submit(putRequest).toList()
            val request = mockRequestBody(method = Method.DELETE, cacheKey = "delete_key")
            val result = engine.submit(request).toList()
            assertThat(result, hasSize(1))
            verifySuccessResponse(result[0])
        }
    }

    @Test
    fun verifyRemoveMethodWithNullCache() = runBlocking {
        engine.setDiskLruCache(null)
        val request = mockRequestBody(method = Method.DELETE, cacheKey = "delete_key")
        val result = engine.submit(request).toList()
        assertThat(result, hasSize(1))
        verifyStatusCodeErrorResponse(result[0])
    }


    @Test
    fun verifyInvalidCacheSize() {
        val exception = assertThrows<IllegalArgumentException> {
            val engine = AndroidCacheEngine(-2, "", 1)
            assertThat(engine, null)
        }
        assertThat(exception.message, `is`("maxSize <= 0"))
    }

    @Test
    fun verifyLruCacheForLockedFile() {
        val file = File("DataNewCache.txt")
        val randomFile = RandomAccessFile(file, "rw")
        randomFile.channel.lock()
        val engine = AndroidCacheEngine(100, "DataNewCache.txt", 1)
        assertThat(engine.mDiskLruCache, `is`(nullValue()))
    }

    @Test
    fun verifyExceptionForNullCacheKey() {
        runBlocking {
            val result = engine.submit(mockRequestBody(cacheKey = null)).toList()
            assertThat(result, hasSize(1))
            verifyStatusCodeErrorResponse(result[0])
        }

    }

    private fun mockRequestBody(
        url: String = "https://www.yml.co/users",
        method: Method = Method.GET,
        body: RequestBody? = requestBody,
        cacheKey: String? = "random_key"
    ): DataRequest<String> = DataRequest.Builder(
            url,
            method,
            String::class
        )
            .setBody(body)
            .setCachePolicy(CachePolicy.CacheOnly)
            .setCacheKey(cacheKey)
            .build()

    private fun verifyStatusCodeErrorResponse(
        result: Resource<DataResponse<String>>,
        statusCode: HttpStatusCode = HttpStatusCode.NOT_FOUND
    ) {
        assertThat(result, `is`(instanceOf(Resource.Error::class.java)))
        val exception = (result as Resource.Error<DataResponse<String>>).error
        assertThat(exception, `is`(instanceOf(StatusCodeException::class.java)))
        assertThat(
            (exception as StatusCodeException).statusCode, `is`(statusCode)
        )
    }

    private fun <DATA, EXCEPTION : Any> verifyErrorResource(
        result: Resource<DataResponse<DATA>>,
        exceptionClass: KClass<EXCEPTION>,
        message: String
    ) {
        assertThat(result, `is`(instanceOf(Resource.Error::class.java)))
        val exception = (result as Resource.Error<DataResponse<DATA>>).error
        assertThat(exception, `is`(instanceOf(exceptionClass.java)))
        assertThat(exception?.message, `is`(message))
    }

    private fun verifySuccessResponse(result: Resource<DataResponse<String>>) {
        val successResult = (result as Resource.Success)
        assertThat(successResult, `is`(instanceOf(Resource.Success::class.java)))
        assertThat(successResult.data.statusCode, Is.`is`(HttpStatusCode.OK))
        assertThat(successResult.data.source, Is.`is`(DataSource.Cache))
    }

}