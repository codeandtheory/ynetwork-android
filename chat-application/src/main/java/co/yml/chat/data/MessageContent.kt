package co.yml.chat.data

import com.google.gson.annotations.SerializedName

sealed class MessageContent {
    data class TextContent(@SerializedName("content") val message: String) : MessageContent()
    data class AttachmentContent(
        @SerializedName("content") val fileName: String,
        @SerializedName("contentType") val contentType: String
    ) : MessageContent()
}
