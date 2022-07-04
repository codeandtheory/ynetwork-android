package com.yml.chat.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yml.chat.ui.auth.LoginScreen
import com.yml.chat.ui.rest.RestChatScreen
import com.yml.chat.ui.socket.SocketChatScreen

const val AUTH_ARGUMENT_TOKEN = "token"
const val AUTH_ARGUMENT_REFRESH_TOKEN = "refreshToken"

private val authArguments = listOf(
    navArgument(AUTH_ARGUMENT_TOKEN) { type = NavType.StringType },
    navArgument(AUTH_ARGUMENT_REFRESH_TOKEN) { type = NavType.StringType }
)

private fun getArgumentOrThrow(navBackStackEntry: NavBackStackEntry, argumentName: String) =
    navBackStackEntry.arguments?.getString(argumentName)
        ?: throw IllegalArgumentException("$argumentName is not provided in the nav arguments.")

@Composable
fun ChatNavigation() {
    val navController = rememberNavController()
    navController.enableOnBackPressed(true)
    NavHost(navController, startDestination = "loading") {
        composable(route = "loading") {
            LoginScreen(authViewModel = hiltViewModel(), navController = navController)
        }
        // Ref: https://developer.android.com/jetpack/compose/navigation#nav-with-args
        composable(
            route = "chat/choose/{$AUTH_ARGUMENT_TOKEN}/{$AUTH_ARGUMENT_REFRESH_TOKEN}",
            arguments = authArguments
        ) {
            val token = getArgumentOrThrow(it, AUTH_ARGUMENT_TOKEN)
            val refreshToken = getArgumentOrThrow(it, AUTH_ARGUMENT_REFRESH_TOKEN)
            ChatServerCommunicationChooser(navController, token, refreshToken)
        }
        composable(
            route = "chat/rest/foreground/{$AUTH_ARGUMENT_TOKEN}/{$AUTH_ARGUMENT_REFRESH_TOKEN}",
            arguments = authArguments
        ) {
            val token = getArgumentOrThrow(it, AUTH_ARGUMENT_TOKEN)
            val refreshToken = getArgumentOrThrow(it, AUTH_ARGUMENT_REFRESH_TOKEN)
            RestChatScreen(hiltViewModel(), token, refreshToken, postInBackground = false)
        }
        composable(
            route = "chat/rest/background/{$AUTH_ARGUMENT_TOKEN}/{$AUTH_ARGUMENT_REFRESH_TOKEN}",
            arguments = authArguments
        ) {
            val token = getArgumentOrThrow(it, AUTH_ARGUMENT_TOKEN)
            val refreshToken = getArgumentOrThrow(it, AUTH_ARGUMENT_REFRESH_TOKEN)
            RestChatScreen(hiltViewModel(), token, refreshToken, postInBackground = true)
        }
        composable(
            route = "chat/socket/{$AUTH_ARGUMENT_TOKEN}/{$AUTH_ARGUMENT_REFRESH_TOKEN}",
            arguments = authArguments
        ) {
            val token = getArgumentOrThrow(it, AUTH_ARGUMENT_TOKEN)
            val refreshToken = getArgumentOrThrow(it, AUTH_ARGUMENT_REFRESH_TOKEN)
            SocketChatScreen(hiltViewModel(), token, refreshToken)
        }
    }
}