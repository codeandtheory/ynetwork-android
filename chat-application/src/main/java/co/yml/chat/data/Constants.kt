package co.yml.chat.data

object HeadersConstants {
    const val TOKEN = "token"
    const val REFRESH_TOKEN = "refreshToken"
}

object UrlConstants {
    const val LOGIN = "/login"
    const val ADD_MESSAGE = "/message/add"
}

object FormKeyConstants {
    const val CONTENT = "content"
    const val CREATED_AT = "createdAt"
    const val ATTACHMENTS = "attachments[]"
}

object MessageContentConstants {
    const val TEXT = "text"
    const val ATTACHMENT = "attachment"
}
