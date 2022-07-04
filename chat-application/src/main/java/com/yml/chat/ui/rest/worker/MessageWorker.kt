package com.yml.chat.ui.rest.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.accelerator.network.core.Resource
import com.yml.chat.UniqueIdGenerator
import com.yml.chat.data.ChatRepository
import com.yml.chat.data.parser.JsonDataParser
import com.yml.chat.ui.rest.postNewMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext

const val MESSAGE_WORKER_RESULT_KEY = "messageWorkerResult"

/**
 * Worker for posting a message using WorkManager
 */
@HiltWorker
class MessageWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ChatRepository,
    private val uniqueIdGenerator: UniqueIdGenerator,
    private val jsonDataParser: JsonDataParser
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val messageWorkerData = MessageWorkerData.fromData(inputData)
        val result = postNewMessage(
            appContext,
            uniqueIdGenerator,
            repository,
            messageWorkerData.message,
            messageWorkerData.attachmentPathUri,
            messageWorkerData.token,
            messageWorkerData.refreshToken
        )
            .last()
            .let {
                when (it) {
                    is Resource.Loading -> it.data?.body
                    is Resource.Success -> it.data.body
                    is Resource.Error -> null
                }
            }

        return@withContext result?.let {
            Result.success(
                Data.Builder()
                    .putString(MESSAGE_WORKER_RESULT_KEY, jsonDataParser.serialize(it))
                    .build()
            )
        }
            ?: Result.failure()
    }
}
