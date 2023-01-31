package co.yml.network.core.request

import java.net.URLDecoder
import java.net.URLEncoder

actual fun encodeUrlData(data: String, charset: String): String =
    // Ref: https://stackoverflow.com/questions/31612065/java-net-urlencoderencode-for-whitespace
    URLEncoder.encode(data, charset).replace("+", "%20")

actual fun decodeUrlData(data: String, charset: String): String =
    URLDecoder.decode(data, charset)
