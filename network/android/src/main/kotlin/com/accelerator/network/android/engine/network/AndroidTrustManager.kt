package com.accelerator.network.android.engine.network

import androidx.annotation.VisibleForTesting
import java.security.KeyStore
import javax.net.ssl.*

/**
 * [AndroidTrustManager] this class helps in reading the SSL certificates provided by the developers
 * and providing the required trust [X509TrustManager] and [SSLSocketFactory] for the network client.
 *
 * @param [androidCertificatePinning] is an interface for developer to provide required setup
 */
class AndroidTrustManager(private val androidCertificatePinning: AndroidCertificatePinning) {

    /**
     * [KeyStore] used to help for encrypted values
     */
    private var keyStore: KeyStore? = getKeyStore()

    /**
     * Returns [SSLSocketFactory] for the given [X509TrustManager]
     * This [SSLSocketFactory] is passed to Network client to handle socket connections.
     *
     * @param [trustManager] manager helping in providing the certificate for [SSLSocketFactory]
     * @return [SSLSocketFactory] returns the socket factory
     */
    fun getSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {

        val sslContext: SSLContext = SSLContext.getInstance("TLS")

        sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
        return sslContext.socketFactory
    }

    /**
     * Provides [X509TrustManager] for the generated trust managers
     *
     * @return [X509TrustManager]
     * @throws [Exception] if default [TrustManager] dose not exists
     */
    fun getTrustManager(): X509TrustManager {

        androidCertificatePinning.getTrustManager()?.let { return it }

        val trustManagers: Array<TrustManager> = getTrustManagers(keyStore)

        if (trustManagers.size == 1 || trustManagers[0] is X509TrustManager) {
            return trustManagers[0] as X509TrustManager
        } else {
            throw Exception("Unexpected default trust managers: ${trustManagers.contentToString()}")
        }

    }

    /**
     * Creates array of [TrustManager] for the given [KeyStore]
     *
     * @param [keyStore] cryptic system holding developers encrypted values
     * @return [Array] of [TrustManager]
     * @throws [Exception] throws exception if not able tot create [TrustManager]
     */
    private fun getTrustManagers(keyStore: KeyStore?): Array<TrustManager> {
        try {

            val trustManagerAlgorithm = androidCertificatePinning.getKeyAlgorithm()
                ?: TrustManagerFactory.getDefaultAlgorithm()

            val trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm)
            trustManagerFactory.init(keyStore)
            return trustManagerFactory.trustManagers
        } catch (exception: Exception) {
            throw Exception("${AndroidTrustManager::class.java.name}: Unable to create TrustManager from given KeyStore. $exception")
        }
    }

    /**
     * Generates [KeyStore] system for given keystore type, input stream of certificates and respective passwords
     *
     * @see <a href="https://developer.android.com/training/articles/keystore">https://developer.android.com/training/articles/keystore</a>
     *
     * @return [KeyStore]
     */
    @VisibleForTesting
    internal fun getKeyStore(): KeyStore {
        var keyStoreType = androidCertificatePinning.getKeyStoreType()
        if (keyStoreType.isNullOrEmpty()) {
            keyStoreType = KeyStore.getDefaultType()
        }
        try {
            val keyStore = KeyStore.getInstance(keyStoreType).apply {
                load(
                    androidCertificatePinning.getInputStream(),
                    androidCertificatePinning.getPassword()
                )
            }
            return keyStore
        } catch (exception: Exception) {
            throw Exception("${AndroidTrustManager::class.java.name}: Unable to read KeyStore from given InputStream. $exception")
        }
    }
}