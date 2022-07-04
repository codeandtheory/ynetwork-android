package com.yml.chat.data

import com.accelerator.network.android.asLiveData
import com.accelerator.network.core.NetworkManager
import com.accelerator.network.core.request.BasicRequestBody
import com.accelerator.network.core.request.DataRequest
import com.accelerator.network.core.request.FormRequestBody
import com.accelerator.network.core.request.encodeUrlData
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class UserLoginData(
    val email: String,
    val password: String,
    val tokenExpiryInMs: Int? = null,
    val refreshTokenExpiryInMs: Int? = null
)

data class AuthData(val token: String, val refreshToken: String)

class AuthRepository @Inject constructor(private val networkManager: NetworkManager) {

    /**
     * Perform Login API call.
     *
     * @return [LiveData] containing the response of the API call.
     */
    fun login(email: String, password: String) = networkManager
        .submit(
            DataRequest.post(
                UrlConstants.LOGIN,
                AuthData::class,
                BasicRequestBody(UserLoginData(email, password))
            )
        )
        .asLiveData()

    /**
     * Perform Login API call with  form data encoding.
     *
     * @return [LiveData] containing the response of the API call.
     */
    fun loginWithFormEncoding(email: String, password: String) = networkManager
        .submit(
            DataRequest.post(
                UrlConstants.LOGIN,
                AuthData::class,
                FormRequestBody.Builder()
                    .addEncoded("email", encodeUrlData(email, StandardCharsets.UTF_8.toString()))
                    .add("password", password)
                    .build()
            )
        )
        .asLiveData()
}
