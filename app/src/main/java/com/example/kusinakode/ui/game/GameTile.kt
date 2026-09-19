package com.example.kusinakode.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.KusinaSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Frame 7 tile palette — shared with the keyboard so feedback reads the same.
// The poster palette is the palette: the splash, the auth wordmark and the
// onboarding ADOBO strip all read from these, so the board a player meets
// is coloured exactly like the tiles that introduced it.
val TileCorrectGreen = Color(0xFF5F8C45)
val TileSemiYellow = Color(0xFFD4A83A)
val TileWrongBrown = Color(0xFFB85C2E)
/** The resting square: a translucent wash, and the only look a tile has before ENTER. */
private val TileEmpty = Color(0xFFF1E2C6).copy(alpha = 0.26f)
private val RevealGold = Color(0xFFFFD166)

/** Reveal timings, shared so callers can wait out a full row flip. */
const val TILE_STAGGER_MS = 85
const val TILE_FLIP_MS = 360
/** Longest post-flip accent (the correct-tile bounce). */
const val TILE_ACCENT_MS = 480

/**
 * Game tile with a staggered flip reveal. Each verdict lands differently so
 * the row reads without reading: greens bounce, misplaced letters shimmy,
 * ruled-out letters sink and fade back.
 *
 * @param revealDelayMs stagger for this column, so a row flips left to right.
 */
@Composable
fun GameTile(
    letter: Char,
    state: TileState,
    modifier: Modifier = Modifier,
    revealDelayMs: Int = 0,
    /** True for a tile a Reveal power-up just filled — plays a burst. */
    revealBurst: Boolean = false,
    /**
     * Set false for decorative tiles (onboarding, the how-to-play legend).
     * Those callers run their own entrance animation, and layering this
     * tile's flip and bounce on top left them settling at uneven sizes.
     */
    animated: Boolean = true
) {
    val isEvaluated = state != TileState.Empty
    val ctx = LocalContext.current

    val flip = remember { Animatable(if (isEvaluated) 1f else 0f) }
    val scale = remember { Animatable(1f) }
    val shift = remember { Animatable(0f) }
    val halo = remember { Animatable(0f) }

    // Power-up reveal: a golden burst so it reads as bought, not guessed.
    LaunchedEffect(revealBurst) {
        if (!revealBurst) {
            halo.snapTo(0f)
            return@LaunchedEffect
        }
        flip.snapTo(1f)
        halo.snapTo(1f)
        launch {
            scale.snapTo(0.35f)
            scale.animateTo(1.35f, tween(180))
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        halo.animateTo(0f, tween(1000))
    }

    // Typing feedback: a quick pop as each letter drops in.
    LaunchedEffect(letter, animated) {
        if (!animated) return@LaunchedEffect
        if (!isEvaluated && letter != ' ') {
            scale.snapTo(0.7f)
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Verdict reveal: flip, then an accent that suits the result.
    LaunchedEffect(state, animated) {
        if (!animated) {
            // Sit at rest so every decorative tile renders identically.
            flip.snapTo(1f)
            scale.snapTo(1f)
            shift.snapTo(0f)
            return@LaunchedEffect
        }
        // A bought reveal has its own burst; don't also flip it.
        if (revealBurst) return@LaunchedEffect
        if (!isEvaluated) {
            flip.snapTo(0f)
            shift.snapTo(0f)
            return@LaunchedEffect
        }
        flip.snapTo(0f)
        delay(revealDelayMs.toLong())
        val cue = when (state) {
            TileState.Correct -> SoundFx.Cue.TileCorrect
            TileState.SemiCorrect -> SoundFx.Cue.TilePresent
            TileState.Wrong -> SoundFx.Cue.TileAbsent
            TileState.Empty -> null
        }
        cue?.let { SoundFx.play(ctx, it) }
        flip.animateTo(1f, tween(TILE_FLIP_MS))
        when (state) {
            TileState.Correct -> {
                scale.animateTo(1.18f, tween(110))
                scale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            TileState.SemiCorrect -> {
                // Nudge side to side: "right letter, wrong seat".
                repeat(2) {
                    shift.animateTo(7f, tween(70))
                    shift.animateTo(-7f, tween(70))
                }
                shift.animateTo(0f, tween(70))
            }
            else -> {
                scale.animateTo(0.92f, tween(120))
                scale.animateTo(1f, tween(160))
            }
        }
    }

    // First half of the flip shows the typed face, second half the verdict.
    val revealed = flip.value >= 0.5f
    val faceState = if (isEvaluated && !revealed) TileState.Empty else state
    val angle = if (flip.value < 0.5f) flip.value * 180f else (1f - flip.value) * 180f

    // High contrast swaps the green/amber pair for blue/orange, which stays
    // distinguishable under the common red-green colour deficiencies.
    val highContrast = KusinaSettings.prefs.collectAsState().value.highContrastTiles
    val correctColor = if (highContrast) Color(0xFF1D6FB8) else TileCorrectGreen
    val presentColor = if (highContrast) Color(0xFFE07B00) else TileSemiYellow

    val bg = when (faceState) {
        TileState.Correct -> correctColor
        TileState.SemiCorrect -> presentColor
        TileState.Wrong -> TileWrongBrown
        // Typing puts a letter in the square, not a colour on it. A typed tile
        // used to fill opaque brown, which read as a verdict of its own before
        // the row was even submitted; now it keeps the empty well and only the
        // flip on ENTER brings colour.
        TileState.Empty -> TileEmpty
    }

    // Every verdict tile carries the same weight, the way the KUSINA KODE
    // wordmark draws them: solid colour, hairline border, white letter. The
    // ruled-out tiles used to recede — no border, a half-faded letter — which
    // left the board's terracotta reading as a washed-out version of the
    // brand's, so the three colours no longer looked like one set.
    val borderAlpha = if (faceState == TileState.Empty && letter == ' ') 0.22f else 0.35f

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shift.value
                rotationX = angle
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (borderAlpha > 0f) {
                    Modifier.border(
                        1.5.dp,
                        Color.White.copy(alpha = borderAlpha),
                        RoundedCornerShape(10.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (letter != ' ') {
            Text(
                letter.uppercaseChar().toString(),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp
            )
        }
        // Golden halo fading out after a bought reveal.
        if (halo.value > 0.01f) {
            Box(
                Modifier
                    .matchParentSize()
                    .border(
                        width = (1 + 3 * halo.value).dp,
                        color = RevealGold.copy(alpha = halo.value),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .background(
                        RevealGold.copy(alpha = halo.value * 0.35f),
                        RoundedCornerShape(10.dp)
                    )
            )
        }
    }
}
