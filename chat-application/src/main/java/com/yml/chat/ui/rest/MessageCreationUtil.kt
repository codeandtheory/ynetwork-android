package com.yml.chat.ui.rest

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.yml.network.core.MimeType
import com.yml.network.core.Resource
import com.yml.network.core.response.DataResponse
import com.yml.chat.UniqueIdGenerator
import com.yml.chat.data.AttachmentData
import com.yml.chat.data.ChatRepository
import com.yml.chat.data.MessageData
import com.yml.chat.ui.getFileName
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream

/**
 * Utility function to post new message.
 *
 * @param applicationContext Application Context for file operation.
 * @param uniqueIdGenerator Unique id Generate for unique file name generation.
 * @param repository [ChatRepository] for making network requests.
 * @param message content of text message to be posted.
 * @param attachmentPathUri File attachment path URI for new message.
 * @param token JWT auth token for authenticating the user.
 * @param refreshToken refresh token for JWT auth token.
 *
 * @return Flow containing the post message response.
 */
fun postNewMessage(
    applicationContext: Context,
    uniqueIdGenerator: UniqueIdGenerator,
    repository: ChatRepository,
    message: String,
    attachmentPathUri: Uri?,
    token: String,
    refreshToken: String
): Flow<Resource<DataResponse<MessageData>>> {
    val attachmentData = attachmentPathUri?.let { uri ->
        applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?.let { picturesDir ->
                val contentResolver = applicationContext.contentResolver
                val mimeType = contentResolver.getType(uri)
                val extension =
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

                val outputFile =
                    File(picturesDir, uniqueIdGenerator.generateId() + "." + extension)

                contentResolver.openInputStream(uri)?.use { contentInputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        contentInputStream.copyTo(outputStream)
                    }
                }
                AttachmentData(
                    getFileName(uri, contentResolver) ?: outputFile.name,
                    outputFile.absolutePath,
                    mimeType?.let { MimeType(it) } ?: MimeType.OCTET_STREAM)
            }
    }
    return repository.addMessage(
        message,
        attachmentData,
        token,
        refreshToken
    )
}
