package com.yml.network.core

import com.yml.network.core.response.DataResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Execute the network request in async manner in the provided [coroutineScope] and provide the callbacks in specified [observer].
 *
 * @param observer A callback function to which the updates needs to be provided.
 * @param coroutineScope [CoroutineScope] in which the network request needs to be executed.
 */
@JvmOverloads
fun <RESPONSE> NetworkCall<RESPONSE>.addObserver(
    observer: (Resource<DataResponse<RESPONSE>>) -> Unit,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    coroutineScope.launch { asFlow().collect { observer(it) } }
}

/**
 * Execute the network request by blocking current thread/coroutine scope and provide the response ([Resource]<[DataResponse]<[RESPONSE]>>) for consumption.
 *
 * @return [Resource]<[DataResponse]<[RESPONSE]>> containing the response data.
 * @throws [InterruptedException] when the current execution thread is interrupted.
 */
@Throws(InterruptedException::class)
fun <RESPONSE> NetworkCall<RESPONSE>.execute(): Resource<DataResponse<RESPONSE>> =
    runBlocking { asList().last() }
