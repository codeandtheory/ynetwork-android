package com.accelerator.network.core.parser

import com.accelerator.network.core.MimeType
import com.accelerator.network.core.request.RequestPath
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BasicDataParserFactoryShould {

    private val mockNetworkParser = BasicDataParserFactory(mockParserFactoryList)

    @Test
    fun verifyParserForValidContentType() {
        val dataParserFactory =
            mockNetworkParser.getParser(MimeType.JSON.toString(), RequestPath(), null, null)
        assertThat(dataParserFactory, instanceOf(MockObjectParser::class.java))
    }

    @Test
    fun verifyParserForValidContentTypeWithCharset() {
        val dataParserFactory =
            mockNetworkParser.getParser(
                "${MimeType.JSON}; charset=utf-8",
                RequestPath(),
                null,
                null
            )
        assertThat(dataParserFactory, instanceOf(MockObjectParser::class.java))
    }

    @Test
    fun verifyExceptionForInvalidContentType() {
        val exception = assertThrows<Exception> {
            mockNetworkParser.getParser(MimeType.TEXT_XML.toString(), RequestPath(), null, null)
        }
        assertThat(exception.message, `is`("No parser specified for ${MimeType.TEXT_XML}."))
    }

}
