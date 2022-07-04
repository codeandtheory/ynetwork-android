package com.yml.chat.actions.ui.rest

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.yml.chat.TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT
import com.yml.chat.TAG_REST_CHAT_SCREEN_MESSAGE_INPUT
import com.yml.chat.TAG_REST_CHAT_SCREEN_SEND_MESSAGE
import org.junit.rules.TestRule

class RestChatScreenActions<RULE : TestRule, ACTIVITY : ComponentActivity>(private val composeTestRule: AndroidComposeTestRule<RULE, ACTIVITY>) {

    /**
     * Perform type operation on the message input with [message] text.
     *
     * @param message the text that needs to be typed.
     */
    fun typeMessage(message: String) {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_MESSAGE_INPUT)
            .assertIsDisplayed()
            .performTextInput(message)
        Espresso.closeSoftKeyboard()    // Explicitly close the keyboard after typing the test.
    }

    /**
     * Perform a tap operation on Send message button.
     */
    fun tapOnSendButton() {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_SEND_MESSAGE)
            .assertIsDisplayed()
            .performClick()
    }

    /**
     * Perform a tap operation on attachment button.
     */
    fun tapOnAttachmentButton() {
        composeTestRule.onNodeWithTag(TAG_REST_CHAT_SCREEN_ADD_ATTACHMENT)
            .assertIsDisplayed()
            .performClick()
    }
}