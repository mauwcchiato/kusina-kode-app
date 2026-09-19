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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.ui.components.DialogueGold
import com.example.kusinakode.ui.components.DialogueInk
import com.example.kusinakode.ui.components.DialogueNamePlate
import com.example.kusinakode.ui.components.dialoguePlate
import kotlinx.coroutines.delay

/**
 * A beat the chef speaks: one or more lines the player taps through.
 *
 * Held as a list rather than a string because the teaching moments are
 * genuinely several sentences, and a visual novel earns its pacing by letting
 * the reader turn each one over before the next arrives.
 */
data class ChefBeat(val lines: List<String>)

/**
 * Visual-novel dialogue layer: the chef stands over the board and speaks,
 * and the player taps to move him along.
 *
 * A tap does what a tap does in any VN — first it finishes the line being
 * typed, then it advances to the next one, and on the last line it hands the
 * screen back via [onFinished]. Nothing here decides *when* to speak; the
 * caller owns that.
 */
@Composable
fun ChefDialogue(
    beat: ChefBeat,
    reduceMotion: Boolean,
    onFinished: () -> Unit,
    /** What the last line offers to do — play on, or move to the next screen. */
    finalLabel: String = "TAP TO PLAY",
    modifier: Modifier = Modifier
) {
    var cursor by remember(beat) { mutableIntStateOf(0) }
    val line = beat.lines.getOrElse(cursor) { "" }

    var typed by remember(beat, cursor) { mutableIntStateOf(0) }
    val done = typed >= line.length

    LaunchedEffect(line, reduceMotion) {
        if (reduceMotion) {
            typed = line.length
            return@LaunchedEffect
        }
        typed = 0
        for (i in 1..line.length) {
            typed = i
            delay(if (line[i - 1] == ' ') 12L else 22L)
        }
    }

    // Talking pose while the words are still arriving, resting pose once the
    // line has landed — the sprite swap is the mouth animation.
    val pose = if (done) R.drawable.tutorial_chef_idle else R.drawable.tutorial_chef_guide

    val bob by rememberInfiniteTransition(label = "chef_bob").animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 6f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "chef_bob_y"
    )
    val chevron by rememberInfiniteTransition(label = "chef_chevron").animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 4f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "chef_chevron_y"
    )

    val advance: () -> Unit = {
        when {
            !done -> typed = line.length
            cursor < beat.lines.lastIndex -> cursor += 1
            else -> onFinished()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            // The whole layer is the tap target, the way a VN screen is — a
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

                if (done) {
                    Row(
                        Modifier.align(Alignment.BottomEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (cursor < beat.lines.lastIndex) "TAP" else finalLabel,
                            color = DialogueGold.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(12.dp)
                                .graphicsLayer { translationY = chevron }
                                .drawBehind {
                                    // A downward chevron, the VN "there's more".
                                    val w = size.width
                                    val h = size.height
                                    drawLine(
                                        DialogueGold,
                                        Offset(w * 0.1f, h * 0.3f),
                                        Offset(w * 0.5f, h * 0.75f),
                                        strokeWidth = w * 0.16f
                                    )
                                    drawLine(
                                        DialogueGold,
                                        Offset(w * 0.9f, h * 0.3f),
                                        Offset(w * 0.5f, h * 0.75f),
                                        strokeWidth = w * 0.16f
                                    )
                                }
                        )
                    }
                }
            }
        }

    }
}
