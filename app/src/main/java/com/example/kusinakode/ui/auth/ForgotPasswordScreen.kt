package com.example.kusinakode.ui.auth

import com.example.kusinakode.ui.components.clickSfx
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.ErrorRed
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.OutlineDefault
import com.example.kusinakode.ui.theme.OutlineFocused
import com.example.kusinakode.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

private val BrandOrange = Color(0xFF8E411C)
private val SageBlob = Color(0xFFEDE3D4)
private val SuccessBlob = Color(0xFFCDE8C8)
private val LockBlob = Color(0xFFE6D5BE)

/**
 * Forgot-password kitchen: email → mail sent → 6-digit code → new password → done.
 * Same wood header and terracotta CTAs as login / signup.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    /**
     * Leaving after the password actually changed, as opposed to backing
     * out part-way. Login needs to know the difference so it can drop the
     * password the player just replaced.
     */
    onDone: () -> Unit = onBack,
    viewModel: PasswordResetViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()

    // goBack() only leaves the flow from the first and last steps; from the
    // last one the reset has succeeded, so that exit is onDone.
    val leaveFlow = { if (ui.step == ResetStep.Done) onDone() else onBack() }

    BackHandler { viewModel.goBack(leaveFlow) }

    Box(Modifier.fillMaxSize()) {
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
                ResetStepDots(ui.step)
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = ui.step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "reset_step"
                ) { step ->
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (step) {
                            ResetStep.Email -> EmailStep(ui, viewModel, onBack)
                            ResetStep.Sent -> SentStep(ui, viewModel)
                            ResetStep.Verify -> VerifyStep(ui, viewModel)
                            ResetStep.NewPassword -> NewPasswordStep(ui, viewModel)
                            ResetStep.Done -> DoneStep(onDone)
                        }
                    }
                }
            }
        }
        IconButton(
            onClick = clickSfx { viewModel.goBack(leaveFlow) },
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun EmailStep(
    ui: PasswordResetUiState,
    viewModel: PasswordResetViewModel,
    onSignIn: () -> Unit
) {
    EmailEnvelopeHero()
    Spacer(Modifier.height(18.dp))
    ResetTitle("Forgot Password")
    ResetSubtitle("Enter the email on your Kusina Kode account and we'll send a code.")
    Spacer(Modifier.height(22.dp))
    LabeledAuthField(
        label = "Email address",
        value = ui.email,
        onValueChange = viewModel::onEmailChange,
        placeholder = "you@email.com",
        leadingIcon = Icons.Outlined.Email,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    ResetFeedback(ui)
    Spacer(Modifier.height(22.dp))
    AuthPrimaryButton(
        text = "SUBMIT",
        enabled = ui.email.isNotBlank() && !ui.isLoading,
        isLoading = ui.isLoading,
        onClick = viewModel::requestCode
    )
    Spacer(Modifier.height(12.dp))
    AuthSwitchLink(
        prompt = "Remember the password?",
        action = "Log in",
        onClick = onSignIn
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SentStep(
    ui: PasswordResetUiState,
    viewModel: PasswordResetViewModel
) {
    ResetDonutHero(ringColor = SuccessBlob) { SuccessMailGlyph() }
    Spacer(Modifier.height(18.dp))
    ResetTitle("Success")
    ResetSubtitle(
        prefix = "Please check ",
        highlight = ui.email,
        suffix = " for a 6-digit code."
    )
    ResetDevCode(ui.devCode)
    ResetFeedback(ui)
    Spacer(Modifier.height(22.dp))
    AuthPrimaryButton(
        text = "ENTER CODE",
        enabled = !ui.isLoading,
        isLoading = false,
        onClick = viewModel::goToVerify
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun VerifyStep(
    ui: PasswordResetUiState,
    viewModel: PasswordResetViewModel
) {
    ResetDonutHero { VerifyLockGlyph() }
    Spacer(Modifier.height(18.dp))
    ResetTitle("Verification")
    Spacer(Modifier.height(8.dp))
    ResetSubtitle(
        prefix = "We've sent a verification code to ",
        highlight = ui.email,
        suffix = ". Enter the 6-digit code to continue."
    )
    ResetDevCode(ui.devCode)
    Spacer(Modifier.height(22.dp))
    Text(
        "Enter 6-digit code",
        fontFamily = BeVietnamPro,
        color = DarkBrown,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    ResetCodeRow(code = ui.code, onCodeChange = viewModel::onCodeChange)
    ResetFeedback(ui)
    Spacer(Modifier.height(20.dp))
    AuthPrimaryButton(
        text = "VERIFY",
        enabled = ui.codeComplete && !ui.isLoading,
        isLoading = ui.isLoading,
        onClick = viewModel::confirmCode
    )
    Spacer(Modifier.height(8.dp))
    ResendLink(
        canResend = ui.canResend,
        seconds = ui.resendSeconds,
        loading = ui.isLoading,
        prompt = "Didn't receive a code?",
        action = "Resend code!",
        onResend = viewModel::resendCode
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NewPasswordStep(
    ui: PasswordResetUiState,
    viewModel: PasswordResetViewModel
) {
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    ResetDonutHero {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = BrandOrange,
            modifier = Modifier.size(62.dp)
        )
    }
    Spacer(Modifier.height(18.dp))
    ResetTitle("Change New Password")
    ResetSubtitle("Pick a different password from the last one. Same strength rules as sign up.")
    Spacer(Modifier.height(18.dp))
    LabeledAuthField(
        label = "New Password",
        value = ui.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        placeholder = "Enter a new password",
        leadingIcon = Icons.Outlined.Lock,
        trailingIcon = if (showNew) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
        onTrailingClick = { showNew = !showNew },
        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
    if (ui.newPassword.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        PasswordRequirementsList(getPasswordRequirements(ui.newPassword))
    }
    Spacer(Modifier.height(14.dp))
    LabeledAuthField(
        label = "Confirm Password",
        value = ui.confirmPassword,
        onValueChange = viewModel::onConfirmPasswordChange,
        placeholder = "Re-enter your password",
        leadingIcon = Icons.Outlined.Lock,
        trailingIcon = if (showConfirm) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
        onTrailingClick = { showConfirm = !showConfirm },
        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
    if (ui.confirmPassword.isNotBlank() && ui.newPassword != ui.confirmPassword) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Passwords do not match",
            color = ErrorRed,
            fontFamily = BeVietnamPro,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
    ResetFeedback(ui)
    Spacer(Modifier.height(22.dp))
    AuthPrimaryButton(
        text = "RESET PASSWORD",
        enabled = ui.newPassword.isNotBlank() &&
            ui.confirmPassword.isNotBlank() &&
            !ui.isLoading,
        isLoading = ui.isLoading,
        onClick = viewModel::submitNewPassword
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun DoneStep(onBack: () -> Unit) {
    ResetHero(happy = true)
    Spacer(Modifier.height(18.dp))
    ResetTitle("You're back in")
    ResetSubtitle("Your password was updated. Log in with the new one and get cooking.")
    Spacer(Modifier.height(22.dp))
    AuthPrimaryButton(
        text = "BACK TO LOG IN",
        enabled = true,
        isLoading = false,
        onClick = onBack
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun EmailEnvelopeHero() {
    ResetDonutHero {
        Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = null,
            tint = BrandOrange,
            modifier = Modifier.size(62.dp)
        )
    }
}

@Composable
private fun ResetDonutHero(
    ringColor: Color = Color(0xFFD2B48C),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(148.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(125.dp)
                .clip(CircleShape)
                .background(ringColor)
        )
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(CardSurface)
        )
        content()
    }
}

@Composable
private fun SuccessMailGlyph() {
    Box(Modifier.size(62.dp)) {
        Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier
                .size(62.dp)
                .align(Alignment.Center)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun ResetHero(
    happy: Boolean = false,
    lock: Boolean = false,
    verify: Boolean = false
) {
    val blob = when {
        lock -> LockBlob
        happy -> SuccessBlob
        else -> SageBlob
    }
    Box(
        modifier = Modifier.size(148.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(blob)
        )
        when {
            lock -> {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(64.dp)
                )
            }
            verify -> VerifyLockGlyph()
            happy -> {
                Box(Modifier.size(64.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

/** Shield with a circle head + rounded torso flush to the tip, like the reference. */
@Composable
private fun VerifyLockGlyph() {
    val person = Color(0xFFE8D4C0)
    Canvas(Modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * 12f / 24f, h * 1f / 24f)
            lineTo(w * 3f / 24f, h * 5f / 24f)
            lineTo(w * 3f / 24f, h * 11f / 24f)
            cubicTo(
                w * 3f / 24f, h * 16.55f / 24f,
                w * 6.84f / 24f, h * 21.74f / 24f,
                w * 12f / 24f, h * 23f / 24f
            )
            cubicTo(
                w * 17.16f / 24f, h * 21.74f / 24f,
                w * 21f / 24f, h * 16.55f / 24f,
                w * 21f / 24f, h * 11f / 24f
            )
            lineTo(w * 21f / 24f, h * 5f / 24f)
            close()
        }
        drawPath(shield, BrandOrange)

        val torso = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = w * 0.22f,
                        top = h * 0.49f,
                        right = w * 0.78f,
                        bottom = h * 1.06f
                    ),
                    radiusX = w * 0.30f,
                    radiusY = w * 0.30f
                )
            )
        }
        val clippedTorso = Path().apply {
            op(torso, shield, PathOperation.Intersect)
        }
        drawPath(clippedTorso, person)

        drawCircle(
            color = person,
            radius = w * 0.135f,
            center = Offset(w * 0.50f, h * 0.32f)
        )
    }
}

@Composable
private fun ResetTitle(text: String) {
    Text(
        text,
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        color = DarkBrown,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ResetSubtitle(text: String) {
    Text(
        text,
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = HintGray,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
    )
}

@Composable
private fun ResetSubtitle(prefix: String, highlight: String, suffix: String) {
    Text(
        buildAnnotatedString {
            append(prefix)
            withStyle(
                SpanStyle(
                    color = BrandOrange,
                    fontWeight = FontWeight.Bold,
                    fontFamily = BeVietnamPro
                )
            ) {
                append(highlight)
            }
            append(suffix)
        },
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = HintGray,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
    )
}

@Composable
private fun ResetStepDots(step: ResetStep) {
    val index = when (step) {
        ResetStep.Email -> 0
        ResetStep.Sent -> 1
        ResetStep.Verify -> 2
        ResetStep.NewPassword -> 3
        ResetStep.Done -> 4
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { i ->
            val filled = index > i || (step == ResetStep.Done)
            Box(
                Modifier
                    .size(if (index == i) 8.dp else 7.dp)
                    .clip(CircleShape)
                    .background(if (filled || index == i) BrandOrange else OutlineDefault)
            )
        }
    }
}

@Composable
private fun ResetCodeRow(
    code: String,
    onCodeChange: (String) -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Focus and the keyboard are two separate things, which is the whole bug.
    //
    // The six boxes are decoration; the real field is transparent and 1sp
    // tall. Tapping a box called requestFocus() alone — and once the field
    // already has focus that is a no-op, so after dismissing the keyboard
    // once there was no way to bring it back and the boxes looked dead.
    // Asking the IME to show, every tap, is what actually reopens it.
    LaunchedEffect(Unit) {
        focus.requestFocus()
        // The step arrives through an AnimatedContent crossfade; asking
        // before that settles is ignored, so this waits a frame or two.
        delay(150)
        keyboard?.show()
    }

    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        decorationBox = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(PasswordResetUiState.CODE_LENGTH) { i ->
                    val digit = code.getOrNull(i)?.toString().orEmpty()
                    val active = code.length == i ||
                        (code.length == PasswordResetUiState.CODE_LENGTH && i == PasswordResetUiState.CODE_LENGTH - 1)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(
                                1.5.dp,
                                if (active) OutlineFocused else OutlineDefault,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                focus.requestFocus()
                                keyboard?.show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            digit,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = DarkBrown
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ResendLink(
    canResend: Boolean,
    seconds: Int,
    loading: Boolean,
    prompt: String,
    action: String,
    onResend: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            prompt,
            color = HintGray,
            fontFamily = BeVietnamPro,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (!canResend && seconds > 0) "Wait ${seconds}s" else action,
            color = if (canResend && !loading) BrandOrange else HintGray,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = canResend && !loading, onClick = onResend)
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun ResetDevCode(devCode: String?) {
    if (devCode.isNullOrBlank()) return
    Text(
        "Dev code: $devCode",
        fontFamily = BeVietnamPro,
        fontSize = 12.sp,
        color = BrandOrange,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp)
    )
}

@Composable
private fun ResetFeedback(ui: PasswordResetUiState) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ui.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                it,
                color = ErrorRed,
                fontFamily = BeVietnamPro,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (ui.error == null && ui.step == ResetStep.Sent) {
            ui.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    msg,
                    color = HintGray,
                    fontFamily = BeVietnamPro,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
