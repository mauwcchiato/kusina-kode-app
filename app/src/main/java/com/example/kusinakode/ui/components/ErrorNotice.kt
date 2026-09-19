package com.example.kusinakode.ui.components

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.domain.model.AppError

private val Cream = Color(0xFFFBF3E4)
private val Amber = Color(0xFFC8892C)
private val Ink = Color(0xFF3E2723)
private val Muted = Color(0xFF7A6A5B)

/**
 * Shows a failure without making it feel like a crash.
 *
 * The tone is deliberate: warm amber rather than error red, a soft card rather
 * than bare text, and a headline written for a player. The original exception
 * is still one tap away under "Details" — hiding it would just move the pain
 * to whoever has to debug this at a demo.
 */
@Composable
fun ErrorNotice(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    var showDetails by remember(error) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Cream,
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                ErrorGlyph(error.kind)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        error.headline,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        error.guidance,
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            if (onRetry != null || error.technical != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onRetry != null && error.isRetryable) {
                        FilledTonalButton(
                            onClick = onRetry,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Amber.copy(alpha = 0.16f),
                                contentColor = Color(0xFF8A5A16)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Try again", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    error.technical?.let {
                        Text(
                            if (showDetails) "Hide details" else "Details",
                            color = Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = clickSfx { showDetails = !showDetails })
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Ink.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            error.technical.orEmpty(),
                            color = Muted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

/** A soft badge rather than a warning triangle — informative, not alarming. */
@Composable
private fun ErrorGlyph(kind: AppError.Kind) {
    val icon: ImageVector = when (kind) {
        AppError.Kind.OFFLINE -> Icons.Default.WifiOff
        AppError.Kind.UNREACHABLE -> Icons.Default.CloudOff
        AppError.Kind.SERVER -> Icons.Default.RestaurantMenu
        AppError.Kind.REJECTED, AppError.Kind.UNKNOWN -> Icons.Default.ErrorOutline
    }

    // One slow breath, so the notice registers as new without demanding panic.
    val pulse by rememberInfiniteTransition(label = "notice").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "notice_pulse"
    )

    Box(
        Modifier
            .size(34.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(Amber.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
    }
}
