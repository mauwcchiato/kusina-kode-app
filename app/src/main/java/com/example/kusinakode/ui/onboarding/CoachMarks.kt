package com.example.kusinakode.ui.onboarding

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import kotlin.math.roundToInt

/**
 * One stop on a guided walkthrough.
 *
 * [anchorKey] names the element to spotlight. A step whose anchor was never
 * registered — or is off-screen — still shows, just centred without a
 * cutout, so a tour never silently loses a step.
 */
data class CoachStep(
    val anchorKey: String? = null,
    val title: String,
    val body: String
)

/**
 * Collects the on-screen bounds of anchored elements.
 *
 * Compose gives no way to ask "where did that composable end up?" after the
 * fact, so each participating element reports its own position via
 * [coachAnchor] and the overlay reads them back by key.
 */
@Stable
class CoachAnchors {
    internal val bounds = mutableStateMapOf<String, Rect>()

    operator fun get(key: String?): Rect? = key?.let { bounds[it] }
}

@Composable
fun rememberCoachAnchors(): CoachAnchors = remember { CoachAnchors() }

/** Registers this element's window bounds under [key] for the overlay. */
fun Modifier.coachAnchor(key: String, anchors: CoachAnchors): Modifier =
    this.onGloballyPositioned { anchors.bounds[key] = it.boundsInWindow() }

/**
 * A dim-the-screen, spotlight-one-thing walkthrough.
 *
 * Renders as a window [Popup] so the dim covers the bottom navigation and
 * the cutout uses the same coordinate space as the anchors.
 */
@Composable
fun CoachMarkOverlay(
    steps: List<CoachStep>,
    anchors: CoachAnchors,
    onFinish: () -> Unit
) {
    if (steps.isEmpty()) return

    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps.getOrNull(index) ?: return
    val target = anchors[step.anchorKey]

    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInWindow() }
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
            val windowHeightPx = constraints.maxHeight.toFloat()
            val windowWidthPx = constraints.maxWidth.toFloat()
            val localTarget = target?.let { r ->
                Rect(
                    offset = Offset(r.left - overlayOrigin.x, r.top - overlayOrigin.y),
                    size = Size(r.width, r.height)
                )
            }
            val localStats = anchors["home_stats"]?.let { r ->
                Rect(
                    offset = Offset(r.left - overlayOrigin.x, r.top - overlayOrigin.y),
                    size = Size(r.width, r.height)
                )
            }
            val gapPx = with(density) { 16.dp.toPx() }
            val minCardPx = with(density) { 168.dp.toPx() }
            val sidePadPx = with(density) { 20.dp.toPx() }
            val estimatedCard = cardHeightPx.takeIf { it > 0f } ?: minCardPx

            val cardTopPx = localTarget?.let { spot ->
                val below = spot.bottom + gapPx
                val above = spot.top - gapPx - estimatedCard
                val fitsBelow = below + estimatedCard < windowHeightPx - 12f
                val fitsAbove = above >= 12f
                when {
                    // Prefer sitting next to the hole: below if it fits, else above.
                    fitsBelow -> below
                    fitsAbove -> above.coerceAtLeast(12f)
                    else -> 12f
                }
            } ?: (windowHeightPx / 2f - estimatedCard / 2f)

            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                drawRect(Color.Black.copy(alpha = 0.78f))
                localTarget?.let { r ->
                    val isWallet = step.anchorKey == "home_wallet"
                    val pad = if (isWallet) 6.dp.toPx() else 10.dp.toPx()
                    val padded = Rect(
                        r.left - pad,
                        r.top - pad,
                        r.right + pad,
                        r.bottom + pad
                    )
                    val clip = if (isWallet) {
                        null
                    } else {
                        localStats?.takeIf { it.overlaps(padded) }
                    }
                    val holeRect = clip?.let { padded.intersect(it) } ?: padded
                    if (holeRect.width <= 0f || holeRect.height <= 0f) return@let
                    val corner = if (isWallet) {
                        14.dp.toPx()
                    } else {
                        10.dp.toPx().coerceAtMost(holeRect.minDimension / 4f)
                    }
                    val ring = 3.dp.toPx()
                    val topLeft = Offset(holeRect.left, holeRect.top)
                    val hole = Size(holeRect.width, holeRect.height)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = topLeft,
                        size = hole,
                        cornerRadius = CornerRadius(corner),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = LightOrange,
                        topLeft = topLeft,
                        size = hole,
                        cornerRadius = CornerRadius(corner),
                        style = Stroke(width = ring)
                    )
                }
            }

            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(sidePadPx.roundToInt(), cardTopPx.roundToInt()) }
                    .width(with(density) { (windowWidthPx - sidePadPx * 2).toDp() })
                    .onGloballyPositioned { cardHeightPx = it.size.height.toFloat() }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFFDF9),
                    shadowElevation = 12.dp
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "STEP ${index + 1} OF ${steps.size}",
                            color = DarkBrown.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            step.title,
                            style = MaterialTheme.typography.titleMedium.copy(color = DarkBrown),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            step.body,
                            style = MaterialTheme.typography.bodyMedium.copy(color = HintGray),
                            lineHeight = 21.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = clickSfx(onFinish)) {
                                Text("Skip", color = HintGray, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = clickSfx {
                                    if (index >= steps.lastIndex) onFinish() else index++
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkBrown,
                                    contentColor = LightOrange
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    if (index >= steps.lastIndex) "Got it" else "Next",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
