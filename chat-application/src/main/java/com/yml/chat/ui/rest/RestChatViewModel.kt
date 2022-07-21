package com.yml.chat.ui.rest

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yml.network.core.Resource
import com.yml.network.core.request.FileTransferInfo
import com.yml.chat.FileTransferManager
import com.yml.chat.UniqueIdGenerator
import com.yml.chat.data.ChatRepository
import com.yml.chat.data.MessageData
import com.yml.chat.data.parser.JsonDataParser
import com.yml.chat.ui.rest.worker.MESSAGE_WORKER_RESULT_KEY
import com.yml.chat.ui.rest.worker.MessageWorker
import com.yml.chat.ui.rest.worker.MessageWorkerData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TAG = RestChatViewModel::class.simpleName

/**
 * View Model for chat screen.
 */
@HiltViewModel
class RestChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    fileTransferManager: FileTransferManager,
    private val uniqueIdGenerator: UniqueIdGenerator,
    application: Application,
    private val jsonDataParser: JsonDataParser
) : AndroidViewModel(application) {

    val postMessageLiveData = MediatorLiveData<MessageData>()
    val fileTransferList = mutableStateListOf<Pair<FileTransferInfo, Resource<Long>>>()

    init {
        fileTransferManager.fileTransferCallback.add { fileInfo, transferredBytesResource ->
            val existingEntryIndex =
                fileTransferList.indexOfFirst { it.first.filePath == fileInfo.filePath }
            if (existingEntryIndex == -1) {
                fileTransferList.add(fileInfo to transferredBytesResource)
            } else {
                fileTransferList[existingEntryIndex] = fileInfo to transferredBytesResource
            }
        }

    }

    fun postNewMessageInBackground(
        message: String,
        attachmentPathUri: Uri?,
        token: String,
        refreshToken: String
    ) {
        val workRequest = OneTimeWorkRequestBuilder<MessageWorker>()
            .setInputData(
                MessageWorkerData(message, attachmentPathUri, token, refreshToken).toData()
            )
            .build()
        val appContext = getApplication<Application>().applicationContext

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueue(workRequest)

        postMessageLiveData.addSource(workManager.getWorkInfoByIdLiveData(workRequest.id)) { workInfo ->
            workInfo.outputData.getString(MESSAGE_WORKER_RESULT_KEY)?.let { workerResult ->
                val messageData = jsonDataParser.deserialize(workerResult, MessageData::class)
                postMessageLiveData.postValue(messageData)
            }
        }
    }

    fun postNewMessage(
        message: String,
        attachmentPathUri: Uri?,
        token: String,
        refreshToken: String
    ) {
        viewModelScope.launch {
            launch(Dispatchers.IO) {
                postNewMessage(
                    getApplication<Application>().applicationContext,
                    uniqueIdGenerator,
                    repository,
                    message,
                    attachmentPathUri,
                    token,
                    refreshToken
                ).collect {
                    when (it) {
                        is Resource.Loading -> it.data?.body?.let(postMessageLiveData::postValue)
                        is Resource.Success -> it.data.body?.let(postMessageLiveData::postValue)
                        is Resource.Error ->
                            Log.e(TAG, "Error occurred while making network request", it.error)
                    }
                }
            }
        }
    }
}
