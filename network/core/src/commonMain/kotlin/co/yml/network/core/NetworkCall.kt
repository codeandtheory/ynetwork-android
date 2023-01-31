package co.yml.network.core

import co.yml.network.core.response.DataResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * Class for allowing developers to consume the response according to their needs.
 */
class NetworkCall<RESPONSE>(private val flow: Flow<Resource<DataResponse<RESPONSE>>>) {

    /**
     * Execute the network request in async manner and provides the response consumption as a [Flow]<[Resource]<[DataResponse]<[RESPONSE]>>>
     *
     * @return [Flow]<[Resource]<[DataResponse]<[RESPONSE]>>> containing the response data.
     */
    fun asFlow() = flow

    /**
     * Execute the network request in async manner and provides the response consumption as a [List]<[Resource]<[DataResponse]<[RESPONSE]>>>
     *
     * @return [List]<[Resource]<[DataResponse]<[RESPONSE]>>> containing the response data.
     */
    suspend fun asList(destination: MutableList<Resource<DataResponse<RESPONSE>>> = ArrayList()) =
        flow.toList(destination)

    /**
     * Execute the network request in async manner and provides the response consumption as a [Deferred]<[Resource]<[DataResponse]<[RESPONSE]>>>
     *
     * @param coroutineScope [CoroutineScope] in which the task needs to be executed.
     *
     * @return [Deferred]<[Resource]<[DataResponse]<[RESPONSE]>>> which can be used to await for response data.
     */
    fun asAsync(coroutineScope: CoroutineScope): Deferred<Resource<DataResponse<RESPONSE>>> =
        coroutineScope.async { asList().last() }

}
