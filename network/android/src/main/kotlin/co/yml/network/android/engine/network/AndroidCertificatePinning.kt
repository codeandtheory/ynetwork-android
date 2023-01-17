package co.yml.network.android.engine.network

import java.io.InputStream
import javax.net.ssl.X509TrustManager

/**
 * An interface for developers to provide the required values to generate SSLCertificate pinning
 */
interface AndroidCertificatePinning {

    /**
     * Mandatory input, with this developer need to provide an [InputStream]
     * for the physical certificate file stored in raw folders
     *
     * @return [InputStream] fo the certificate file
     */
    fun getInputStream(): InputStream

    /**
     * Type provided to generate specified KeyStore.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore">https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore</a>
     *
     * @return [String] KeyStore type specified
     */
    fun getKeyStoreType(): String? = null

    /**
     * Password given to read the protected SSL certificates
     */
    fun getPassword(): CharArray? = null

    /**
     * Type of algorithm used encrypt KeyStore values
     */
    fun getKeyAlgorithm(): String? = null

    /**
     * Developers can implement required custom TrustManager which will be passed to Network client
     * This implementation will have custom implementation to verify certificate trust
     */
    fun getTrustManager(): X509TrustManager? = null
}