package com.example.kusinakode.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.data.repository.RemoteAuthRepository
import com.example.kusinakode.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ResetStep {
    Email,
    Sent,
    Verify,
    NewPassword,
    Done
}

data class PasswordResetUiState(
    val step: ResetStep = ResetStep.Email,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** Present only when the debug backend echoes the OTP. */
    val devCode: String? = null,
    val resendSeconds: Int = 0
) {
    val canResend: Boolean get() = resendSeconds <= 0 && !isLoading
    val codeComplete: Boolean get() = code.length == CODE_LENGTH
    val passwordsMatch: Boolean
        get() = newPassword.isNotEmpty() && newPassword == confirmPassword

    companion object {
        const val CODE_LENGTH = 6
        const val RESEND_SECONDS = 30
    }
}

class PasswordResetViewModel(
    private val authRepository: AuthRepository = RemoteAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordResetUiState())
    val uiState: StateFlow<PasswordResetUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onEmailChange(v: String) =
        _uiState.update { it.copy(email = v, error = null, message = null) }

    fun onCodeChange(v: String) =
        _uiState.update {
            it.copy(
                code = v.filter { ch -> ch.isDigit() }.take(PasswordResetUiState.CODE_LENGTH),
                error = null,
                message = null
            )
        }

    fun onNewPasswordChange(v: String) =
        _uiState.update { it.copy(newPassword = v, error = null, message = null) }

    fun onConfirmPasswordChange(v: String) =
        _uiState.update { it.copy(confirmPassword = v, error = null, message = null) }

    fun requestCode() = sendCode(advanceToSent = true)

    fun resendCode() {
        if (!_uiState.value.canResend) return
        sendCode(advanceToSent = false)
    }

    fun goToVerify() = _uiState.update {
        it.copy(step = ResetStep.Verify, error = null, message = null)
    }

    fun confirmCode() {
        val s = _uiState.value
        if (!s.codeComplete) {
            _uiState.update { it.copy(error = "Enter the 6-digit code from your email") }
            return
        }
        _uiState.update {
            it.copy(step = ResetStep.NewPassword, error = null, message = null)
        }
    }

    fun submitNewPassword() {
        val s = _uiState.value
        if (!isStrongPassword(s.newPassword)) {
            _uiState.update {
                it.copy(error = "Use 8+ characters with uppercase, lowercase, a number, and a special character.")
            }
            return
        }
        if (!s.passwordsMatch) {
            _uiState.update { it.copy(error = "Those passwords don't match") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, message = null) }
        viewModelScope.launch {
            authRepository.resetPassword(s.email, s.code, s.newPassword)
                .onSuccess { msg ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = ResetStep.Done,
                            message = msg,
                            newPassword = "",
                            confirmPassword = "",
                            code = ""
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = ResetStep.Verify,
                            error = e.message ?: "That code didn't work. Try again or resend."
                        )
                    }
                }
        }
    }

    fun goBack(leaveFlow: () -> Unit) {
        when (_uiState.value.step) {
            ResetStep.Email, ResetStep.Done -> leaveFlow()
            ResetStep.Sent -> _uiState.update {
                it.copy(step = ResetStep.Email, error = null, message = null)
            }
            ResetStep.Verify -> _uiState.update {
                it.copy(step = ResetStep.Sent, error = null, message = null)
            }
            ResetStep.NewPassword -> _uiState.update {
                it.copy(step = ResetStep.Verify, error = null, message = null)
            }
        }
    }

    private fun sendCode(advanceToSent: Boolean) {
        val email = _uiState.value.email.trim()
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(error = "Enter the email on your account") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, message = null, email = email) }
        viewModelScope.launch {
            authRepository.requestPasswordReset(email)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = if (advanceToSent || it.step == ResetStep.Email) {
                                ResetStep.Sent
                            } else {
                                it.step
                            },
                            message = result.message,
                            devCode = result.devCode,
                            code = if (advanceToSent) "" else it.code
                        )
                    }
                    startCooldown()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (seconds in PasswordResetUiState.RESEND_SECONDS downTo 0) {
                _uiState.update { it.copy(resendSeconds = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    private fun isValidEmail(v: String): Boolean =
        v.contains("@") && v.substringAfter("@").contains(".")
}
