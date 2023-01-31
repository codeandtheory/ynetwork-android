package co.yml.chat.ui.auth

import androidx.lifecycle.ViewModel
import co.yml.chat.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View Model for login screen.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {

    /**
     * Perform Login API call.
     */
    fun loginUser(email: String, password: String) = repository.login(email, password)

    /**
     * Perform Login API call with form encoding.
     */
    fun loginUserWithFormEncoding(email: String, password: String) = repository.loginWithFormEncoding(email, password)
}
