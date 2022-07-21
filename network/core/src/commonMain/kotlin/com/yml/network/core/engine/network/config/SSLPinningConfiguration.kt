package com.yml.network.core.engine.network.config

/**
 * Class for holding configuration for SSL pinning
 */
class SSLPinningConfiguration {

    /**
     * Field to store the list of certificate data with the respective domain name.
     */
    val sslCertificates: Map<String, List<String>>
        get() = _sslCertificates

    private val _sslCertificates = HashMap<String, MutableList<String>>()

    /**
     * Add the certificate content for a single domain.
     *
     * @param domainName domain name for which the certificate content should be added.
     * @param certificatesContent certificate content for the domain.
     *
     * @return [SSLPinningConfiguration]'s instance for builder pattern
     */
    fun add(domainName: String, vararg certificatesContent: String): SSLPinningConfiguration {
        val certificates = _sslCertificates.getOrPut(domainName) { mutableListOf() }
        certificatesContent.forEach(certificates::add)
        return this
    }
}
