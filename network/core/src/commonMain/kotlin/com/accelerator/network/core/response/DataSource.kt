package com.accelerator.network.core.response

/**
 * Source from which the has been fetched.
 */
enum class DataSource {
    /**
     * Represents that the data has been fetched from local Cache.
     */
    Cache,

    /**
     * Represents that the data has been fetched from Network i.e. from server.
     */
    Network
}
