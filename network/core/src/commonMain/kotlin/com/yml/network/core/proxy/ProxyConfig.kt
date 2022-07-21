package com.yml.network.core.proxy

/**
 * Enum class to mention type of Proxy.
 * This is a shadow class of proxy types to support multiplatform proxy config.
 *
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/net/Proxy.Type.html">https://docs.oracle.com/javase/7/docs/api/java/net/Proxy.Type.html</a>
 */
enum class ProxyType {
    DIRECT, //Represents a direct connection, or the absence of a proxy.
    HTTP, //Represents proxy for high level protocols such as HTTP or FTP.
    SOCKS //Represents a SOCKS (V4 or V5) proxy.
}

/**
 * Config data class holding variables required for creating proxy servers
 *
 * @property proxyType [ProxyType] type of proxy server we want to create
 * @property proxyUrl Url on which proxy server is configured
 * @property proxyPort Port of the configured proxy server
 */
data class ProxyConfig(
    val proxyType: ProxyType,
    val proxyUrl: String,
    val proxyPort: Int
)