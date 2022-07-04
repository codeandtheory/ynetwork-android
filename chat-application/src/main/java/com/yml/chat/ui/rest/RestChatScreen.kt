package com.yml.chat.ui.rest

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.accelerator.network.core.MimeType
import com.accelerator.network.core.Resource
import com.accelerator.network.core.request.FileTransferInfo
import com.yml.chat.R
import com.yml.chat.TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_PROGRESS
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_TITLE
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_LIST
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_INPUT
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_CONTAINER
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_CONTENT
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_FROM
import com.yml.chat.TAG_REST_CHAT_SCREEN_SEND_MESSAGE
import com.yml.chat.data.MessageContent
import com.yml.chat.data.MessageData

private fun bytesToReadableSize(bytesSize: Long): String {
    var convertedSize = bytesSize.toFloat()
    var notation = "bytes"

    if (convertedSize > 1024) {
        convertedSize /= 1024
        notation = "KB"
    }
    if (convertedSize > 1024) {
        convertedSize /= 1024
        notation = "MB"
    }
    if (convertedSize > 1024) {
        convertedSize /= 1024
        notation = "GB"
    }
    return "${String.format("%.2f", convertedSize)} $notation"
}

@Composable
private fun ChatMessage(data: MessageData) {
    Column(
        modifier = Modifier
            .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_CONTAINER)
            .padding(dimensionResource(id = R.dimen.spacing_1))
            .fillMaxWidth()
    ) {
        Text(
            text = data.from,
            modifier = Modifier.testTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_FROM)
        )
        Column(
            modifier = Modifier
                .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_CONTENT)
                .fillMaxWidth()
                .padding(top = dimensionResource(id = R.dimen.spacing_1))
        ) {
            data.content.forEach {
                when (it) {
                    is MessageContent.TextContent -> {
                        Text(text = it.message)
                    }
                    is MessageContent.AttachmentContent -> {
                        Text(text = it.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun OngoingAttachment(data: Pair<FileTransferInfo, Resource<Long>>) {
    val (fileInfo, bytesTransferredResource) = data

    val progress = when (bytesTransferredResource) {
        is Resource.Loading ->
            (bytesTransferredResource.data ?: 0).toFloat() / fileInfo.fileSizeInBytes
        is Resource.Success -> 1f
        is Resource.Error -> 0f
    }
    val fileStatus = when (bytesTransferredResource) {
        is Resource.Loading -> {
            if (bytesTransferredResource.data == fileInfo.fileSizeInBytes) stringResource(id = R.string.file_upload_processing)
            else stringResource(
                R.string.file_upload_loading,
                bytesToReadableSize(bytesTransferredResource.data ?: 0),
                bytesToReadableSize(fileInfo.fileSizeInBytes),
                String.format("%.2f", progress * 100)
            )
        }
        is Resource.Success -> stringResource(R.string.file_upload_success)
        is Resource.Error -> stringResource(R.string.file_upload_error)
    }
    val backgroundColorRes = when (bytesTransferredResource) {
        is Resource.Loading -> R.color.loading
        is Resource.Success -> R.color.success
        is Resource.Error -> R.color.error
    }
    val textColorRes = R.color.white

    Column(
        modifier = Modifier
            .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM)
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.spacing_1))
            .background(
                colorResource(id = backgroundColorRes),
                RoundedCornerShape(dimensionResource(id = R.dimen.spacing_2))
            )
            .padding(dimensionResource(id = R.dimen.spacing_2))
    ) {
        Text(
            text = "${fileInfo.fileName} (${fileInfo.mimeType})",
            color = colorResource(id = textColorRes),
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.spacing_1))
                .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_TITLE)
        )
        Text(
            text = fileStatus,
            color = colorResource(id = textColorRes),
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.spacing_1))
                .fillMaxWidth()
                .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_PROGRESS)
        )
        LinearProgressIndicator(
            progress = progress,
            color = colorResource(id = R.color.white),
            backgroundColor = colorResource(id = R.color.black),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RestChatScreenUI(
    newMessageLiveData: LiveData<MessageData>,
    fileUploadStateList: SnapshotStateList<Pair<FileTransferInfo, Resource<Long>>>,
    postMessageCallback: (message: String, attachmentPath: Uri?) -> Unit
) {
    val messageContentList = remember { mutableStateListOf<MessageData>() }
    val fileUploadList = remember { fileUploadStateList }

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

    var attachmentPath by rememberSaveable { mutableStateOf<Uri?>(null) }
    val pickPictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { fileUri ->
        attachmentPath = fileUri
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (attachmentList, messageList, messageInput, attachment, sendMessage) = createRefs()

        LazyColumn(modifier = Modifier
            .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_LIST)
            .constrainAs(attachmentList) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }) {
            items(fileUploadList) { item ->
                OngoingAttachment(data = item)
            }
        }

        LazyColumn(
            modifier = Modifier
                .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST)
                .constrainAs(messageList) {
                    top.linkTo(attachmentList.bottom)
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
            modifier = Modifier
                .testTag(TAG_REST_CHAT_SCREEN_MESSAGE_INPUT)
                .constrainAs(messageInput) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(attachment.start)
                    start.linkTo(parent.start)
                    width = Dimension.fillToConstraints
                },
            // Not required, just a demo of how to select inputType.
            // inputType => keyboardOptions.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        IconButton(
            onClick = { pickPictureLauncher.launch("*/*") },
            modifier = Modifier
                .testTag(TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT)
                .constrainAs(attachment) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(sendMessage.start)
                },
            enabled = attachmentPath == null
        ) {
            Icon(
                imageVector = Icons.Filled.AttachFile,
                ""
            )
        }

        Button(onClick = { postMessageCallback(message, attachmentPath) },
            modifier = Modifier
                .testTag(TAG_REST_CHAT_SCREEN_SEND_MESSAGE)
                .constrainAs(sendMessage) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }) {
            Text(text = stringResource(id = R.string.send))
        }
    }
}

@Composable
fun RestChatScreen(
    restChatViewModel: RestChatViewModel,
    token: String,
    refreshToken: String,
    postInBackground: Boolean
) {
    RestChatScreenUI(
        restChatViewModel.postMessageLiveData,
        restChatViewModel.fileTransferList
    ) { message, attachmentUri ->
        if (postInBackground) restChatViewModel.postNewMessageInBackground(
            message,
            attachmentUri,
            token,
            refreshToken
        )
        else restChatViewModel.postNewMessage(message, attachmentUri, token, refreshToken)
    }
}

@Preview(showBackground = true, name = "Chat screen using REST API")
@Composable
private fun RestChatScreenPreview() {
    val mbToBytes = (1024 * 1024).toLong()
    val fileInfo = FileTransferInfo(
        "/home/1234567890.txt",
        "abc.txt",
        MimeType.TEXT_PLAIN,
        true,
        500 * mbToBytes
    )

    val messageLiveData = MutableLiveData<MessageData>()
    messageLiveData.value = MessageData(
        from = "ABC XYZ",
        content = arrayOf(
            MessageContent.TextContent("Hello world"),
            MessageContent.AttachmentContent("greetings.mp4", "media/mp4")
        )
    )
    RestChatScreenUI(
        messageLiveData,
        SnapshotStateList<Pair<FileTransferInfo, Resource<Long>>>().apply {
            add(fileInfo to Resource.Success(500 * mbToBytes))
            add(fileInfo to Resource.Loading(500 * mbToBytes))
            add(fileInfo to Resource.Loading(250 * mbToBytes))
            add(fileInfo to Resource.Error())
        }
    ) { _, _ -> }
}
