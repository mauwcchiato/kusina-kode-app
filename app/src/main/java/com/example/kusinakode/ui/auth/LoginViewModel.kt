package com.example.kusinakode.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.data.repository.AppException
import com.example.kusinakode.data.repository.RemoteAuthRepository
import com.example.kusinakode.data.repository.toAppError
import com.example.kusinakode.domain.model.AppError
import com.example.kusinakode.domain.model.UserSession
import com.example.kusinakode.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    // form fields (name = username)
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** Classified failure — headline, guidance, and the raw text for Details. */
    val error: AppError? = null,
    /** Set once on successful login/sign-up; consume with [LoginViewModel.consumeSuccess]. */
    val success: UserSession? = null
) {
    /** Local validation messages don't come from the network layer. */
    companion object {
        fun validation(message: String) = AppError(
            kind = AppError.Kind.REJECTED,
            headline = message,
            guidance = "Fill that in and try again."
        )
    }
}

class LoginViewModel(
    private val authRepository: AuthRepository = RemoteAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onNicknameChange(v: String) = _uiState.update { it.copy(nickname = v) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun consumeSuccess() = _uiState.update { it.copy(success = null) }

    fun login() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = LoginUiState.validation("Email and password cannot be empty")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(s.email, s.password)
                .onSuccess { session -> _uiState.update { it.copy(isLoading = false, success = session) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = (e as? AppException)?.error ?: e.toAppError()) } }
        }
    }

    fun signUp() {
        val s = _uiState.value
        if (s.name.isBlank() || s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = LoginUiState.validation("Username, email, and password are required")) }
            return
        }
        val usernameErr = validateUsername(s.name.trim())
        if (usernameErr != null) {
            _uiState.update { it.copy(error = LoginUiState.validation(usernameErr)) }
            return
        }
        if (!isStrongPassword(s.password)) {
            _uiState.update {
                it.copy(error = LoginUiState.validation("Strong password required: 8+ characters with uppercase, lowercase, number, and special character."))
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.register(
                name = s.name.trim(),
                nickname = s.nickname.ifBlank { null },
                email = s.email.trim(),
                password = s.password
            )
                .onSuccess { session -> _uiState.update { it.copy(isLoading = false, success = session) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = (e as? AppException)?.error ?: e.toAppError()) } }
        }
    }

    // USERNAME RULES: required, 3+ chars, letters/numbers/underscore only.
    private fun validateUsername(username: String): String? {
        if (username.isEmpty()) return "Username is required"
        if (username.length < 3) return "Use at least 3 characters for username"
        if (!username.all { it.isLetterOrDigit() || it == '_' }) {
            return "Username may only contain letters, numbers, and _"
        }
        return null
    }

    // PASSWORD RULES (mirror PHP register.php).
    private fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }
}
