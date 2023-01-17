package co.yml.network.core

import co.yml.network.core.config.NetworkManagerConfiguration
import co.yml.network.core.config.PlatformConfiguration
import co.yml.network.core.constants.THREAD_COUNT_NOT_DEFINED
import co.yml.network.core.constants.TIMEOUT_NOT_DEFINED
import co.yml.network.core.engine.cache.CacheEngine
import co.yml.network.core.engine.network.NetworkEngine
import co.yml.network.core.engine.network.config.SSLPinningConfiguration
import co.yml.network.core.interceptors.Interceptor
import co.yml.network.core.parser.DataParserFactory
import co.yml.network.core.proxy.ProxyConfig
import co.yml.network.core.request.CachePolicy
import co.yml.network.core.request.FileTransferProgressCallback

class NetworkManagerBuilder(
    private val networkEngine: NetworkEngine,
    private val dataParserFactory: DataParserFactory
) {

    private var basePath: String? = null
    private var cacheEngine: CacheEngine? = null
    private var defaultCachePolicy: CachePolicy = CachePolicy.CacheFailsThenNetwork
    private var fileTransferProgressCallback: FileTransferProgressCallback? = null
    private var headers: Headers? = null
    private var interceptors: List<Interceptor>? = null
    private var platformConfig: Map<String, PlatformConfiguration>? = null
    private var proxyConfig: ProxyConfig? = null
    private var shouldFollowRedirect: Boolean = true
    private var sslPinningConfiguration: SSLPinningConfiguration? = null
    private var threadCount: Int = THREAD_COUNT_NOT_DEFINED
    private var timeout: Long = TIMEOUT_NOT_DEFINED


    /**
     * Set the provided [basePath] field in the builder.
     * @param basePath root path which will be used with relative URL for creating a full URL.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setBasePath(basePath: String?) = apply { this.basePath = basePath }

    /**
     * Set the provided [cacheEngine] field in the builder.
     * @param cacheEngine for making data requests to local cache.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setCacheEngine(cacheEngine: CacheEngine?) = apply { this.cacheEngine = cacheEngine }

    /**
     * Set the provided [defaultCachePolicy] field in the builder.
     * @param defaultCachePolicy default [CachePolicy] to use when cachePolicy not specified in [com.accelerator.network.core.request.DataRequest].
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setDefaultCachePolicy(defaultCachePolicy: CachePolicy) =
        apply { this.defaultCachePolicy = defaultCachePolicy }

    /**
     * Set the provided [fileTransferProgressCallback] field in the builder.
     * @param fileTransferProgressCallback callback for file upload progress.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setFileTransferProgressCallback(fileTransferProgressCallback: FileTransferProgressCallback?) =
        apply { this.fileTransferProgressCallback = fileTransferProgressCallback }

    /**
     * Set the provided [headers] field in the builder.
     * @param headers which needs to be sent with each request.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setHeaders(headers: Headers?) = apply { this.headers = headers }

    /**
     * Set the provided [interceptors] field in the builder.
     * @param interceptors list of interceptor implemented.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setInterceptors(interceptors: List<Interceptor>?) =
        apply { this.interceptors = interceptors }

    /**
     * Set the provided [platformConfig] field in the builder.
     * @param platformConfig a Map of [PlatformConfiguration] w.r.t. the platform name.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setPlatformConfig(platformConfig: Map<String, PlatformConfiguration>?) =
        apply { this.platformConfig = platformConfig }

    /**
     * Set the provided [proxyConfig] field in the builder.
     * @param proxyConfig configuration for proxy service.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setProxyConfig(proxyConfig: ProxyConfig?) = apply { this.proxyConfig = proxyConfig }

    /**
     * Set the provided [shouldFollowRedirect] field in the builder.
     * @param shouldFollowRedirect specify whether the HTTP redirection should be followed or not.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setShouldFollowRedirect(shouldFollowRedirect: Boolean) =
        apply { this.shouldFollowRedirect = shouldFollowRedirect }

    /**
     * Set the provided [sslPinningConfiguration] field in the builder.
     * @param sslPinningConfiguration configuration for SSL pinning.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setSslPinningConfiguration(sslPinningConfiguration: SSLPinningConfiguration?) =
        apply { this.sslPinningConfiguration = sslPinningConfiguration }

    /**
     * Set the provided [threadCount] field in the builder.
     * @param threadCount number of threads to use while making parallel network requests.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setThreadCount(threadCount: Int) = apply { this.threadCount = threadCount }

    /**
     * Set the provided [timeout] field in the builder.
     * @param timeout default timeout while making network request in milliseconds.
     * @return [NetworkManagerBuilder]'s current instance for builder pattern.
     */
    fun setTimeout(timeout: Long) = apply { this.timeout = timeout }

    /**
     * Build and return the [NetworkManager]'s instance
     *
     * @return [NetworkManager] generated instance
     */
    fun build() = NetworkManager(
        NetworkManagerConfiguration(
            networkEngine,
            dataParserFactory,
            basePath,
            cacheEngine,
            platformConfig,
            sslPinningConfiguration,
            shouldFollowRedirect,
            headers,
            proxyConfig,
            timeout,
            threadCount,
            defaultCachePolicy,
            interceptors,
            fileTransferProgressCallback
        )
    )
}
