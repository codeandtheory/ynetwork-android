package co.yml.chat.ui.rest.worker

import android.net.Uri
import androidx.work.Data
import androidx.work.workDataOf

private const val KEY_MESSAGE = "message"
private const val KEY_ATTACHMENT_PATH_URI = "attachmentPathUri"
private const val KEY_TOKEN = "token"
private const val KEY_REFRESH_TOKEN = "refreshToken"

data class MessageWorkerData(
    val message: String,
    val attachmentPathUri: Uri?,
    val token: String,
    val refreshToken: String
) {

    fun toData(): Data = workDataOf(
        KEY_MESSAGE to message,
        KEY_ATTACHMENT_PATH_URI to attachmentPathUri?.toString(),
        KEY_TOKEN to token,
        KEY_REFRESH_TOKEN to refreshToken
    )

    companion object {
        fun fromData(workerData: Data) = with(workerData) {
            val message = getString(KEY_MESSAGE)
                ?: throw IllegalArgumentException("Message is required in ${MessageWorkerData::class.simpleName}.")
            val attachmentPathUri = getString(KEY_ATTACHMENT_PATH_URI)?.let { Uri.parse(it) }
            val token = getString(KEY_TOKEN)
                ?: throw IllegalArgumentException("Token is required in ${MessageWorkerData::class.simpleName}.")
            val refreshToken = getString(KEY_REFRESH_TOKEN)
                ?: throw IllegalArgumentException("Token is required in ${MessageWorkerData::class.simpleName}.")
            return@with MessageWorkerData(message, attachmentPathUri, token, refreshToken)
        }
    }
}
