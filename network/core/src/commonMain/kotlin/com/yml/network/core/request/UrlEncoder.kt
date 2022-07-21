package com.yml.network.core.request

/**
 * Encode the query param URL data. E.g. Replace space to %20 in query data.
 *
 * @param data which needs to be encoded.
 * @param charset which needs to be referred while encoding the [data]. E.g. "utf-8".
 *
 * @return the encoded data.
 */
expect fun encodeUrlData(data: String, charset: String): String

/**
 * Decode the query param URL data. E.g. Replace %20 to space in query data.
 *
 * @param data which needs to be decoded.
 * @param charset which needs to be referred while decoding the [data]. E.g. "utf-8".
 *
 * @return the decoded data.
 */
expect fun decodeUrlData(data: String, charset: String): String
