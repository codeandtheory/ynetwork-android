package com.accelerator.network.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.accelerator.network.core.NetworkCall
import com.accelerator.network.core.Resource
import com.accelerator.network.core.response.DataResponse
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Execute the network request in async manner and provides the response consumption as a [LiveData]<[Resource]<[DataResponse]<[RESPONSE]>>>
 *
 * @return [LiveData]<[Resource]<[DataResponse]<[RESPONSE]>>> containing the response data.
 */
fun <RESPONSE> NetworkCall<RESPONSE>.asLiveData(context: CoroutineContext = Dispatchers.IO) =
    asFlow().asLiveData(context)
