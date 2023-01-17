package co.yml.chat.data

import co.yml.network.core.Headers
import co.yml.network.core.MimeType
import co.yml.network.core.NetworkManager
import co.yml.network.core.Resource
import co.yml.network.core.request.DataRequest
import co.yml.network.core.request.FileRequestBody
import co.yml.network.core.request.MultiPartRequestBody
import co.yml.network.core.response.DataResponse
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
