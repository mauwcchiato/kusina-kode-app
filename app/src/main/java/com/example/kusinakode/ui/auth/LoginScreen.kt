package com.example.kusinakode.ui.auth

import com.example.kusinakode.domain.model.AppError
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.components.ErrorNotice
import com.example.kusinakode.ui.components.SecureScreen
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.ErrorRed

/** Log-in form on the white sheet, per the mockup's Frame 2. */
@Composable
fun LoginScreen(
    onLoginSuccess: (userId: Int, displayName: String, email: String) -> Unit,
    onForgotPassword: () -> Unit,
    onGoToSignUp: () -> Unit,
    /** True once when returning from a completed password reset. */
    justResetPassword: Boolean = false,
    onResetHandled: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    // Keep the password on this screen off screenshots, screen recordings and
    // the recent-apps thumbnail. Scoped to login: cleared when the screen leaves.
    SecureScreen()

    val ui by viewModel.uiState.collectAsState()
    var passVisible by remember { mutableStateOf(false) }

    // This screen stays on the back stack while the reset flow runs, so its
    // ViewModel survives and would otherwise still hold the old password and
    // the lockout card that sent the player away.
    LaunchedEffect(justResetPassword) {
        if (justResetPassword) {
            viewModel.onReturnFromPasswordReset()
            passVisible = false
            onResetHandled()
        }
    }

    ui.success?.let { session ->
        viewModel.consumeSuccess()
        onLoginSuccess(session.userId, session.displayName, session.email)
    }

    AuthSheetScaffold(sheetGradient = true) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LabeledAuthField(
                label = "Email or Username",
                value = ui.email,
                onValueChange = { viewModel.onEmailChange(it) },
                placeholder = "Email or Username",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = clickSfx(onForgotPassword), enabled = !ui.isLoading) {
                    Text(
                        "Forgot Password?",
                        color = androidx.compose.ui.graphics.Color(0xFF8E411C),
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            ui.error?.let { err ->
                val locked = err.kind == AppError.Kind.LOCKED
                ErrorNotice(
                    error = err,
                    onRetry = { viewModel.login() },
                    // A locked account cannot be typed out of, so the only
                    // button offered is the one that actually helps.
                    actionLabel = if (locked) "Reset password" else null,
                    onAction = if (locked) onForgotPassword else null,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            AuthPrimaryButton(
                text = "LOG IN",
                enabled = ui.canSubmitLogin,
                isLoading = ui.isLoading,
                onClick = { viewModel.login() }
            )

            Spacer(Modifier.height(24.dp))
            ui.googlePrompt?.let { prompt ->
                GoogleSignUpDialog(
                    email = prompt.email,
                    onConfirm = { viewModel.confirmGoogleSignUp() },
                    onDismiss = { viewModel.dismissGooglePrompt() }
                )
            }

            GoogleAuthSection(
                label = "Log in with Google",
                // Log in: ask first if there is no account yet.
                onSignIn = { viewModel.signInWithGoogle(it) },
                enabled = !ui.isLoading
            )
            Spacer(Modifier.height(18.dp))
            AuthSwitchLink(
                prompt = "Don't have an account?",
                action = "Sign up",
                onClick = onGoToSignUp
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
