package com.example.kusinakode.ui.tutorial

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.KusinaSettings
import com.example.kusinakode.SoundFx
import com.example.kusinakode.R
import com.example.kusinakode.ui.components.DialogueGold
import com.example.kusinakode.ui.components.DialogueInk
import com.example.kusinakode.ui.components.DialogueNamePlate
import com.example.kusinakode.ui.components.dialoguePlate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A beat the chef speaks: one or more lines he reads out in turn.
 *
 * Held as a list rather than a string because the teaching moments are
 * genuinely several sentences, and a visual novel earns its pacing by letting
 * the reader turn each one over before the next arrives.
 */
data class ChefBeat(
    val lines: List<String>,
    /** The recorded clip for each line, by position; a missing one is spoken by TTS. */
    val clips: List<Int> = emptyList()
)

/** Breath between one spoken line and the next. */
private const val LINE_PAUSE_MS = 1_200L

/**
 * Visual-novel dialogue layer: the chef stands over the board and speaks.
 *
 * Each line plays its recorded clip (or, without one, is read aloud by
 * text-to-speech) through [ChefVoice] and the chef moves on by
 * himself once it lands; after the last line the screen is handed back via
 * [onFinished]. A tap still skips ahead for anyone who reads faster than he
 * talks. With sound off he stays quiet and lines advance at reading pace.
 * Nothing here decides *when* to speak; the caller owns that.
 */
@Composable
fun ChefDialogue(
    beat: ChefBeat,
    reduceMotion: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voice = remember { ChefVoice(context) }
    DisposableEffect(voice) { onDispose { voice.shutdown() } }
    // Music under the voice: quieter for as long as the chef is on screen,
    // back to its usual level the moment he hands the board back.
    DisposableEffect(Unit) {
        SoundFx.duckBgm(true)
        onDispose { SoundFx.duckBgm(false) }
    }

    var cursor by remember(beat) { mutableIntStateOf(0) }
    val line = beat.lines.getOrElse(cursor) { "" }

    var typed by remember(beat, cursor) { mutableIntStateOf(0) }
    var talking by remember(beat, cursor) { mutableStateOf(false) }
    // Auto-advance and a tap can land together on the last line; hand the
    // screen back only once.
    var handedBack by remember(beat) { mutableStateOf(false) }

    val advance: () -> Unit = {
        when {
            cursor < beat.lines.lastIndex -> cursor += 1
            !handedBack -> {
                handedBack = true
                onFinished()
            }
        }
    }

    LaunchedEffect(beat, cursor, reduceMotion) {
        val typing = launch {
            if (reduceMotion) {
                typed = line.length
                return@launch
            }
            typed = 0
            for (i in 1..line.length) {
                typed = i
                delay(if (line[i - 1] == ' ') 12L else 22L)
            }
        }

        val prefs = KusinaSettings.prefs.value
        talking = true
        val clip = beat.clips.getOrNull(cursor)
        val spoke = prefs.soundEffects && (
            (clip != null && voice.playClip(clip, prefs.sfxVolume)) ||
                voice.speak(line, prefs.sfxVolume)
            )
        if (!spoke) {
            // No voice to pace by, so give the eye roughly what the ear would get.
            delay((line.length * 55L).coerceAtLeast(1_500L))
        }
        typing.join()
        talking = false

        delay(LINE_PAUSE_MS)
        advance()
    }

    // Talking pose while he is speaking, resting pose once the line has
    // landed — the sprite swap is the mouth animation.
    val pose = if (talking) R.drawable.tutorial_chef_guide else R.drawable.tutorial_chef_idle

    val bob by rememberInfiniteTransition(label = "chef_bob").animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 6f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "chef_bob_y"
    )
    Box(
        modifier
            .fillMaxSize()
            // The whole layer is the skip target, the way a VN screen is — a
            // small "next" button would be a worse version of the same thing.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = advance
            )
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color(0xCC120702),
                    1f to Color(0xF00E0602)
                )
            )
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            // The chef stands on the plate's top edge, so he reads as being in
            // front of the board rather than pasted onto the box.
            Image(
                painter = painterResource(pose),
                contentDescription = "The kitchen chef",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .height(210.dp)
                    .offset(y = 22.dp)
                    .graphicsLayer { translationY = -bob }
                    .semantics { contentDescription = "The kitchen chef" }
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .heightIn(min = 128.dp)
                    .dialoguePlate()
                    .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 16.dp)
            ) {
                Column {
                    // Speaker's name above the line, as a script would set it.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .drawBehind { drawCircle(DialogueNamePlate) }
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "Chef",
                            color = DialogueGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        line.take(typed),
                        color = DialogueInk,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

    }
}
