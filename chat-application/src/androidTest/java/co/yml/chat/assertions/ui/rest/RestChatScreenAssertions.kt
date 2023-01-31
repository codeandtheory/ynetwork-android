package co.yml.chat.assertions.ui.rest

import androidx.activity.ComponentActivity
import androidx.annotation.IntRange
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import co.yml.chat.R
import co.yml.chat.TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_PROGRESS
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_TITLE
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_LIST
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_INPUT
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_CONTENT
import co.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_FROM
import co.yml.chat.TAG_REST_CHAT_SCREEN_SEND_MESSAGE
import co.yml.chat.data.MessageContent
import co.yml.chat.data.MessageData
import org.junit.rules.TestRule

class RestChatScreenAssertions<RULE : TestRule, ACTIVITY : ComponentActivity>(private val composeTestRule: AndroidComposeTestRule<RULE, ACTIVITY>) {

    /**
     * Verify the chat screen UI components are displayed on the screen.
     */
    fun verifyScreenDisplayed() {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_MESSAGE_INPUT)
            .assertExists("Input message is not present")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT)
            .assertExists("Add attachment button is no present")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_SEND_MESSAGE)
            .assertExists("Send button is not present")
            .assertTextEquals(composeTestRule.activity.getString(R.string.send))
            .assertIsDisplayed()
    }

    /**
     * Verify whether the attachment button is enabled or not based on [isEnabled].
     *
     * @param [isEnabled] specifies, whether the attachment button should be enabled or not.
     */
    fun verifyAttachmentButtonEnabled(isEnabled: Boolean) {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT)
            .assertExists("Add attachment button is no present")
            .assertIsDisplayed()
            .let {
                if (isEnabled) it.assertIsEnabled() else it.assertIsNotEnabled()
            }
    }

    /**
     * Verify the attachment list's item count.
     *
     * @param itemCount expected item count.
     */
    fun verifyAttachmentListItemCount(@IntRange(from = 0) itemCount: Int) {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_LIST)
            .assertExists("Attachment list is not present")
            .onChildren()
            .assertCountEquals(itemCount)
    }

    /**
     * Verify the attachment list's item data.
     *
     * @param itemIndex index of the item in the list.
     * @param attachmentTitle title that needs to be asserted.
     * @param attachmentProgress progress string that needs to be asserted.
     */
    fun verifyAttachmentListItem(
        @IntRange(from = 0) itemIndex: Int,
        attachmentTitle: String,
        attachmentProgress: String
    ) {
        val child = composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_LIST)
            .onChildAt(itemIndex).onChildren()
        child.filterToOne(hasTestTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_TITLE))
            .assertExists("Attachment title for $itemIndex does not exists")
            .assertTextEquals(attachmentTitle)
        child.filterToOne(hasTestTag(TAG_REST_CHAT_SCREEN_MESSAGE_ATTACHMENT_ITEM_PROGRESS))
            .assertExists("Attachment progress for $itemIndex does not exists")
            .assertTextEquals(attachmentProgress)
    }

    /**
     * Verify the message list's item count.
     *
     * @param itemCount expected item count.
     */
    fun verifyMessageListItemCount(@IntRange(from = 0) itemCount: Int) {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST)
            .assertExists("Message List is not present")
            .onChildren()
            .assertCountEquals(itemCount)
    }

    /**
     * Verify the attachment list's item data.
     *
     * @param itemIndex index of the item in the list.
     * @param messageData data that needs to be asserted.
     */
    fun verifyMessageListItem(
        @IntRange(from = 0) itemIndex: Int,
        messageData: MessageData
    ) {
        val child = composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST)
            .onChildAt(itemIndex).onChildren()
        child.filterToOne(hasTestTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_FROM))
            .assertExists("Message from for $itemIndex does not exists")
            .assertTextEquals(messageData.from)

        val contentUI =
            child.filterToOne(hasTestTag(TAG_REST_CHAT_SCREEN_MESSAGE_LIST_CHAT_ITEM_CONTENT))
                .assertExists("Message content for $itemIndex does not exists")
                .onChildren()

        contentUI.assertCountEquals(messageData.content.size)

        messageData.content.forEachIndexed { index, messageContent ->
            when (messageContent) {
                is MessageContent.TextContent -> contentUI[index]
                    .assertTextEquals(messageContent.message)
                is MessageContent.AttachmentContent -> contentUI[index]
                    .assertTextEquals(messageContent.toString())
            }
        }
    }
}