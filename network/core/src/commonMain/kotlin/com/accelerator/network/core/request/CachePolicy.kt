package com.accelerator.network.core.request

/**
 * Cache policy to determine from which source the data should be fetched.
 */
enum class CachePolicy {
    /**
     * Policy for fetching the data from Cache only
     */
    CacheOnly,

    /**
     * Policy for fetching the data from Network only and should avoid the Cache.
     */
    NetworkOnly,

    /**
     * Policy for fetching the data from Cache first. In case the data request from cache fails
     * i.e. the Cache does not contains the required data then fetch the data from Network.
     */
    CacheFailsThenNetwork,

    /**
     * Policy for fetching the data from both Cache and Network.
     */
    CacheAndNetworkParallel
}
