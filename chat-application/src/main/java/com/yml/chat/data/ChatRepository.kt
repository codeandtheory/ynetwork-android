package com.yml.chat.data

import com.accelerator.network.core.Headers
import com.accelerator.network.core.MimeType
import com.accelerator.network.core.NetworkManager
import com.accelerator.network.core.Resource
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.FileRequestBody
import com.accelerator.network.core.request.MultiPartRequestBody
import com.accelerator.network.core.response.DataResponse
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

data class AttachmentData(val fileName: String, val filePath: String, val mimeType: MimeType)

class ChatRepository @Inject constructor(private val networkManager: NetworkManager) {

    fun addMessage(
        message: String,
        attachmentData: AttachmentData?,
        token: String,
        refreshToken: String
    ): Flow<Resource<DataResponse<MessageData>>> {
        val body = MultiPartRequestBody.Builder(MimeType.MULTIPART_FORM)
            .apply {
                addFormData(FormKeyConstants.CONTENT, message)
                addFormData(
                    FormKeyConstants.CREATED_AT,
                    Calendar.getInstance().time.time.toString()
                )
                attachmentData?.let {
                    addFormData(
                        FormKeyConstants.ATTACHMENTS,
                        it.fileName,
                        FileRequestBody(it.filePath, it.fileName, it.mimeType)
                    )
                }
            }
            .build()
        return networkManager.submit(
            DataRequest.post(UrlConstants.ADD_MESSAGE, MessageData::class, body)
                .setHeaders(
                    Headers()
                        .add(HeadersConstants.TOKEN, token)
                        .add(HeadersConstants.REFRESH_TOKEN, refreshToken)
                )
        ).asFlow()
    }
}
