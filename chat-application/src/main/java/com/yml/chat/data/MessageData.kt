package com.yml.chat.data

data class MessageData(val from: String, val content: Array<MessageContent>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageData

        if (from != other.from) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
