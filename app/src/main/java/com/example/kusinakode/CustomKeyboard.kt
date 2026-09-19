package com.example.kusinakode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.ui.game.TileCorrectGreen
import com.example.kusinakode.ui.game.TileSemiYellow
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.LightOrange

private val KeyDefault = Color(0xFFF1E2C6)
private val KeyWrong = Color(0xFF3A2B1C).copy(alpha = 0.85f)
private val BlastOrange = Color(0xFFFF7A2F)
private val BurntOrange = Color(0xFFCC6B1F)

/** Dark glass keyboard matching the Frame 7 game screen. */
@Composable
fun CustomKeyboard(
    keyStates: Map<Char, TileState>,
    onKeyClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    /** Letters the bomb just cleared — these play a blast on arrival. */
    justBombed: Set<Char> = emptySet(),
    /** Soft pulse on ENTER so a tutorial can point at the next action. */
    pulseEnter: Boolean = false
) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

    // Key widths already flex, but the heights were fixed, so the board above
    // pushed the action row off the bottom of a short phone and left a gap on
    // a tall one. Scale them with the screen instead, with a floor that keeps
    // every key comfortably tappable.
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val keyHeight = (screenHeight * 0.052f).dp.coerceIn(34.dp, 48.dp)
    val actionHeight = keyHeight + 4.dp
    val gap = (keyHeight.value * 0.11f).dp.coerceIn(3.dp, 6.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = gap),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(keyHeight),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEachIndexed { indexInRow, ch ->
                    val state = keyStates[ch] ?: TileState.Empty
                    KeyButton(
                        label = ch.toString(),
                        state = state,
                        enabled = state != TileState.Wrong,
                        // Ripple the blast across the row rather than firing
                        // every key at once.
                        blastDelayMs = if (ch in justBombed) indexInRow * 45 else -1,
                        onClick = { onKeyClick(ch) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // ENTER + BACKSPACE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(actionHeight),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            EnterKey(
                pulse = pulseEnter,
                onClick = onEnter,
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BurntOrange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onBackspace)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("⌫", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun EnterKey(
    pulse: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = if (pulse) {
        val pulseScale by rememberInfiniteTransition(label = "enter_pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(640), RepeatMode.Reverse),
            label = "enter_pulse_value"
        )
        pulseScale
    } else {
        1f
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkBrown,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (pulse) Modifier.border(2.dp, LightOrange, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "ENTER",
                color = LightOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    state: TileState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Milliseconds to wait before playing the bomb blast; -1 to skip it. */
    blastDelayMs: Int = -1
) {
    val (bg, fg) = when (state) {
        TileState.Correct -> TileCorrectGreen to Color.White
        TileState.SemiCorrect -> TileSemiYellow to Color.White
        TileState.Wrong -> KeyWrong to Color.White.copy(alpha = 0.3f)
        TileState.Empty -> KeyDefault to DarkBrown
    }

    // Bomb blast: a jolt, a shake, then the key collapses into its spent state.
    val scale = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(blastDelayMs) {
        if (blastDelayMs < 0) return@LaunchedEffect
        delay(blastDelayMs.toLong())
        flash.snapTo(1f)
        launch {
            scale.animateTo(1.28f, tween(90))
            scale.animateTo(0.82f, tween(110))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            repeat(3) {
                shake.animateTo(6f, tween(45))
                shake.animateTo(-6f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
        flash.animateTo(0f, tween(650))
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shake.value
            }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                textAlign = TextAlign.Center
            )
            // Heat flare left behind by the blast.
            if (flash.value > 0.01f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            BlastOrange.copy(alpha = flash.value * 0.85f),
                            RoundedCornerShape(10.dp)
                        )
                )
            }
        }
    }
}
