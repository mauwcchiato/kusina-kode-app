package com.example.kusinakode.ui.auth

import com.example.kusinakode.ui.components.readableWidth

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.KusinaToast
import com.example.kusinakode.R
import com.example.kusinakode.data.auth.GoogleSignInClient
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.ErrorRed
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.OutlineDefault
import com.example.kusinakode.ui.theme.OutlineFocused
import com.example.kusinakode.ui.branding.KodeTiles
import com.example.kusinakode.ui.branding.KusinaTiles
import com.example.kusinakode.ui.branding.WordmarkChip

private val BrandOrange = Color(0xFF8E411C)
/** "KUSINA / KODE" in the poster tile colours (green / yellow / terracotta / glass). */
@Composable
fun TileWordmark(tileSize: androidx.compose.ui.unit.Dp = 34.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KusinaTiles.forEach { WordmarkChip(it, tileSize) }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            KodeTiles.forEach { WordmarkChip(it, tileSize) }
        }
    }
}


/**
 * Brand mark for the auth screens.
 *
 * The official logo is the chef symbol with no lettering, so it sits happily
 * above [TileWordmark], which spells the name out underneath.
 */
@Composable
fun AuthLogo(size: androidx.compose.ui.unit.Dp = 96.dp) {
    Image(
        painter = painterResource(R.drawable.kk_logo),
        contentDescription = "Kusina Kode",
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/** Labeled input matching the mockup: small bold label above a rounded field. */
@Composable
fun LabeledAuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: String? = null,
    isError: Boolean = false
) {
    Column(modifier) {
        Text(
            label,
            fontFamily = BeVietnamPro,
            color = DarkBrown,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = DarkBrown, fontFamily = BeVietnamPro, fontSize = 16.sp),
            placeholder = {
                Text(
                    placeholder,
                    color = HintGray.copy(alpha = 0.7f),
                    fontFamily = BeVietnamPro
                )
            },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = HintGray,
                    modifier = Modifier
                        .padding(start = 13.dp)
                        .size(22.dp)
                )
            },
            trailingIcon = if (trailingIcon != null && onTrailingClick != null) {
                {
                    IconButton(
                        onClick = clickSfx(onTrailingClick),
                        modifier = Modifier.padding(end = 9.dp)
                    ) {
                        Icon(trailingIcon, contentDescription = "Toggle visibility", tint = HintGray)
                    }
                }
            } else null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            isError = isError,
            supportingText = if (supportingText != null) {
                { Text(supportingText, color = ErrorRed) }
            } else null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkBrown,
                unfocusedTextColor = DarkBrown,
                focusedBorderColor = OutlineFocused,
                unfocusedBorderColor = OutlineDefault,
                cursorColor = DarkBrown,
                errorBorderColor = ErrorRed,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

/** Primary full-width CTA in the burnt-orange style of the mockup. */
@Composable
fun AuthPrimaryButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = clickSfx(onClick),
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandOrange,
            contentColor = Color.White,
            disabledContainerColor = BrandOrange.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(
                text,
                fontFamily = BeVietnamPro,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * "OR CONTINUE WITH" divider + Google pill, per the mockup.
 *
 * Always rendered, configured or not. An earlier version of this hid itself
 * when no web client ID was set, which was wrong twice over: it took the
 * divider and its spacing with it, so both auth screens stopped matching the
 * mockup, and a button quietly disappearing is a worse thing to debug than a
 * button that explains itself. The server-side kk_google_configured() check
 * is what actually prevents a half-working sign-in; this only decides what
 * the tap does.
 *
 * Takes the Context to the caller's handler rather than making each screen
 * reach for LocalContext: Credential Manager needs an Activity context to
 * put its sheet on screen, and this composable already has one.
 */
@Composable
fun GoogleAuthSection(
    label: String,
    onSignIn: (Context) -> Unit,
    enabled: Boolean = true
) {
    val ctx = LocalContext.current
    val configured = GoogleSignInClient.isConfigured(ctx)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(Modifier.weight(1f), color = OutlineDefault)
            Text(
                "OR CONTINUE WITH",
                fontFamily = BeVietnamPro,
                color = HintGray,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(Modifier.weight(1f), color = OutlineDefault)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = clickSfx {
                if (configured) {
                    onSignIn(ctx)
                } else {
                    // Says which of the two setup steps is missing, rather
                    // than "coming soon" - this is a build-configuration
                    // gap, and whoever taps it is the person who can close
                    // it. See res/values/google_signin.xml.
                    KusinaToast.show(
                        ctx,
                        "Google sign-in needs a web client ID in google_signin.xml.",
                        long = true
                    )
                }
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, OutlineDefault),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(label, color = HintGray, fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

/** Bottom cross-link, e.g. "Don't have an account? Sign Up". */
@Composable
fun AuthSwitchLink(prompt: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(prompt, color = HintGray, fontFamily = BeVietnamPro, style = MaterialTheme.typography.bodyMedium)
        // Gap between the question and the action. Raise or lower this .dp.
        Spacer(Modifier.width(4.dp))
        Text(
            action,
            color = BrandOrange,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp)
        )
    }
}

/** Shared page scaffold: photo header with logo + wordmark, white sheet below. */
@Composable
fun AuthSheetScaffold(
    headerHeightFraction: Float = 0.33f,
    logoSize: Dp = 100.dp,
    tileSize: Dp = 42.dp,
    sheetGradient: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(CardSurface)) {
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(headerHeightFraction + 0.08f),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(headerHeightFraction + 0.08f)
                .background(Color.Black.copy(alpha = 0.12f))
        )
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(headerHeightFraction)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AuthLogo(size = logoSize)
                Spacer(Modifier.height(if (logoSize < 80.dp) 8.dp else 12.dp))
                TileWordmark(tileSize = tileSize)
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (sheetGradient) 36.dp else 30.dp,
                    topEnd = if (sheetGradient) 36.dp else 30.dp
                ),
                color = CardSurface,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (sheetGradient) {
                                Modifier.drawWithCache {
                                    val glow = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFF8F1EB),
                                            Color(0xFFF3E6DC)
                                        ),
                                        center = Offset(size.width * 0.18f, size.height * 0.08f),
                                        radius = size.maxDimension * 0.95f
                                    )
                                    onDrawBehind { drawRect(glow) }
                                }
                            } else Modifier
                        )
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // One cap here covers log in, sign up, the reset flow and
                    // the welcome poster. The cream sheet and its glow still
                    // fill the screen; only the form is held to a width where
                    // a text field is not a metre wide on a tablet.
                    Column(
                        Modifier.readableWidth(440.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content
                    )
                }
            }
        }
    }
}
