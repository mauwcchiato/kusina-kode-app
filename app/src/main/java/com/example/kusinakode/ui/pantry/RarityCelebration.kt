package com.example.kusinakode.ui.pantry

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.kusinakode.domain.pantry.Rarity
import kotlin.random.Random

/**
 * A rare or legendary pull should feel like an event, so those two tiers get
 * light behind the card and paper in front of it. Common and uncommon draws
 * stay quiet — if every pull celebrates, none of them do.
 */
internal val Rarity.isBigPull: Boolean
    get() = this == Rarity.RARE || this == Rarity.LEGENDARY

private fun celebrationColors(rarity: Rarity): List<Color> = when (rarity) {
    Rarity.LEGENDARY -> listOf(
        Color(0xFFFFD24A), Color(0xFFFFF0B8), Color(0xFFC77DFF),
        Color(0xFF9D4EDD), Color(0xFFFFFFFF)
    )
    else -> listOf(
        Color(0xFF6FA8DC), Color(0xFFCFE3F7), Color(0xFF2F6DB5),
        Color(0xFFFFFFFF), Color(0xFFA8C8E8)
    )
}

private fun rayTint(rarity: Rarity): Color =
    if (rarity == Rarity.LEGENDARY) Color(0xFFFFD24A) else Color(0xFF7FB2E8)

/**
 * Godrays behind the card. Drawn under the stack so the art stays readable —
 * light spilling from behind the frame, not a wash over it.
 */
@Composable
internal fun RarityRays(rarity: Rarity, modifier: Modifier = Modifier) {
    val tint = rayTint(rarity)
    val spin by rememberInfiniteTransition(label = "rays").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
        label = "ray_spin"
    )
    val breathe by rememberInfiniteTransition(label = "ray_pulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "ray_alpha"
    )
    val bloom = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        bloom.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }

    Canvas(modifier) {
        if (bloom.value <= 0f) return@Canvas
        val centre = Offset(size.width / 2f, size.height * 0.44f)
        val radius = size.maxDimension * 0.78f * bloom.value
        val rays = if (rarity == Rarity.LEGENDARY) 16 else 12
        val sweep = if (rarity == Rarity.LEGENDARY) 9f else 7f

        rotate(spin, centre) {
            repeat(rays) { i ->
                drawArc(
                    color = tint.copy(alpha = 0.16f * breathe * bloom.value),
                    startAngle = i * (360f / rays),
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(centre.x - radius, centre.y - radius),
                    size = Size(radius * 2f, radius * 2f)
                )
            }
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = 0.22f * bloom.value), Color.Transparent),
                center = centre,
                radius = radius * 0.62f
            ),
            radius = radius * 0.62f,
            center = centre
        )
    }
}

private data class Fleck(
    val xFraction: Float,
    val delayMs: Int,
    val colorIndex: Int,
    val width: Float,
    val drift: Float,
    val spin: Float
)

/**
 * Confetti over the card. A Canvas takes no pointer input, so the stack
 * underneath still turns on tap and swipe while this plays.
 */
@Composable
internal fun RarityConfetti(rarity: Rarity, modifier: Modifier = Modifier) {
    ConfettiBurst(
        colors = remember(rarity) { celebrationColors(rarity) },
        count = if (rarity == Rarity.LEGENDARY) 54 else 34,
        modifier = modifier
    )
}

/**
 * The paper itself, in whatever colours the occasion calls for. Shared so the
 * draw reveal and the daily wheel fall the same way rather than each growing
 * their own physics.
 */
@Composable
internal fun ConfettiBurst(
    colors: List<Color>,
    count: Int,
    modifier: Modifier = Modifier
) {
    val flecks = remember(colors, count) {
        List(count) {
            Fleck(
                xFraction = Random.nextFloat(),
                delayMs = Random.nextInt(0, 700),
                colorIndex = Random.nextInt(colors.size),
                width = Random.nextInt(6, 14).toFloat(),
                drift = Random.nextFloat() * 2f - 1f,
                spin = Random.nextFloat() * 720f - 360f
            )
        }
    }
    val totalMs = 2600
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(1f, tween(totalMs, easing = LinearEasing)) }

    Canvas(modifier) {
        flecks.forEach { f ->
            val t = ((clock.value * totalMs - f.delayMs) / 1800f).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val x = size.width * f.xFraction + f.drift * 70f * t
            val y = -30f + (size.height + 80f) * t
            val fade = if (t > 0.75f) (1f - t) / 0.25f else 1f
            rotate(degrees = f.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[f.colorIndex].copy(alpha = fade.coerceIn(0f, 1f)),
                    topLeft = Offset(x - f.width / 2f, y - f.width / 2f),
                    size = Size(f.width, f.width * 1.7f)
                )
            }
        }
    }
}
