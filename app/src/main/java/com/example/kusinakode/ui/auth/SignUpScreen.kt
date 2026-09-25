package com.example.kusinakode.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.components.ErrorNotice
import com.example.kusinakode.ui.components.SecureScreen
import com.example.kusinakode.ui.theme.ErrorRed
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.SuccessGreen

/** Create-account form on the white sheet, per the mockup's Frame 3. */
@Composable
fun SignUpScreen(
    onSignUpSuccess: (userId: Int, displayName: String, email: String) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    // Password fields on this screen stay off screenshots / recordings.
    SecureScreen()

    val ui by viewModel.uiState.collectAsState()
    var passVisible by remember { mutableStateOf(false) }

    ui.success?.let { session ->
        viewModel.consumeSuccess()
        onSignUpSuccess(session.userId, session.displayName, session.email)
    }

    val usernameError = remember(ui.name) {
        if (ui.name.isEmpty()) null else validateUsernameForUi(ui.name)
    }
    val passwordStrong = remember(ui.password) { isStrongPassword(ui.password) }
    val canSubmit = ui.name.isNotBlank() &&
            usernameError == null &&
            ui.email.isNotBlank() &&
            passwordStrong &&
            !ui.isLoading

    AuthSheetScaffold(headerHeightFraction = 0.33f, sheetGradient = true) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LabeledAuthField(
                label = "Username",
                value = ui.name,
                onValueChange = { v -> viewModel.onNameChange(v.filter { it.isLetterOrDigit() || it == '_' }) },
                placeholder = "Enter your username",
                leadingIcon = Icons.Outlined.Person,
                supportingText = usernameError,
                isError = usernameError != null
            )
            Spacer(Modifier.height(14.dp))
            LabeledAuthField(
                label = "Nickname (Optional)",
                value = ui.nickname,
                onValueChange = { viewModel.onNicknameChange(it) },
                placeholder = "Enter your nickname",
                leadingIcon = Icons.Outlined.Person
            )
            Spacer(Modifier.height(14.dp))
            LabeledAuthField(
                label = "Email Address",
                value = ui.email,
                onValueChange = { viewModel.onEmailChange(it) },
                placeholder = "Enter your email",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(14.dp))
            LabeledAuthField(
                label = "Password",
                value = ui.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                placeholder = "Enter your password",
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = if (passVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                onTrailingClick = { passVisible = !passVisible },
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (ui.password.isNotBlank() && !passwordStrong) {
                Spacer(Modifier.height(10.dp))
                PasswordRequirementsList(getPasswordRequirements(ui.password))
            }

            ui.error?.let { err ->
                Spacer(Modifier.height(10.dp))
                ErrorNotice(error = err, onRetry = { viewModel.signUp() })
            }

            Spacer(Modifier.height(20.dp))
            AuthPrimaryButton(
                text = "SIGN UP",
                enabled = canSubmit,
                isLoading = ui.isLoading,
                onClick = { viewModel.signUp() }
            )

            Spacer(Modifier.height(20.dp))
            ui.googlePrompt?.let { prompt ->
                GoogleSignUpDialog(
                    email = prompt.email,
                    onConfirm = { viewModel.confirmGoogleSignUp() },
                    onDismiss = { viewModel.dismissGooglePrompt() }
                )
            }

            GoogleAuthSection(
                label = "Sign up with Google",
                // Sign up: the tap was the answer. Create it.
                onSignIn = { viewModel.signInWithGoogle(it, createDirectly = true) },
                enabled = !ui.isLoading
            )
            Spacer(Modifier.height(14.dp))
            AuthSwitchLink(
                prompt = "Have an account?",
                action = "Log in",
                onClick = onGoToLogin
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
internal fun PasswordRequirementsList(requirements: List<PasswordRequirement>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        requirements.forEach { req ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (req.met) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (req.met) SuccessGreen else HintGray
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    req.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (req.met) SuccessGreen else HintGray
                    )
                )
            }
        }
    }
}

internal data class PasswordRequirement(val label: String, val met: Boolean)

/** Same rules as the backend (register.php). */
internal fun isStrongPassword(password: String): Boolean {
    if (password.length < 8) return false
    if (!password.any { it.isUpperCase() }) return false
    if (!password.any { it.isLowerCase() }) return false
    if (!password.any { it.isDigit() }) return false
    if (!password.any { !it.isLetterOrDigit() }) return false
    return true
}

internal fun getPasswordRequirements(password: String): List<PasswordRequirement> = listOf(
    PasswordRequirement("At least 8 characters", password.length >= 8),
    PasswordRequirement(
        "Uppercase and lowercase letters",
        password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
    ),
    PasswordRequirement("At least one number", password.any { it.isDigit() }),
    PasswordRequirement(
        "At least one special character (! @ # \$ % …)",
        password.any { !it.isLetterOrDigit() }
    )
)

/** UI-only validation; the ViewModel and backend re-validate on submit. */
internal fun validateUsernameForUi(username: String): String? {
    if (username.isEmpty()) return "Username is required"
    if (username.length < 3) return "Use at least 3 characters"
    if (!username.all { it.isLetterOrDigit() || it == '_' }) {
        return "Only letters, numbers, and _ allowed"
    }
    return null
}
