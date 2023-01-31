package co.yml.network.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import co.yml.network.core.NetworkCall
import co.yml.network.core.Resource
import co.yml.network.core.response.DataResponse
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Execute the network request in async manner and provides the response consumption as a [LiveData]<[Resource]<[DataResponse]<[RESPONSE]>>>
 *
 * @return [LiveData]<[Resource]<[DataResponse]<[RESPONSE]>>> containing the response data.
 */
fun <RESPONSE> NetworkCall<RESPONSE>.asLiveData(context: CoroutineContext = Dispatchers.IO) =
    asFlow().asLiveData(context)
