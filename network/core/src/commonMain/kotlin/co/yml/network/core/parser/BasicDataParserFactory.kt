package co.yml.network.core.parser

import co.yml.network.core.Headers
import co.yml.network.core.MimeType
import co.yml.network.core.request.RequestPath

/**
 * Basic implementation of [DataParserFactory] holding a map of [DataParser] w.r.t. it's contentType.
 */
class BasicDataParserFactory(private val parserFactoryMap: Map<String, DataParser>) :
    DataParserFactory {

    override fun getParser(
        contentType: String,
        requestPath: RequestPath,
        requestHeaders: Headers?,
        responseHeaders: Headers?
    ): DataParser {
        val trimmedContentType = contentType.trim()
        val key = parserFactoryMap.keys.find { trimmedContentType.startsWith(it) }
        return parserFactoryMap[key]
            ?: throw Exception("No parser specified for $trimmedContentType.")
    }

    companion object {
        fun json(jsonDataParser: DataParser) =
            BasicDataParserFactory(mapOf(MimeType.JSON.toString() to jsonDataParser))
    }
}
