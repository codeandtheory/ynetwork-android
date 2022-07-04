package com.yml.chat.ui.socket

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.yml.chat.R
import com.yml.chat.data.MessageContent
import com.yml.chat.data.MessageData

@Composable
private fun ChatMessage(data: MessageData) {
    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.spacing_1))
            .fillMaxWidth()
    ) {
        Text(text = data.from)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(id = R.dimen.spacing_1))
        ) {
            data.content.forEach {
                when (it) {
                    is MessageContent.TextContent -> {
                        Text(text = it.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocketChatScreenUI(
    memberOnlineLiveData: LiveData<Int>,
    newMessageLiveData: LiveData<MessageData>,
    postMessageCallback: (message: String) -> Unit
) {
    val messageContentList = remember { mutableStateListOf<MessageData>() }

    // Ref: https://developer.android.com/jetpack/compose/side-effects#disposableeffect
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = Observer<MessageData> {
            messageContentList.add(it)
        }
        newMessageLiveData.observe(lifecycleOwner, observer)

        onDispose {
            newMessageLiveData.removeObserver(observer)
        }
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (onlineCount, messageList, messageInput, sendMessage) = createRefs()

        val onlineCountValue by memberOnlineLiveData.observeAsState()

        Text(text = "${onlineCountValue ?: 0} users online",
            modifier = Modifier.constrainAs(onlineCount) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
            })

        LazyColumn(modifier = Modifier.constrainAs(messageList) {
            top.linkTo(onlineCount.bottom)
            bottom.linkTo(messageInput.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }) {
            items(messageContentList) { item ->
                ChatMessage(data = item)
            }
        }

        var message by rememberSaveable { mutableStateOf("") }
        TextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(text = stringResource(id = R.string.input_message)) },
            modifier = Modifier.constrainAs(messageInput) {
                bottom.linkTo(parent.bottom)
                end.linkTo(sendMessage.start)
                start.linkTo(parent.start)
                width = Dimension.fillToConstraints
            },
            // Not required, just a demo of how to select inputType.
            // inputType => keyboardOptions.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Button(onClick = {
            postMessageCallback(message)
        },
            modifier = Modifier.constrainAs(sendMessage) {
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }) {
            Text(text = stringResource(id = R.string.send))
        }
    }
}

@Composable
fun SocketChatScreen(socketChatViewModel: SocketChatViewModel, token: String, refreshToken: String) {
    DisposableEffect(socketChatViewModel) {
        socketChatViewModel.connectSocket(token, refreshToken)
        onDispose {
            socketChatViewModel.disconnectSocket()
        }
    }
    SocketChatScreenUI(
        memberOnlineLiveData = socketChatViewModel.getMemberOnlineLiveData(),
        newMessageLiveData = socketChatViewModel.getNewMessageLiveData(),
        postMessageCallback = socketChatViewModel::postNewMessage
    )
}


@Preview
@Composable
private fun SocketChatScreenPreview() {
    SocketChatScreenUI(
        memberOnlineLiveData = MutableLiveData(5),
        newMessageLiveData = MutableLiveData<MessageData>().apply {
            setValue(MessageData("From1", arrayOf(MessageContent.TextContent("Hey1"))))
        },
        postMessageCallback = {}
    )
}
