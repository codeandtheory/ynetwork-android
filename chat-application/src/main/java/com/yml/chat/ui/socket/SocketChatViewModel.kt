package com.yml.chat.ui.socket

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.yml.chat.data.ChatSocketHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View Model for chat screen.
 */
@HiltViewModel
class SocketChatViewModel @Inject constructor(private val socketHelper: ChatSocketHelper) :
    ViewModel() {

    /**
     * Connect the socket with given auth tokens
     *
     * @param token Auth JWT token
     * @param refreshToken Token for refreshing Auth JWT token.
     *
     * @throws [IllegalArgumentException] in case the socket is already connected.
     */
    fun connectSocket(token: String, refreshToken: String) =
        socketHelper.connect(token, refreshToken)

    /**
     * Disconnect the socket.
     */
    fun disconnectSocket() = socketHelper.disconnect()

    /**
     * Get a [LiveData] listening to the change in online member count socket event.
     *
     * @return [LiveData] containing the online member count
     */
    fun getMemberOnlineLiveData() = socketHelper.getMemberOnlineLiveData()

    /**
     * Get a [LiveData] listening to the new Message event socket event.
     *
     * @return [LiveData] containing the message data.
     */
    fun getNewMessageLiveData() = socketHelper.getNewMessageLiveData()

    /**
     * Post new message on the socket.
     *
     * @param message content of the new message.
     */
    fun postNewMessage(message: String) = socketHelper.postNewMessage(message)
}