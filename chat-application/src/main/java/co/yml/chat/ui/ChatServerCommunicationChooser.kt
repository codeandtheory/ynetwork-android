package co.yml.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import co.yml.chat.R

enum class CommunicationMode {
    CHAT_SOCKET_COMMUNICATION,
    CHAT_REST_FOREGROUND_COMMUNICATION,
    CHAT_REST_BACKGROUND_COMMUNICATION
}

// NOTE FOR DEVELOPERS:
// Current version of Kotlin doesn't allow annotations for parameters of Higher Order Function.
// Hence used Enum class instead of IntDef.
@Composable
private fun ChatServerCommunicationChooserUI(onClick: (mode: CommunicationMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.spacing_4)),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(id = R.string.choose_communication_mode))
        Button(
            onClick = { onClick(CommunicationMode.CHAT_SOCKET_COMMUNICATION) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.choose_socket))
        }
        Button(
            onClick = { onClick(CommunicationMode.CHAT_REST_FOREGROUND_COMMUNICATION) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.choose_rest_api_foreground))
        }
        Button(
            onClick = { onClick(CommunicationMode.CHAT_REST_BACKGROUND_COMMUNICATION) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.choose_rest_api_background))
        }
    }
}

@Composable
fun ChatServerCommunicationChooser(
    navController: NavHostController,
    token: String,
    refreshToken: String
) {
    ChatServerCommunicationChooserUI(onClick = {
        val destination = when (it) {
            CommunicationMode.CHAT_SOCKET_COMMUNICATION -> "chat/socket"
            CommunicationMode.CHAT_REST_FOREGROUND_COMMUNICATION -> "chat/rest/foreground"
            CommunicationMode.CHAT_REST_BACKGROUND_COMMUNICATION -> "chat/rest/background"
        }
        navController.navigate("$destination/$token/$refreshToken")
    })
}

@Preview
@Composable
private fun ChatServerCommunicationChooserPreview() {
    ChatServerCommunicationChooserUI(onClick = {})
}