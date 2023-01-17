package co.yml.network.core.engine.network

import co.yml.network.core.Resource
import co.yml.network.core.engine.network.config.NetworkEngineConfiguration
import co.yml.network.core.request.DataRequest
import co.yml.network.core.response.DataResponse
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
