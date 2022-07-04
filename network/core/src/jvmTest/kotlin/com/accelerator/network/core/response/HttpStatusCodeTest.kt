package com.accelerator.network.core.response

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HttpStatusCodeTest {

    @Test
    fun shouldReturnHttpStatusCodeForValidCode() {
        val statusCode = HttpStatusCode.of(200)
        assertThat(statusCode.code, `is`(200))
        assertThat(statusCode.message, `is`("Ok"))
    }

    @Test
    fun shouldReturnHttpStatusCodeForValidStringCode() {
        val statusCode = HttpStatusCode.of("400")
        assertThat(statusCode.code, `is`(400))
        assertThat(statusCode.message, `is`("Bad Request"))
    }

    @Test
    fun shouldThrowForInvalidCode() {
        val exception = assertThrows<NoSuchElementException> { HttpStatusCode.of(600) }
        assertThat(exception.message, `is`("Status code 600 not found in HttpStatusCode"))
    }

    @Test
    fun shouldAllowDefaultValueOfMethodForSerialization() {
        val statusCode = HttpStatusCode.valueOf("OK")
        assertThat(statusCode.code, `is`(200))
        assertThat(statusCode.message, `is`("Ok"))
    }

}
