package com.example.kusinakode.ui.auth

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
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.ErrorRed

/** Log-in form on the white sheet, per the mockup's Frame 2. */
@Composable
fun LoginScreen(
    onLoginSuccess: (userId: Int, displayName: String, email: String) -> Unit,
    onForgotPassword: () -> Unit,
    onGoToSignUp: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    var passVisible by remember { mutableStateOf(false) }

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
                ErrorNotice(
                    error = err,
                    onRetry = { viewModel.login() },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            AuthPrimaryButton(
                text = "LOG IN",
                enabled = ui.email.isNotBlank() && ui.password.isNotBlank() && !ui.isLoading,
                isLoading = ui.isLoading,
                onClick = { viewModel.login() }
            )

            Spacer(Modifier.height(24.dp))
            GoogleAuthSection(label = "Log in with Google")
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
