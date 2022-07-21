package com.accelerator.network.core.request

import com.yml.network.core.request.RequestPath
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RequestPathTest {
    @Test
    fun testRemoveQueryParam() {
        val reqObject =
            RequestPath().setBasePath("https://yml.co/abc").addParams("Query Key", "Query Value")
        reqObject.removeAll("Query Key")
        assertThat(reqObject.build(), `is`("https://yml.co/abc"))
    }

    @Test
    fun testEmptyPathSegment() {
        val reqObject = RequestPath().setBasePath("https://yml.co/abc")
        assertThat(reqObject.build(), `is`("https://yml.co/abc"))
    }

    @Test
    fun testEmptyPathSegmentToString() {
        val reqObject = RequestPath().setBasePath("https://yml.co/abc")
        assertThat(reqObject.toString(), `is`("https://yml.co/abc"))
    }

    @Test
    fun testExceptionForMissingBasePath() {
        val reqObject = RequestPath()
        val exception = assertThrows<IllegalArgumentException> { reqObject.build() }
        assertThat(exception.message, `is`("Base path is not specified"))
    }

    @Test
    fun testExceptionForInvalidPath() {
        val reqObject = RequestPath("/get user data/").setBasePath("https://yml.co/")
        val exception = assertThrows<IllegalArgumentException> { reqObject.build() }
        assertThat(exception.message, `is`("Invalid url"))
    }

    @Test
    fun testEmptyBasePath() {
        val exception = assertThrows<IllegalArgumentException> { RequestPath().build() }
        assertThat(exception.message, `is`("Base path is not specified"))
    }

    @Test
    fun testAddQueryParam() {
        val reqObject = RequestPath().setBasePath("https://yml.co/abc").addParams("id", "1")
        assertThat(reqObject.build(), `is`("https://yml.co/abc?id=1"))
    }

    @Test
    fun testAddQueryParamWithSpace() {
        val reqObject =
            RequestPath().setBasePath("https://yml.co/abc").addParams("q", "Search Param")
        assertThat(reqObject.build(), `is`("https://yml.co/abc?q=Search%20Param"))
    }

    @Test
    fun testAddQueryParamWithPlus() {
        val reqObject =
            RequestPath().setBasePath("https://yml.co/abc").addParams("q", "Search+Param")
        assertThat(reqObject.build(), `is`("https://yml.co/abc?q=Search%2BParam"))
    }

    @Test
    fun testAppendParam() {
        val reqObject =
            RequestPath().setBasePath("https://yml.co/abc").appendPathSegment("v1/user/", "id", "1")
        assertThat(reqObject.build(), `is`("https://yml.co/abc/v1/user/id/1"))
    }

    @Test
    fun testAbsoluteUrl() {
        val reqObject = RequestPath("https://yml.co/abc")
        assertThat(reqObject.build(), `is`("https://yml.co/abc"))
    }

    @Test
    fun testBaseUrlWithQueryParams() {
        val reqPath =
            RequestPath("/user").addParams("k1", "v1").setBasePath("https://yml.co/abc?attempt=1")
        assertThat(reqPath.build(), `is`("https://yml.co/abc/user?attempt=1&k1=v1"))
    }

    @Test
    fun testBaseUrlWithMultipleQueryParams() {
        val reqPath =
            RequestPath("/user").addParams("k1", "v1").setBasePath("https://yml.co/abc?attempt=1&k1=v2")
        assertThat(reqPath.build(), `is`("https://yml.co/abc/user?attempt=1&k1=v2&k1=v1"))
    }
}
