package com.yml.chat.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.NavController
import com.yml.network.core.Resource
import com.yml.chat.R

@Composable
private fun LoginScreenUI(
    onLoginClick: (email: String, password: String, shouldUseFormEncoding: Boolean) -> Unit,
    loginState: State<String?>
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var emailValue by rememberSaveable { mutableStateOf("john@gmail.com") }
        var passwordValue by rememberSaveable { mutableStateOf("John Doe's password") }
        var passwordVisibility by remember { mutableStateOf(false) }

        val maxWidthWithPadding = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.spacing_4),
                vertical = dimensionResource(id = R.dimen.spacing_2)
            )

        Text(
            text = loginState.value ?: "",
            modifier = maxWidthWithPadding,
            textAlign = TextAlign.Center
        )
        TextField(
            value = emailValue,
            onValueChange = { emailValue = it },
            label = { Text(text = stringResource(id = R.string.email_field_label)) },
            modifier = maxWidthWithPadding
        )
        TextField(
            value = passwordValue,
            onValueChange = { passwordValue = it },
            label = { Text(text = stringResource(id = R.string.password_field_label)) },
            modifier = maxWidthWithPadding,
            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = {
                    passwordVisibility = !passwordVisibility
                }) {
                    Icon(
                        imageVector = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        ""
                    )
                }
            }
        )
        Button(onClick = { onLoginClick(emailValue, passwordValue, false) }) {
            Text(text = stringResource(id = R.string.login_button_label))
        }
        Button(onClick = { onLoginClick(emailValue, passwordValue, true) }) {
            Text(text = stringResource(id = R.string.login_with_form_encoding_button_label))
        }
    }
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel, navController: NavController) {
    val loginStateLiveData = MediatorLiveData<String>()

    LoginScreenUI(
        onLoginClick = { email, password, shouldUseFormEncoding ->
            val loginLiveData =
                if (shouldUseFormEncoding) authViewModel.loginUserWithFormEncoding(email, password)
                else authViewModel.loginUser(email, password)
            loginStateLiveData.addSource(loginLiveData) {
                when (it) {
                    is Resource.Loading -> loginStateLiveData.value = "loading"
                    is Resource.Error -> {
                        loginStateLiveData.value = "Error ${it.error}"
                        loginStateLiveData.removeSource(loginLiveData)
                    }
                    is Resource.Success -> {
                        it.data.body?.let { body ->
                            navController.navigate("chat/choose/${body.token}/${body.refreshToken}")
                            loginStateLiveData.value = "success"
                        } ?: run {
                            loginStateLiveData.value = "Error: Auth token missing"
                        }
                        loginStateLiveData.removeSource(loginLiveData)
                    }
                    null -> loginStateLiveData.value = ""
                }
            }
        },
        loginState = loginStateLiveData.observeAsState()
    )
}

@Preview
@Composable
private fun LoginScreenPreview() {
    LoginScreenUI({ _, _, _ -> }, remember { mutableStateOf("Loading") })
}
