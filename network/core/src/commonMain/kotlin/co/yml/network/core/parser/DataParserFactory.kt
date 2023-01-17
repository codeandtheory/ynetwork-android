package co.yml.network.core.parser

import co.yml.network.core.Headers
import co.yml.network.core.request.RequestPath

/**
 * Class/Interface for providing the [DataParser]
 */
interface DataParserFactory {

    /**
     * Provide the parser for specified [contentType].
     *
     * @param contentType contentType/mimeType for which the data parser is required.
     * @param requestPath [RequestPath] for which the data parser is required.
     * @param requestHeaders request [Headers] to provide additional information to choose data parser.
     * @param responseHeaders response [Headers] to provide additional information to choose data parser.
     *
     * @return the [DataParser] for the specified request/response data.
     * @throws [Exception] when [DataParser] for specified request/response data is not available.
     */
    fun getParser(
        contentType: String,
        requestPath: RequestPath,
        requestHeaders: Headers?,
        responseHeaders: Headers?
    ): DataParser
}
