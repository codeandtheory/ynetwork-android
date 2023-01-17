package co.yml.chat.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import co.yml.chat.BASE_URL
import co.yml.chat.data.parser.JsonDataParser
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.engineio.client.Transport
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

private data class NewMessageData(val content: Array<MessageContent>, val createdAt: Date) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewMessageData

        if (!content.contentEquals(other.content)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

private data class MemberOnlineCount(val count: Int)

class ChatSocketHelper @Inject constructor(private val jsonParser: JsonDataParser) {

    private val socket = IO.socket(BASE_URL)

    /**
     * Connect to the socket server with given auth headers.
     *
     * @param token Auth JWT Token for authentication
     * @param refreshToken Token for refreshing Auth JWT Token.
     *
     * @throws [IllegalArgumentException] in case the socket is already connected.
     */
    fun connect(token: String, refreshToken: String) {
        if (socket.connected()) {
            throw IllegalStateException("Socket already connected")
        }

        // Connect socket
        socket.connect()

        // Send auth headers
        // Ref: https://stackoverflow.com/questions/33127213/socket-io-android-library-authentication
        socket.io().on(Manager.EVENT_TRANSPORT) { args ->
            val transport = args[0] as Transport

            transport.on(Transport.EVENT_REQUEST_HEADERS) { requestArgs ->
                @Suppress("UNCHECKED_CAST") val headers =
                    requestArgs[0] as MutableMap<String, List<String>>
                // modify request headers
                headers[HeadersConstants.TOKEN] = listOf(token)
                headers[HeadersConstants.REFRESH_TOKEN] = listOf(refreshToken)
            }
        }
    }

    /**
     * Disconnect with the socket server.
     */
    fun disconnect() {
        socket.disconnect()
    }

    /**
     * Get a [LiveData] listening to the change in online member count socket event.
     *
     * @return [LiveData] containing the online member count
     */
    fun getMemberOnlineLiveData(): LiveData<Int> {
        val liveData = MutableLiveData<Int>()
        socket.on(SOCKET_EVENT_CHANGE_MEMBER_ONLINE) { eventData ->
            val data = jsonParser.deserialize(eventData[0].toString(), MemberOnlineCount::class)
            liveData.postValue(data.count)
        }
        return liveData
    }

    /**
     * Get a [LiveData] listening to the new Message event socket event.
     *
     * @return [LiveData] containing the message data.
     */
    fun getNewMessageLiveData(): LiveData<MessageData> {
        val liveData = MutableLiveData<MessageData>()
        socket.on(SOCKET_EVENT_NEW_MESSAGE) { eventData ->
            val messageData = jsonParser.deserialize(eventData[0].toString(), MessageData::class)
            liveData.postValue(messageData)
        }
        return liveData
    }

    /**
     * Post new message on the socket.
     *
     * @param message content of the new message.
     */
    fun postNewMessage(message: String) {
        val newMessageData = NewMessageData(
            arrayOf(MessageContent.TextContent(message)),
            Calendar.getInstance().time
        )
        socket.emit(
            SOCKET_EVENT_CREATE_MESSAGE,
            JSONObject(jsonParser.serialize(newMessageData)),
            UUID.randomUUID().toString()
        )
    }
}
