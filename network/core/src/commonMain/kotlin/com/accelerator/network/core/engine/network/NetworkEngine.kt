package com.accelerator.network.core.engine.network

import com.accelerator.network.core.Resource
import com.accelerator.network.core.engine.network.config.NetworkEngineConfiguration
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.response.DataResponse
import kotlinx.coroutines.flow.Flow

/**
 * This class/interface is responsible for making the network requests
 */
interface NetworkEngine {

    /**
     * Initialize the [NetworkEngine] with the configuration.
     *
     * @param config a [NetworkEngineConfiguration] to initialize the [NetworkEngine]
     */
    fun init(config: NetworkEngineConfiguration)

    /**
     * Submit a data request to the [NetworkEngine] to get a data response.
     *
     * @param request a [DataRequest] containing the request data.
     *
     * @return [Flow] containing the response data.
     */
    fun submit(request: DataRequest<String>): Flow<Resource<DataResponse<String>>>
}
