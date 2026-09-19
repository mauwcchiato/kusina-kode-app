package com.example.kusinakode.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.data.auth.GoogleSignInCancelled
import com.example.kusinakode.data.auth.GoogleSignInClient
import com.example.kusinakode.data.repository.AppException
import com.example.kusinakode.data.repository.RemoteAuthRepository
import com.example.kusinakode.data.repository.toAppError
import com.example.kusinakode.domain.model.AppError
import com.example.kusinakode.domain.model.GoogleOutcome
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
    val success: UserSession? = null,
    /**
     * Set when Google verified someone this app has never seen. Holds the
     * prompt the screen shows; nothing has been written server-side yet.
     */
    val googlePrompt: GooglePrompt? = null
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

/**
 * A pending "shall I create this account?" question.
 *
 * The token is kept so answering yes does not need a second trip through the
 * Google sheet. It stays in memory only, and is dropped the moment the
 * question is answered either way.
 */
data class GooglePrompt(val email: String, val name: String, val idToken: String)

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

    /**
     * Google sign-in, which is also Google sign-up - the server creates the
     * account on first use, so there is nothing for this side to decide.
     *
     * Two steps, and the split matters: Credential Manager produces a token,
     * then the server verifies it. A token in hand is not a session, and
     * this app never treats it as one.
     *
     * A dismissed sheet is not a failure. It leaves no error on screen,
     * because the player already knows what they did.
     *
     * [createDirectly] is what the two screens disagree about. From Sign Up,
     * tapping "Sign up with Google" has already said what the player wants,
     * so asking "create an account?" afterwards is asking the same question
     * twice. From Log in, the intent was to reach an account that already
     * exists - so finding none is worth stopping for, and that is the one
     * place the confirmation earns its keep.
     */
    fun signInWithGoogle(context: Context, createDirectly: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val idToken = try {
                GoogleSignInClient.getIdToken(context)
            } catch (e: GoogleSignInCancelled) {
                // Say so rather than going quiet. Treating this as a
                // no-op was wrong: when the sheet fails to open - an
                // unlisted test user, a SHA-1 mismatch - it arrives here
                // too, and the player is left tapping a button that
                // appears to do nothing at all.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = LoginUiState.validation(
                            "Google sign-in did not complete. If you did not close it yourself, " +
                                "check Logcat for KKGoogleSignIn."
                        )
                    )
                }
                return@launch
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = LoginUiState.validation(e.message ?: "Google sign-in failed")
                    )
                }
                return@launch
            }

            finishGoogle(idToken, create = createDirectly)
        }
    }

    /** The player said yes to the prompt. Same token, now with consent. */
    fun confirmGoogleSignUp() {
        val prompt = _uiState.value.googlePrompt ?: return
        _uiState.update { it.copy(isLoading = true, error = null, googlePrompt = null) }
        viewModelScope.launch { finishGoogle(prompt.idToken, create = true) }
    }

    /** The player said no. Nothing was created, so there is nothing to undo. */
    fun dismissGooglePrompt() = _uiState.update { it.copy(googlePrompt = null) }

    private suspend fun finishGoogle(idToken: String, create: Boolean) {
        authRepository.signInWithGoogle(idToken, create)
            .onSuccess { outcome ->
                when (outcome) {
                    is GoogleOutcome.SignedIn -> _uiState.update {
                        it.copy(isLoading = false, success = outcome.session)
                    }
                    is GoogleOutcome.NeedsSignUp -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            googlePrompt = GooglePrompt(outcome.email, outcome.name, idToken)
                        )
                    }
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = (e as? AppException)?.error ?: e.toAppError())
                }
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
