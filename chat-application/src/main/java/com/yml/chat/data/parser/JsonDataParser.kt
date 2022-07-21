package com.yml.chat.data.parser

import com.yml.network.core.parser.DataParser
import com.google.gson.GsonBuilder
import com.yml.chat.data.MessageContent
import com.yml.chat.data.MessageContentConstants
import kotlin.reflect.KClass

val messageContentAdapterFactory =
    RuntimeTypeAdapterFactory.of(MessageContent::class.java)
        .registerSubtype(MessageContent.TextContent::class.java, MessageContentConstants.TEXT)
        .registerSubtype(
            MessageContent.AttachmentContent::class.java,
            MessageContentConstants.ATTACHMENT
        )

class JsonDataParser : DataParser {
    private val gson =
        GsonBuilder().registerTypeAdapterFactory(messageContentAdapterFactory).create()

    override fun <DATA : Any> serialize(data: DATA): String = gson.toJson(data, data.javaClass)

    override fun <DATA : Any> deserialize(data: String, kClass: KClass<DATA>): DATA =
        gson.fromJson(data, kClass.java)
}
