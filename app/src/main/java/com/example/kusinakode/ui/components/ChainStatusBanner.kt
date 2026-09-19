package com.example.kusinakode.ui.components

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.domain.ChainActivity
import com.example.kusinakode.domain.ChainEvents
import com.example.kusinakode.ui.theme.LightOrange
import kotlinx.coroutines.delay

/**
 * Narrates the reward pipeline wherever the player happens to be.
 *
 * Panel revision item 3: the four phases take real time, and without this the
 * app reads as laggy rather than busy. Hosted once at the navigation root so a
 * mint that starts on the game screen still finishes visibly on Home.
 *
 * The banner is display-only: it narrates the pipeline, nothing more.
 */
@Composable
fun ChainStatusBanner(modifier: Modifier = Modifier) {
    val activity by ChainEvents.activity.collectAsState()

    // Terminal states are informational, so they clear themselves. A mint in
    // flight stays put until it actually resolves.
    LaunchedEffect(activity) {
        when (activity) {
            is ChainActivity.Minted -> {
                // Long enough to read, short enough not to sit on the game.
                delay(3_000)
                ChainEvents.clear()
            }
            is ChainActivity.Failed -> {
                delay(4500)
                ChainEvents.clear()
            }
            is ChainActivity.Validating -> {
                // Belt and braces: if whatever started this never reports back,
                // the banner still goes away instead of spinning forever.
                delay(30_000)
                ChainEvents.clear()
            }
            else -> Unit
        }
    }

    AnimatedVisibility(
        visible = activity !is ChainActivity.Idle,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        val current = activity
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (current) {
                is ChainActivity.Failed -> Color(0xFF6B2B1F)
                is ChainActivity.Minted -> Color(0xFF2E5B34)
                else -> Color(0xFF4A2409)
            },
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                // Flick it away in any direction rather than waiting it out.
                .pointerInput(current) {
                    var drag = Offset.Zero
                    val up = 40.dp.toPx()
                    val side = 64.dp.toPx()
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDragEnd = { drag = Offset.Zero },
                        onDragCancel = { drag = Offset.Zero }
                    ) { change, delta ->
                        change.consume()
                        drag += delta
                        if (drag.y < -up || kotlin.math.abs(drag.x) > side) {
                            ChainEvents.clear()
                        }
                    }
                }
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(LightOrange.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (current) {
                        is ChainActivity.Validating -> CircularProgressIndicator(
                            color = LightOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        is ChainActivity.Minted -> Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = LightOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        is ChainActivity.Failed -> Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = LightOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        else -> Unit
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (current) {
                            is ChainActivity.Validating -> current.label
                            is ChainActivity.Minted -> "Badge Successfully Minted"
                            is ChainActivity.Failed -> current.label
                            else -> ""
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
