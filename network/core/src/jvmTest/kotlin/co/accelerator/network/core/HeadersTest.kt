package co.accelerator.network.core

import co.yml.network.core.Headers
import co.yml.network.core.MimeType
import co.yml.network.core.constants.HeadersConstants
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Test

private const val COOKIE1 =
    "name1=value1; Expires=Thu, 31 Oct 2022 07:28:00 GMT; SameSite=Strict; Secure; HttpOnly"
private const val COOKIE2 = "name2=value2; Expires=Thu, 31 Oct 2023 07:28:00 GMT;"
private const val COOKIE3 = "name3=value3;"

class HeadersTest {

    @Test
    fun verifyAdd() {
        val headers = Headers()
        assertThat(headers.size, `is`(0))

        headers.add(HeadersConstants.CONTENT_TYPE, MimeType.JSON.toString())
        assertThat(headers.size, `is`(1))
    }

    @Test
    fun verifyGet() {
        val headers = createHeaders()

        val headerValue = headers.get(HeadersConstants.CONTENT_TYPE)
        assertThat(headerValue, `is`(MimeType.JSON.toString()))
    }

    @Test
    fun verifyGetFirst() {
        val headers = createHeaders()

        val headerValue = headers.get(HeadersConstants.SET_COOKIE)
        assertThat(headerValue, `is`(COOKIE1))
    }

    @Test
    fun verifyGetLast() {
        val headers = createHeaders()

        val headerValue = headers.getLast(HeadersConstants.SET_COOKIE)
        assertThat(headerValue, `is`(COOKIE3))
    }

    @Test
    fun verifyGetAll() {
        val headers = createHeaders()

        val headerValues = headers.getAll(HeadersConstants.SET_COOKIE)
        assertThat(headerValues, hasSize(3))
        assertThat(headerValues[0], `is`(COOKIE1))
        assertThat(headerValues[1], `is`(COOKIE2))
        assertThat(headerValues[2], `is`(COOKIE3))
    }

    @Test
    fun verifyAppend() {
        val headers = createHeaders()

        val clonedHeaders = Headers()
        assertThat(clonedHeaders.size, `is`(0))

        clonedHeaders.append(headers)
        assertThat(clonedHeaders.size, `is`(headers.size))
        headers.keys().forEach {
            assertThat(clonedHeaders.getAll(it), `is`(headers.getAll(it)))
        }
    }

    @Test
    fun verifyAppendFromConstructor() {
        val headers = createHeaders()

        val clonedHeaders = Headers(headers)
        assertThat(clonedHeaders.size, `is`(headers.size))
        headers.keys().forEach {
            assertThat(clonedHeaders.getAll(it), `is`(headers.getAll(it)))
        }
    }

    @Test
    fun verifyAppendWithExistingValues() {
        val headers = createHeaders()

        val cookie4 = "name4=value4;"
        val clonedHeaders = Headers().add(HeadersConstants.CONTENT_LENGTH, "256")
            .add(HeadersConstants.SET_COOKIE, cookie4)
        assertThat(clonedHeaders.size, `is`(2))

        val headerValues1 = clonedHeaders.getAll(HeadersConstants.SET_COOKIE)
        assertThat(headerValues1, hasSize(1))
        assertThat(headerValues1[0], `is`(cookie4))

        clonedHeaders.append(headers)
        assertThat(clonedHeaders.size, `is`(headers.size + 2))

        val headerValues2 = clonedHeaders.getAll(HeadersConstants.SET_COOKIE)
        assertThat(headerValues2, hasSize(4))
        assertThat(headerValues2[0], `is`(cookie4))
        assertThat(headerValues2[1], `is`(COOKIE1))
        assertThat(headerValues2[2], `is`(COOKIE2))
        assertThat(headerValues2[3], `is`(COOKIE3))

        assertThat(clonedHeaders.get(HeadersConstants.CONTENT_LENGTH), `is`("256"))
    }

    @Test
    fun verifyRemove() {
        val headers = createHeaders()
        val originalHeaderSize = headers.size

        val nonExistingHeaderName = HeadersConstants.ACCEPT
        val nonExistingHeaderValue = "name4=value4;"

        assertThat(headers.remove(nonExistingHeaderName, nonExistingHeaderValue), `is`(false))
        assertThat(
            headers.size,
            `is`(originalHeaderSize)
        )  // Verify size haven't changed for unsuccessful remove

        assertThat(headers.remove(nonExistingHeaderName, COOKIE1), `is`(false))
        assertThat(
            headers.size,
            `is`(originalHeaderSize)
        )  // Verify size haven't changed for unsuccessful remove

        assertThat(headers.remove(HeadersConstants.SET_COOKIE, nonExistingHeaderValue), `is`(false))
        assertThat(
            headers.size,
            `is`(originalHeaderSize)
        )  // Verify size haven't changed for unsuccessful remove

        assertThat(headers.remove(HeadersConstants.SET_COOKIE, COOKIE1), `is`(true))
        assertThat(
            headers.size,
            `is`(originalHeaderSize - 1)
        )  // Verify size has changed for successful remove
    }

    @Test
    fun verifyRemoveWithDuplicates() {
        val headers = createHeaders().add(
            HeadersConstants.SET_COOKIE,
            COOKIE1
        ) // Append duplicate key-value pair
        val originalHeaderSize = headers.size

        assertThat(headers.remove(HeadersConstants.SET_COOKIE, COOKIE1), `is`(true))
        assertThat(
            headers.size,
            `is`(originalHeaderSize - 1)
        )  // Verify size has changed for successful remove
    }

    @Test
    fun verifyRemoveAll() {
        val headers = createHeaders()
        val originalHeaderSize = headers.size

        val nonExistingHeaderName = HeadersConstants.ACCEPT
        assertThat(headers.removeAll(nonExistingHeaderName), `is`(false))
        assertThat(
            headers.size,
            `is`(originalHeaderSize)
        )  // Verify size haven't changed for unsuccessful remove

        val numberOfSetCookieValues = headers.getAll(HeadersConstants.SET_COOKIE).size
        assertThat(headers.removeAll(HeadersConstants.SET_COOKIE), `is`(true))
        assertThat(
            headers.size,
            `is`(originalHeaderSize - numberOfSetCookieValues)
        )  // Verify size has changed for successful remove
    }

    @Test
    fun verifyRemoveAllWithCustomLogic() {
        val headers = createHeaders()
        val originalHeaderSize = headers.size

        val numberOfCookieValues = headers.getAll(HeadersConstants.SET_COOKIE).size
        val removeResult = headers.removeAll { headerName, _ -> headerName.contains("cookie") }
        assertThat(removeResult, `is`(true))
        assertThat(
            headers.size,
            `is`(originalHeaderSize - numberOfCookieValues)
        )  // Verify size has changed for successful remove
    }

    private fun createHeaders() = Headers()
        .add(HeadersConstants.CONTENT_TYPE, MimeType.JSON.toString())
        .add(HeadersConstants.SET_COOKIE, COOKIE1)
        .add(HeadersConstants.SET_COOKIE, COOKIE2)
        .add(HeadersConstants.SET_COOKIE, COOKIE3)
}
