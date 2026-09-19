package com.example.kusinakode.ui.pantry

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.kusinakode.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Diameter of the puck. Big enough to hit with a thumb, small enough to ignore. */
private val PuckSize = 58.dp

private val PuckRim = Color(0xFFE8C36A)
private val PuckDeep = Color(0xFF8A3E12)
private val PuckWarm = Color(0xFFCC6B1F)

/**
 * The always-there spin puck, in the spirit of AssistiveTouch.
 *
 * Floats above whatever screen is showing, can be dragged anywhere, and turns
 * on its own so it reads as a wheel rather than a button. Let go and it slides
 * to the nearer side, the way AssistiveTouch does — parked mid-screen it sits
 * over whatever is underneath, and the vertical position the player chose is
 * the part worth keeping.
 *
 * Position survives recomposition and is re-clamped to the current bounds, so
 * a rotation brings it back into view rather than losing it offscreen.
 *
 * @param onClick fired on a tap, never at the end of a drag.
 */
@Composable
fun FloatingSpinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val puckPx = with(density) { PuckSize.toPx() }
        val maxX = (constraints.maxWidth - puckPx).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - puckPx).coerceAtLeast(0f)
        val scope = rememberCoroutineScope()

        // Opens docked to the right, clear of the bottom nav but inside the
        // thumb's arc. Which side it ends up on is the player's to choose.
        val x = remember { Animatable(0f) }
        val y = remember { Animatable(0f) }
        var placed by remember { mutableStateOf(false) }
        LaunchedEffect(maxX, maxY) {
            if (!placed) {
                x.snapTo(maxX)
                y.snapTo(maxY * 0.62f)
                placed = true
            } else {
                // Bounds changed under it — a rotation, or the keyboard. Pull
                // it back inside rather than leaving it off the edge.
                x.snapTo(x.value.coerceIn(0f, maxX))
                y.snapTo(y.value.coerceIn(0f, maxY))
            }
        }

        val spin by rememberInfiniteTransition(label = "puck-spin").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(5200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "puck-angle"
        )

        Box(
            Modifier
                .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                .size(PuckSize)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(PuckWarm, PuckDeep)))
                .border(2.dp, PuckRim, CircleShape)
                // Two detectors, not one: detectDragGestures never reports a
                // gesture that stays inside touch slop, so a plain tap would
                // never reach onDragEnd. Taps get their own handler, and the
                // drag detector claims anything that actually moves.
                .pointerInput(Unit) {
                    detectTapGestures { onClick() }
                }
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragEnd = {
                            // Whichever side the puck's own centre is nearer.
                            val centre = x.value + puckPx / 2f
                            val target = if (centre < (maxX + puckPx) / 2f) 0f else maxX
                            scope.launch {
                                x.animateTo(
                                    target,
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    ) { change, drag ->
                        change.consume()
                        scope.launch {
                            x.snapTo((x.value + drag.x).coerceIn(0f, maxX))
                            y.snapTo((y.value + drag.y).coerceIn(0f, maxY))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            PixelArt(
                res = R.drawable.baul_closed,
                contentDescription = "Spin the palayok wheel",
                modifier = Modifier
                    .size(34.dp)
                    .rotate(spin)
            )
        }
    }
}
