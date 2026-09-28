package com.example.kusinakode.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.R
import com.example.kusinakode.domain.model.Region
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One-shot flag: set to the level id the player is heading into when they tap
 * NEXT LEVEL on the win screen, so that round — and only that round — greets
 * them with the dropping sign. The destination round reads it once and clears
 * it, so a plain Play Now or a resume never triggers the banner.
 */
object NextLevelCue {
    var armedForLevel: Int? = null
}

/**
 * The wooden "NEXT LEVEL" sign that drops from the very top of the screen when
 * a new level is opened from the win screen. The round stays visible under a
 * dim while the next dish's [ingredientArt] bursts out of the sign, then it
 * lifts away. A tap anywhere calls [onSkip] to lift it early.
 */
@Composable
fun NextLevelBanner(
    show: Boolean,
    modifier: Modifier = Modifier,
    ingredientArt: List<Int> = emptyList(),
    /** The player-facing level number shown big under the sign; null hides it. */
    levelNumber: Int? = null,
    regionName: String = "",
    /** Which island is being entered; picks the number's colours. */
    region: Region? = null,
    /**
     * False for a plain level start (from the map, Home or Continue): just the
     * big level number, with no sign and no ingredient burst.
     */
    withSign: Boolean = true,
    /** Where to centre the level number, in window coordinates: the tile grid. */
    numberCenter: Offset? = null,
    onSkip: () -> Unit = {}
) {
    Box(modifier.fillMaxSize()) {
        // A dim, not a solid fill: the round shows through underneath.
        AnimatedVisibility(
            visible = show,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(280))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BannerBackdrop)
                    .pointerInput(Unit) { detectTapGestures { onSkip() } }
            )
        }
        // Behind the sign, so the ingredients fly out from under it and never
        // cover the words. A Canvas takes no touches, so taps still skip.
        AnimatedVisibility(
            visible = show && withSign && ingredientArt.isNotEmpty(),
            enter = fadeIn(tween(1)),
            exit = fadeOut(tween(280))
        ) {
            IngredientBurst(ingredientArt, Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            AnimatedVisibility(
                visible = show && withSign,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessLow
                    )
                ) { full -> -full } + fadeIn(),
                exit = slideOutVertically { full -> -full } + fadeOut()
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val imageHeight = maxWidth * (562f / 1000f)
                    // Chains meet the top edge like a hanging banner. A few dp
                    // stay on screen so the wooden bar is not sliced off.
                    val pullUp = (
                        imageHeight * (88f / 562f) * SIGN_SCALE - 8.dp
                    ).coerceAtLeast(0.dp)
                    Image(
                        painter = painterResource(R.drawable.next_level_sign),
                        contentDescription = "Next level",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = -pullUp)
                            .graphicsLayer {
                                scaleX = SIGN_SCALE
                                scaleY = SIGN_SCALE
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                    )
                }
            }
        }
        if (levelNumber != null) {
            AnimatedVisibility(
                visible = show,
                enter = fadeIn(tween(1)),
                exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.6f)
            ) {
                // Centred on the tile grid it is introducing, so the number
                // flashes where the round is about to appear. Falls back to
                // the centre of the screen until the grid has been measured.
                var origin by remember { mutableStateOf(Offset.Zero) }
                Box(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { origin = it.positionInWindow() }
                        .layout { measurable, constraints ->
                            val badge = measurable.measure(
                                constraints.copy(minWidth = 0, minHeight = 0)
                            )
                            val at = numberCenter?.let { it - origin }
                                ?: Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                badge.place(
                                    (at.x - badge.width / 2f).roundToInt(),
                                    (at.y - badge.height / 2f).roundToInt()
                                )
                            }
                        }
                ) {
                    LevelNumberBadge(
                        levelNumber,
                        regionName,
                        region,
                        // With the sign, wait for it to land; alone, pop in at once.
                        startDelayMs = if (withSign) 520L else 120L
                    )
                }
            }
        }
    }
}

private data class Spark(
    val angle: Float,
    /** Distance from the centre as a fraction of the badge radius. */
    val dist: Float,
    val phase: Float,
    val sizeDp: Float
)

private data class Streak(val angle: Float, val phase: Float, val curl: Float)

/**
 * The level number, big, popping in under the sign once it lands, inside a
 * loop of gold sparkles: twinkling four-point sparks plus thin streaks that
 * curl outward and fade.
 */
@Composable
private fun LevelNumberBadge(
    number: Int,
    regionName: String,
    region: Region?,
    startDelayMs: Long = 520L
) {
    val glow = islandGlow(region)
    val pop = remember { Animatable(0f) }
    // One-shot flash and ring of big sparks thrown out as the number lands.
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // After the sign has dropped in, so the two do not land together.
        delay(startDelayMs)
        launch {
            // Low damping: it punches well past full size, then settles.
            pop.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMedium))
        }
        burst.animateTo(1f, tween(700, easing = LinearEasing))
    }
    val burstSparks = remember {
        List(BURST_SPARKS) { i ->
            Spark(
                angle = (i / BURST_SPARKS.toFloat() * 2f * PI).toFloat() + Random.nextFloat() * 0.3f,
                dist = 0.9f + Random.nextFloat() * 0.5f,
                phase = 0f,
                sizeDp = 12f + Random.nextFloat() * 10f
            )
        }
    }
    val loop = rememberInfiniteTransition(label = "sparkle")
    val t by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing)),
        label = "sparkle_t"
    )
    val sparks = remember {
        List(34) {
            Spark(
                angle = (Random.nextFloat() * 2f * PI).toFloat(),
                dist = 0.35f + Random.nextFloat() * 0.65f,
                phase = Random.nextFloat(),
                sizeDp = 8f + Random.nextFloat() * 10f
            )
        }
    }
    val streaks = remember {
        List(STREAKS) { i ->
            Streak(
                angle = (i / STREAKS.toFloat() * 2f * PI).toFloat() + Random.nextFloat() * 0.6f,
                phase = i / STREAKS.toFloat(),
                curl = if (i % 2 == 0) 1f else -1f
            )
        }
    }

    Box(Modifier.size(BadgeSize), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = pop.value.coerceIn(0f, 1f) }
        ) {
            val radius = size.minDimension / 2f
            sparks.forEach { s ->
                val local = (t + s.phase) % 1f
                // 0 → 1 → 0 over its cycle: grows, glints, shrinks away.
                val glint = sin(local * PI).toFloat()
                if (glint <= 0.02f) return@forEach
                val r = radius * s.dist * (0.85f + 0.15f * local)
                val p = center + Offset(cos(s.angle) * r, sin(s.angle) * r)
                drawSparkle(p, s.sizeDp.dp.toPx() * glint, glint, glow.spark)
            }
            streaks.forEach { k ->
                val local = (t + k.phase) % 1f
                val fade = sin(local * PI).toFloat()
                // A short tail of dots behind a head that spirals outward.
                for (j in 0 until STREAK_TAIL) {
                    val u = local - j * 0.022f
                    if (u < 0f) break
                    val ang = k.angle + k.curl * u * 1.7f
                    val r = radius * (0.25f + 0.75f * u)
                    val a = (1f - j / STREAK_TAIL.toFloat()) * fade
                    drawCircle(
                        color = glow.spark.copy(alpha = a.coerceIn(0f, 1f)),
                        radius = (4.5f - j * 0.28f).coerceAtLeast(1f).dp.toPx(),
                        center = center + Offset(cos(ang) * r, sin(ang) * r)
                    )
                }
            }
        }
        // The landing: a white-gold flash and a ring of big sparks flung out
        // past the badge's edge. Drawn unclipped, so they can leave the box.
        Canvas(Modifier.fillMaxSize()) {
            val b = burst.value
            if (b <= 0f || b >= 1f) return@Canvas
            val radius = size.minDimension / 2f
            val flash = (1f - b).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.85f * flash), glow.spark.copy(alpha = 0.5f * flash), Color.Transparent),
                    center = center,
                    radius = radius * (0.3f + 0.9f * b)
                ),
                radius = radius * (0.3f + 0.9f * b),
                center = center
            )
            // Fast out, easing to a stop as they fade.
            val travel = 1f - (1f - b) * (1f - b)
            burstSparks.forEach { s ->
                val r = radius * s.dist * travel
                val p = center + Offset(cos(s.angle) * r, sin(s.angle) * r)
                drawSparkle(p, s.sizeDp.dp.toPx() * (1f - 0.5f * b), flash, glow.spark)
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            }
        ) {
            if (regionName.isNotBlank()) {
                Text(
                    regionName.uppercase(),
                    color = Color.White,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 5.sp
                )
            }
            Text(
                number.toString(),
                color = glow.number,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 132.sp,
                lineHeight = 136.sp,
                style = TextStyle(
                    shadow = Shadow(glow.shadow, Offset(0f, 10f), blurRadius = 0f)
                )
            )
        }
    }
}

/** A four-point star with a white-hot core, [glint] 0..1 fading it. */
private fun DrawScope.drawSparkle(at: Offset, arm: Float, glint: Float, tint: Color) {
    if (arm <= 0f) return
    val color = tint.copy(alpha = glint.coerceIn(0f, 1f))
    val stroke = arm * 0.22f
    drawLine(color, at - Offset(arm, 0f), at + Offset(arm, 0f), stroke, StrokeCap.Round)
    drawLine(color, at - Offset(0f, arm), at + Offset(0f, arm), stroke, StrokeCap.Round)
    drawCircle(Color.White.copy(alpha = glint.coerceIn(0f, 1f)), arm * 0.22f, at)
}

private data class FlyingIngredient(
    val artIndex: Int,
    /** Launch direction in radians; 0 is right, -PI/2 is straight up. */
    val angle: Float,
    /** Launch speed in dp per second. */
    val speed: Float,
    val sizeDp: Float,
    /** Total rotation over the flight, in degrees. */
    val spin: Float,
    val delayMs: Int
)

/**
 * Ingredient photos flung out from behind the sign in two waves, mostly up in
 * a fountain, then pulled down the screen by gravity. One shot per appearance.
 */
@Composable
private fun IngredientBurst(art: List<Int>, modifier: Modifier = Modifier) {
    val painters = art.map { painterResource(it) }
    val pieces = remember(art) {
        List(BURST_COUNT) { i ->
            // Most are thrown upward in a wide fountain so they arc over and
            // rain down the whole screen; the rest scatter every way.
            val angle = if (Random.nextFloat() < 0.7f) {
                -PI / 2 + (Random.nextFloat() * 2f - 1f) * 1.25f
            } else {
                Random.nextFloat() * 2f * PI
            }
            FlyingIngredient(
                artIndex = i % art.size,
                angle = angle.toFloat(),
                speed = 260f + Random.nextFloat() * 520f,
                sizeDp = 30f + Random.nextFloat() * 30f,
                spin = Random.nextFloat() * 900f - 450f,
                // Two waves: a big pop as the sign lands, then a second one.
                delayMs = if (i % 2 == 0) Random.nextInt(0, 150) else Random.nextInt(450, 650)
            )
        }
    }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // Let the sign land first so the burst reads as coming out of it.
        delay(380)
        clock.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
    }

    Canvas(modifier) {
        val originX = size.width / 2f
        // The middle of the hanging sign's board: it is drawn width-relative
        // and pinned to the top, so its centre sits about a quarter-width down.
        val originY = size.width * 0.24f
        val dpPx = density
        pieces.forEach { p ->
            val tSec = (clock.value * BURST_MS - p.delayMs) / 1000f
            if (tSec <= 0f) return@forEach
            // Each piece's own flight, so the second wave still fades out
            // fully rather than being cut off when the clock ends.
            val life = tSec / ((BURST_MS - p.delayMs) / 1000f)
            val x = originX + cos(p.angle) * p.speed * dpPx * tSec
            val y = originY + sin(p.angle) * p.speed * dpPx * tSec +
                0.5f * GRAVITY_DP * dpPx * tSec * tSec
            // Pop from small to full size, fade out over the last third.
            val grow = (tSec / 0.15f).coerceIn(0.4f, 1f)
            val alpha = if (life > 0.66f) ((1f - life) / 0.34f).coerceIn(0f, 1f) else 1f
            val s = p.sizeDp * dpPx * grow
            translate(x - s / 2f, y - s / 2f) {
                rotate(p.spin * life, pivot = Offset(s / 2f, s / 2f)) {
                    with(painters[p.artIndex]) { draw(Size(s, s), alpha = alpha) }
                }
            }
        }
    }
}

private val BannerBackdrop = Color.Black.copy(alpha = 0.72f)

/** The PNG has wide transparent margins, so it is drawn larger than the width. */
private const val SIGN_SCALE = 1.45f

private const val BURST_COUNT = 72
private const val BURST_MS = 2_400
private const val GRAVITY_DP = 1_100f

/** The level number's colours: its fill, its drop shadow, and its sparkles. */
private data class IslandGlow(val number: Color, val shadow: Color, val spark: Color)

/**
 * Each island greets its levels in its own colour. Luzon is the green of its
 * rice terraces, Visayas the turquoise of its seas, Mindanao the royal violet
 * of the south. Gold stays the fallback for anything else.
 */
private fun islandGlow(region: Region?): IslandGlow = when (region) {
    Region.LUZON -> IslandGlow(
        number = Color(0xFF8BE05A),
        shadow = Color(0xFF1F4A12),
        spark = Color(0xFFC6F59A)
    )
    Region.VISAYAS -> IslandGlow(
        number = Color(0xFF4FD6EC),
        shadow = Color(0xFF0B3A57),
        spark = Color(0xFFA9F1FF)
    )
    Region.MINDANAO -> IslandGlow(
        number = Color(0xFFD08BFF),
        shadow = Color(0xFF3A1060),
        spark = Color(0xFFF0C9FF)
    )
    null -> IslandGlow(
        number = SparkGold,
        shadow = NumberShadow,
        spark = SparkGold
    )
}

private val SparkGold = Color(0xFFFFC23D)
private val NumberShadow = Color(0xFF5A2A0C)
private const val STREAK_TAIL = 14
private const val STREAKS = 7
private const val BURST_SPARKS = 16

/** The level number's sparkle field, sized to sit over the tile grid. */
private val BadgeSize = 230.dp
