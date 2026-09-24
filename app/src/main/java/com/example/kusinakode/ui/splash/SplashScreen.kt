package com.example.kusinakode.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.kusinakode.R
import com.example.kusinakode.ui.auth.AuthLogo
import com.example.kusinakode.ui.branding.AllWordmarkTiles
import com.example.kusinakode.ui.branding.KodeTiles
import com.example.kusinakode.ui.branding.KusinaTiles
import com.example.kusinakode.ui.branding.WordmarkChip
import com.example.kusinakode.ui.branding.WordmarkTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

// ---- Timing, in one block so the whole sequence reads at a glance ----
private const val REVEAL_MS = 620          // phase 1: brown floods outward
private const val LOGO_POP_DELAY_MS = 220L // phase 2 starts while the reveal runs
private const val TILE_START_DELAY_MS = 560L
private const val TILE_STAGGER_MS = 70L    // phase 3: gap between letters
private const val HOLD_MS = 1_200L         // requested pause before handing over

/** Same as @color/splash_brown, so the window and the first frame match. */
private val SplashBrown = Color(0xFF4B321F)

private val LOGO_SIZE = 148.dp
private val TILE_SIZE = 40.dp

/**
 * The launch sequence: brown, then a circular reveal floods the wooden table
 * outward from behind the chef, who pops, and the wordmark drops in letter by
 * letter.
 *
 * [onFinished] fires once, guarded by a saveable flag so a rotation mid-
 * animation cannot navigate twice or replay a splash that already ran.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var completed by rememberSaveable { mutableStateOf(false) }

    val reveal = remember { Animatable(0f) }     // 0..1 of the full radius
    val logoScale = remember { Animatable(0.9f) }
    val tileDrop = remember { AllWordmarkTiles.map { Animatable(0f) } }

    // The reveal grows from the logo's centre, so it has to know where that is.
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    val config = LocalConfiguration.current
    val maxRadiusDp = remember(config.screenWidthDp, config.screenHeightDp) {
        hypot(config.screenWidthDp.toFloat(), config.screenHeightDp.toFloat())
    }

    LaunchedEffect(completed) {
        if (completed) {
            // Restored after a rotation: the splash already ran, so show its
            // end state and hand over rather than playing it again.
            reveal.snapTo(1f)
            logoScale.snapTo(1f)
            tileDrop.forEach { it.snapTo(1f) }
            onFinished()
            return@LaunchedEffect
        }

        // ---- Phase 1: the brown floods out ----
        launch { reveal.animateTo(1f, tween(REVEAL_MS, easing = FastOutSlowInEasing)) }

        // ---- Phase 2: the chef pops while the background is still filling ----
        launch {
            delay(LOGO_POP_DELAY_MS)
            logoScale.animateTo(1.1f, tween(180, easing = FastOutSlowInEasing))
            logoScale.animateTo(
                1f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
            )
        }

        // ---- Phase 3: letters drop in, one after another ----
        delay(TILE_START_DELAY_MS)
        tileDrop.forEachIndexed { i, anim ->
            launch {
                delay(i * TILE_STAGGER_MS)
                anim.animateTo(
                    1f,
                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                )
            }
        }
        delay(tileDrop.size * TILE_STAGGER_MS + 420L)

        delay(HOLD_MS)
        completed = true
    }

    Box(Modifier.fillMaxSize().background(SplashBrown)) {

        // The wooden table, clipped to a circle growing from behind the chef.
        // drawWithContent is one clip per frame rather than a recomposed shape,
        // which is what keeps this at 60fps.
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    if (origin == Offset.Unspecified) return@drawWithContent
                    val r = reveal.value * maxRadiusDp * density
                    if (r <= 0f) return@drawWithContent
                    clipPath(Path().apply { addOval(Rect(origin, r)) }) {
                        this@drawWithContent.drawContent()
                    }
                }
        )

        // A warm scrim so the wordmark stays legible over the grain.
        Box(
            Modifier
                .matchParentSize()
                .background(Color(0xFF2A1608).copy(alpha = 0.28f * reveal.value))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A little above true center so the chef + tiles sit between
            // the top of the screen and the tomatoes at the bottom.
            Spacer(Modifier.weight(0.88f))
            Box(
                Modifier
                    .onGloballyPositioned { c ->
                        // Root coordinates: the wooden layer fills the root Box,
                        // so the circle must be centred in that same space.
                        val b = c.boundsInRoot()
                        origin = Offset(b.center.x, b.center.y)
                    }
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
            ) {
                AuthLogo(size = LOGO_SIZE)
            }

            Spacer(Modifier.height(28.dp))

            TileRow(KusinaTiles, tileDrop.take(KusinaTiles.size))
            Spacer(Modifier.height(8.dp))
            TileRow(KodeTiles, tileDrop.drop(KusinaTiles.size).take(KodeTiles.size))
            Spacer(Modifier.weight(1.12f))
        }
    }
}

@Composable
private fun TileRow(tiles: List<WordmarkTile>, drops: List<Animatable<Float, *>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tiles.forEachIndexed { i, tile ->
            val p = drops.getOrNull(i)?.value ?: 1f
            WordmarkChip(
                tile = tile,
                size = TILE_SIZE,
                modifier = Modifier.graphicsLayer {
                    alpha = p
                    // Falls from above and overshoots slightly, which reads as
                    // "dropped" rather than "faded".
                    translationY = (1f - p) * -48f * density
                    scaleX = 0.8f + 0.2f * p
                    scaleY = 0.8f + 0.2f * p
                }
            )
        }
    }
}
