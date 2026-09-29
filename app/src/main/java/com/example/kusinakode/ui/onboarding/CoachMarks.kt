package com.example.kusinakode.ui.onboarding

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.ui.theme.BeVietnamPro
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
import androidx.compose.runtime.LaunchedEffect
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
    val body: String,
    /**
     * Sit the card at the foot of the screen instead of beside the spotlight,
     * for a spotlight too tall to leave room above or below it.
     */
    val cardAtBottom: Boolean = false,
    /** Spotlight a round element with a round hole rather than a square one. */
    val round: Boolean = false
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
    onFinish: () -> Unit,
    onStepChange: (CoachStep) -> Unit = {}
) {
    if (steps.isEmpty()) return

    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps.getOrNull(index) ?: return
    LaunchedEffect(index) { onStepChange(step) }
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

            val cardTopPx = if (step.cardAtBottom) {
                windowHeightPx - estimatedCard - with(density) { 28.dp.toPx() }
            } else localTarget?.let { spot ->
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
                    val isNav = step.anchorKey == "home_wallet" ||
                        step.anchorKey == "home_notif" ||
                        step.anchorKey == "explore_home" ||
                        step.anchorKey == "wallet_profile"
                    val pad = if (isNav) 6.dp.toPx() else 10.dp.toPx()
                    val padded = Rect(
                        r.left - pad,
                        r.top - pad,
                        r.right + pad,
                        r.bottom + pad
                    )
                    val clip = if (isNav) {
                        null
                    } else {
                        localStats?.takeIf { it.overlaps(padded) }
                    }
                    val holeRect = clip?.let { padded.intersect(it) } ?: padded
                    if (holeRect.width <= 0f || holeRect.height <= 0f) return@let
                    val corner = if (isNav) {
                        14.dp.toPx()
                    } else {
                        10.dp.toPx().coerceAtMost(holeRect.minDimension / 4f)
                    }
                    val ring = 3.dp.toPx()
                    if (step.round) {
                        // A circle hugging the element, for a round button.
                        val radius = maxOf(r.width, r.height) / 2f + 6.dp.toPx()
                        drawCircle(Color.Transparent, radius, r.center, blendMode = BlendMode.Clear)
                        drawCircle(LightOrange, radius, r.center, style = Stroke(width = ring))
                        return@let
                    }
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
                // The same parchment plate and carved buttons the app's dialogs
                // wear, so the tour reads as part of the kitchen rather than a
                // plain system tooltip.
                ParchmentCard(contentPadding = 20.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "STEP ${index + 1} OF ${steps.size}",
                                color = CoachStepInk,
                                fontFamily = BeVietnamPro,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp
                            )
                            Spacer(Modifier.weight(1f))
                            // Progress beads: filled up to this step.
                            steps.indices.forEach { i ->
                                Box(
                                    Modifier
                                        .padding(start = 4.dp)
                                        .size(if (i == index) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (i <= index) CoachBeadOn else CoachBeadOff)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            step.title,
                            color = CoachTitleInk,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            step.body,
                            color = CoachBodyInk,
                            fontFamily = BeVietnamPro,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Skip is just a word, still tappable: the tour
                            // should not ask for a decision on every step. Its
                            // touch area is padded out so it is easy to hit.
                            // Hidden on the last step, where Got it ends it anyway.
                            if (index < steps.lastIndex) {
                                Text(
                                    "Skip",
                                    color = CoachSkipInk,
                                    fontFamily = BeVietnamPro,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = clickSfx(onFinish))
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            // A small carved Next, sized to its word.
                            KusinaButton(
                                label = if (index >= steps.lastIndex) "Got it" else "Next",
                                onClick = clickSfx {
                                    if (index >= steps.lastIndex) onFinish() else index++
                                },
                                tone = KusinaButtonTone.Brown,
                                height = 40.dp,
                                fontSize = 13.sp,
                                modifier = Modifier.width(CoachNextWidth)
                            )
                        }
                    }
                }
            }
        }
    }

// Inks for the parchment walkthrough card.
private val CoachStepInk = Color(0xFFA4724C)
private val CoachTitleInk = Color(0xFF4A2412)
private val CoachBodyInk = Color(0xFF6B4A33)
private val CoachBeadOn = Color(0xFFA4724C)
private val CoachBeadOff = Color(0xFFD9C7A6)
private val CoachSkipInk = Color(0xFF8A6A4E)
/** Room for "Got it" on the carved pill; "Next" is centred in the same width. */
private val CoachNextWidth = 108.dp
