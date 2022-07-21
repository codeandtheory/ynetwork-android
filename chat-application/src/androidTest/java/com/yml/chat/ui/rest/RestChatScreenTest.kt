package com.yml.chat.ui.rest

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Environment
import androidx.activity.viewModels
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import com.accelerator.network.android.engine.network.body.SEGMENT_SIZE
import com.yml.network.core.Headers
import com.yml.network.core.MimeType
import com.yml.network.core.Resource
import com.yml.network.core.constants.HeadersConstants
import com.yml.network.core.engine.network.demo.DemoNetworkEngine
import com.yml.network.core.engine.network.demo.FileTransferProgressCallbackHelper
import com.yml.network.core.request.FileTransferInfo
import com.yml.network.core.request.Method
import com.yml.network.core.response.DataResponse
import com.yml.network.core.response.DataSource
import com.yml.network.core.response.HttpStatusCode
import com.yml.chat.ChatActivity
import com.yml.chat.UniqueIdGenerator
import com.yml.chat.actions.ui.rest.RestChatScreenActions
import com.yml.chat.assertions.ui.rest.RestChatScreenAssertions
import com.yml.chat.data.MessageContent
import com.yml.chat.data.MessageData
import com.yml.chat.data.parser.JsonDataParser
import com.yml.chat.ui.theme.AcceleratorTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

private const val MOCK_JWT_TOKEN = "JWT Token"
private const val MOCK_JWT_REFRESH_TOKEN = "JWT Refresh Token"

private const val FILE_NAME = "testData.txt"
private val TEST_DATA = "A".repeat(SEGMENT_SIZE.toInt())

@HiltAndroidTest
class RestChatScreenTest {

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<ChatActivity>()

    @get:Rule(order = 3)
    var tempDir = TemporaryFolder()

    @Inject
    lateinit var uniqueIdGenerator: UniqueIdGenerator

    @Inject
    lateinit var demoNetworkEngine: DemoNetworkEngine

    @Inject
    lateinit var jsonDataParser: JsonDataParser

    @Before
    fun setup() {
        hiltRule.inject()

        Intents.init()
        composeTestRule.setContent {
            AcceleratorTheme {
                RestChatScreen(
                    restChatViewModel = composeTestRule.activity.viewModels<RestChatViewModel>().value,
                    token = MOCK_JWT_TOKEN,
                    refreshToken = MOCK_JWT_REFRESH_TOKEN
                )
            }
        }
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun verifyMessageSend(): Unit = runBlocking {
        val from = "user@gmail.com"
        val greetings = "Hello world!"
        val messageData = MessageData(from, arrayOf(MessageContent.TextContent(greetings)))
        demoNetworkEngine.on(
            Method.POST,
            "/message/add",
            createSuccessResponse(jsonDataParser.serialize(messageData))
        )

        val assertions = RestChatScreenAssertions(composeTestRule)
        val actions = RestChatScreenActions(composeTestRule)

        assertions.verifyScreenDisplayed()
        actions.typeMessage(greetings)
        actions.tapOnSendButton()

        // Wait for network request to settle.
        delay(100)

        assertions.verifyAttachmentListItemCount(0)
        assertions.verifyMessageListItemCount(1)
        assertions.verifyMessageListItem(0, messageData)
    }

    @Test
    fun verifyMessageSendWithAttachment(): Unit = runBlocking {
        val greetings = "Hello world!"
        val responseFlow = MutableStateFlow<Resource<DataResponse<String>>>(Resource.Loading())
        demoNetworkEngine.on(Method.POST, "/message/add", responseFlow)

        val testFile = createMockFileSelection()
        val copiedFile = File(
            composeTestRule.activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            uniqueIdGenerator.generateId() + ".txt"
        )

        val fileHelper = FileTransferProgressCallbackHelper()
        demoNetworkEngine.onFile(copiedFile.absolutePath, fileHelper)

        val assertions = RestChatScreenAssertions(composeTestRule)
        val actions = RestChatScreenActions(composeTestRule)

        assertions.verifyScreenDisplayed()
        actions.typeMessage(greetings)

        assertions.verifyAttachmentButtonEnabled(true)

        actions.tapOnAttachmentButton()
        assertions.verifyAttachmentButtonEnabled(false)

        actions.tapOnSendButton()
        delay(600)  // Wait for 600ms for file copy operation to be completed.

        // Verify the ongoing attachment is added but message list is empty
        assertions.verifyAttachmentListItemCount(0)
        assertions.verifyMessageListItemCount(0)
        val originalFileSize = testFile.length()
        val fileInfo = FileTransferInfo(
            testFile.absolutePath,
            "TestFile.txt",
            MimeType.TEXT_PLAIN,
            true,
            originalFileSize
        )
        fileHelper.emit(fileInfo, Resource.Loading((originalFileSize * 0.25).toLong())) // 25%

        assertions.verifyAttachmentListItemCount(1)

        val attachmentTitle = "${fileInfo.fileName} (${fileInfo.mimeType})"
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Uploading (2.00 MB / 8.00 MB) 25.00%"
        )

        fileHelper.emit(fileInfo, Resource.Loading((originalFileSize * 0.5).toLong())) // 50%
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Uploading (4.00 MB / 8.00 MB) 50.00%"
        )

        fileHelper.emit(fileInfo, Resource.Loading((originalFileSize * 0.75).toLong())) // 75%
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Uploading (6.00 MB / 8.00 MB) 75.00%"
        )

        fileHelper.emit(fileInfo, Resource.Success(originalFileSize))   // 100%
        assertions.verifyAttachmentListItem(0, attachmentTitle, "Completed")

        val messageData = MessageData(
            "user@gmail.com",
            arrayOf(
                MessageContent.TextContent(greetings),
                MessageContent.AttachmentContent(
                    fileInfo.fileName!!,
                    fileInfo.mimeType.toString()
                )
            )
        )
        responseFlow.emit(createSuccessResponse(jsonDataParser.serialize(messageData)))
        // Wait for 100ms to let the flow update the UI.
        delay(100)

        assertions.verifyMessageListItemCount(1)
        assertions.verifyMessageListItem(0, messageData)
    }

    @Test
    fun verifyMessageSendWithAttachmentError(): Unit = runBlocking {
        val greetings = "Hello world!"
        val responseFlow = MutableStateFlow<Resource<DataResponse<String>>>(Resource.Loading())
        demoNetworkEngine.on(Method.POST, "/message/add", responseFlow)

        val testFile = createMockFileSelection()
        val copiedFile = File(
            composeTestRule.activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            uniqueIdGenerator.generateId() + ".txt"
        )

        val fileHelper = FileTransferProgressCallbackHelper()
        demoNetworkEngine.onFile(copiedFile.absolutePath, fileHelper)

        val assertions = RestChatScreenAssertions(composeTestRule)
        val actions = RestChatScreenActions(composeTestRule)

        assertions.verifyScreenDisplayed()
        actions.typeMessage(greetings)

        assertions.verifyAttachmentButtonEnabled(true)

        actions.tapOnAttachmentButton()
        assertions.verifyAttachmentButtonEnabled(false)

        actions.tapOnSendButton()
        delay(600)  // Wait for 600ms for file copy operation to be completed.

        // Verify the ongoing attachment is added but message list is empty
        assertions.verifyAttachmentListItemCount(0)
        assertions.verifyMessageListItemCount(0)
        val originalFileSize = testFile.length()
        val fileInfo = FileTransferInfo(
            testFile.absolutePath,
            "TestFile.txt",
            MimeType.TEXT_PLAIN,
            true,
            originalFileSize
        )
        fileHelper.emit(fileInfo, Resource.Loading((originalFileSize * 0.25).toLong())) // 25%

        assertions.verifyAttachmentListItemCount(1)

        val attachmentTitle = "${fileInfo.fileName} (${fileInfo.mimeType})"
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Uploading (2.00 MB / 8.00 MB) 25.00%"
        )

        fileHelper.emit(fileInfo, Resource.Loading((originalFileSize * 0.5).toLong())) // 50%
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Uploading (4.00 MB / 8.00 MB) 50.00%"
        )

        fileHelper.emit(fileInfo, Resource.Loading((originalFileSize * 0.75).toLong())) // 75%
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Uploading (6.00 MB / 8.00 MB) 75.00%"
        )

        fileHelper.emit(fileInfo, Resource.Error(IOException("Network error")))
        assertions.verifyAttachmentListItem(
            0,
            attachmentTitle,
            "Failed"
        )
    }

    private fun createMockFileSelection(): File {
        val testFile = File(tempDir.newFolder("ChatScreen"), FILE_NAME)
        FileWriter(testFile).use {
            it.write(TEST_DATA.repeat(1024))
            it.flush()
        }
        intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(
                        FileProvider.getUriForFile(
                            composeTestRule.activity,
                            composeTestRule.activity.packageName + ".provider",
                            testFile
                        )
                    )
                )
            )
        return testFile
    }

    private fun createSuccessResponse(response: String): Resource<DataResponse<String>> {
        val headers = Headers()
        headers.add(HeadersConstants.CONTENT_TYPE, MimeType.JSON.toString())
        headers.add(HeadersConstants.CONTENT_LENGTH, response.length.toString())
        return Resource.Success(
            DataResponse(
                response,
                headers,
                DataSource.Network,
                HttpStatusCode.OK
            )
        )
    }
}