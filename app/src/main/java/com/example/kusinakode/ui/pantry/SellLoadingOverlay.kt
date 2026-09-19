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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.kusinakode.R
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.ui.theme.BeVietnamPro
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val Gold = Color(0xFFFFD24A)
private val Cream = Color(0xFFE8C9A0)
private val VaultAspect = 1024f / 576f

private enum class FlySide { Left, Right, Bottom }

/**
 * Chef's Vault fills the overlay. While TRADING, the sold ingredients
 * keep flying in from the sides and up from the pantry toward the stall
 * — always in front of the boards, then shrink into the counter.
 */
@Composable
internal fun SellLoadingOverlay(
    ask: SellAsk,
    busy: Boolean = true,
    onFinished: () -> Unit = {}
) {
    val payout = remember(ask) { Animatable(0f) }
    val busyNow by rememberUpdatedState(busy)
    val finish by rememberUpdatedState(onFinished)
    val motion = rememberInfiniteTransition(label = "vault_trade")
    val breathe by motion.animateFloat(
        initialValue = 0.995f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "breathe"
    )
    val dots by motion.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "wait_dots"
    )
    val glow by motion.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "glow"
    )
    val cycle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2_400, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "flight"
    )

    LaunchedEffect(ask) {
        payout.snapTo(0f)
        delay(2_800)
        while (busyNow) delay(40)
        payout.animateTo(1f, tween(1_400, easing = LinearEasing))
        delay(420)
        finish()
    }

    val flyers = remember(ask) { flyersFor(ask) }
    val p = payout.value
    val paying = p > 0.02f

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes
                                .forEach { it.consume() }
                        }
                    }
                }
                .background(Color(0xF2100806)),
            contentAlignment = Alignment.Center
        ) {
            val vaultW = maxWidth * 0.92f
            val vaultH = vaultW / VaultAspect
            val spanX = maxWidth.value * 0.46f
            val fromBelow = maxHeight.value * 0.34f
            val counterY = vaultH.value * 0.14f

            Image(
                painter = painterResource(R.drawable.vault_trade),
                contentDescription = "Chef's Vault",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .zIndex(1f)
                    .width(vaultW)
                    .aspectRatio(VaultAspect)
                    .scale(if (paying) 1f else breathe)
            )

            if (!paying) {
                flyers.forEach { flyer ->
                    val t = FastOutSlowInEasing.transform(
                        flyerProgress(cycle, flyer.delay)
                    )
                    if (t <= 0f) return@forEach
                    val x = when (flyer.side) {
                        FlySide.Left -> lerp(-spanX, 0f, t)
                        FlySide.Right -> lerp(spanX, 0f, t)
                        FlySide.Bottom -> 0f
                    }
                    val y = when (flyer.side) {
                        FlySide.Bottom -> lerp(fromBelow, counterY, t)
                        else -> lerp(counterY * 0.2f, counterY, t)
                    }
                    val fade = if (t < 0.78f) 1f else (1f - (t - 0.78f) / 0.22f)
                    val scale = when (flyer.side) {
                        FlySide.Bottom -> lerp(1.55f, 0.38f, t)
                        else -> lerp(1.28f, 0.38f, t)
                    }
                    val jarMod = Modifier
                        .zIndex(3f)
                        .offset(x = x.dp, y = y.dp)
                        .scale(scale)
                        .alpha(fade.coerceIn(0f, 1f))
                    ComingLight(
                        pulse = glow,
                        modifier = jarMod.size(168.dp)
                    )
                    Box(
                        jarMod.size(128.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IngredientPhoto(
                            flyer.entry.ingredient,
                            Modifier.size(108.dp)
                        )
                    }
                }
            }

            if (paying) {
                Box(
                    Modifier
                        .zIndex(4f)
                        .size(180.dp)
                        .offset(y = lerp(10f, -48f, p).dp)
                        .alpha(p.coerceIn(0f, 1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Gold.copy(alpha = 0.55f), Color.Transparent)
                            ),
                            radius = size.minDimension * 0.48f
                        )
                    }
                    Text(
                        "+${ask.worthKk} KK",
                        color = Gold,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        letterSpacing = 1.4.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Gold.copy(alpha = 0.85f),
                                offset = Offset.Zero,
                                blurRadius = 28f
                            )
                        ),
                        modifier = Modifier.scale(0.82f + p * 0.22f)
                    )
                }
                repeat(6) { i ->
                    val pt = ((p - i * 0.07f) / 0.62f).coerceIn(0f, 1f)
                    if (pt <= 0f) return@repeat
                    Image(
                        painter = painterResource(R.drawable.ic_kk_pixel),
                        contentDescription = null,
                        modifier = Modifier
                            .zIndex(4f)
                            .offset(
                                x = lerp(-36f, 36f, i / 5f).dp,
                                y = lerp(22f, -80f, pt).dp
                            )
                            .size(22.dp)
                            .alpha((1f - pt) * 0.95f)
                    )
                }
            }

            Column(
                Modifier
                    .zIndex(5f)
                    .align(Alignment.Center)
                    .offset(y = vaultH / 2 + 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (paying) "SOLD" else "TRADING",
                    color = Gold,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 3.sp
                )
                if (!paying) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Please wait" + ".".repeat(dots.roundToInt().coerceIn(1, 3)),
                        color = Cream,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingLight(pulse: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Gold.copy(alpha = 0.42f * pulse),
                    Gold.copy(alpha = 0.16f * pulse),
                    Color.Transparent
                )
            ),
            radius = r
        )
        drawOval(
            color = Color.Black.copy(alpha = 0.28f * pulse),
            topLeft = Offset(center.x - r * 0.42f, center.y + r * 0.22f),
            size = Size(r * 0.84f, r * 0.28f)
        )
    }
}

private data class VaultFlyer(
    val entry: PantryEntry,
    val side: FlySide,
    val delay: Float
)

private fun flyersFor(ask: SellAsk): List<VaultFlyer> {
    val source = ask.entries
    if (source.isEmpty()) return emptyList()
    val n = ask.jars.coerceIn(1, 6)
    if (n == 1) {
        val e = source[0]
        return listOf(
            VaultFlyer(e, FlySide.Bottom, 0f),
            VaultFlyer(e, FlySide.Left, 0.18f),
            VaultFlyer(e, FlySide.Right, 0.36f)
        )
    }
    val sides = listOf(FlySide.Left, FlySide.Bottom, FlySide.Right)
    return List(n) { i ->
        VaultFlyer(
            entry = source[i % source.size],
            side = sides[i % sides.size],
            delay = i * 0.12f
        )
    }
}

private fun flyerProgress(cycle: Float, delay: Float): Float {
    var t = cycle - delay
    if (t < 0f) t += 1f
    return (t / 0.62f).coerceIn(0f, 1f)
}
