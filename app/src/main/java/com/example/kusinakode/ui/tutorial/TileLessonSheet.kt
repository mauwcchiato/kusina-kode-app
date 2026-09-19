package com.example.kusinakode.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kusinakode.ui.branding.WordmarkChip
import com.example.kusinakode.ui.branding.WordmarkOutline
import com.example.kusinakode.ui.branding.WordmarkSolidOutline
import com.example.kusinakode.ui.branding.WordmarkTile
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard

/** Which tile colour the player tapped for an explanation. */
enum class TileLesson { RightSpot, WrongSpot, NotInIt }

private val SheetInk = Color(0xFF3E2723)
private val SheetMuted = Color(0xFF8A7157)
private val ExampleGround = Color(0xFFEFE3CF)

/**
 * The worked example is built on ADOBO rather than on this round's answer.
 *
 * The legend appears mid-round, so demonstrating with LUGAW would hand the
 * player the word they are still trying to guess.
 */
private const val EXAMPLE_ANSWER = "ADOBO"

/**
 * Explains one tile colour, with a guess the player can read off.
 *
 * Opened by tapping a chip in the colour legend — the legend says *what* the
 * colours are, this says what to do about them.
 */
@Composable
fun TileLessonSheet(
    lesson: TileLesson,
    green: Color,
    yellow: Color,
    brown: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        TileLessonCard(lesson, green, yellow, brown, onDismiss)
    }
}

/** The card itself, split out so it can be rendered outside a Dialog. */
@Composable
internal fun TileLessonCard(
    lesson: TileLesson,
    green: Color,
    yellow: Color,
    brown: Color,
    onDismiss: () -> Unit
) {
    val fill = when (lesson) {
        TileLesson.RightSpot -> green
        TileLesson.WrongSpot -> yellow
        TileLesson.NotInIt -> brown
    }
    val title = when (lesson) {
        TileLesson.RightSpot -> "Right letter, right spot"
        TileLesson.WrongSpot -> "Right letter, wrong spot"
        TileLesson.NotInIt -> "Not in the dish"
    }
    val body = when (lesson) {
        TileLesson.RightSpot ->
            "This letter is in the dish and you put it exactly where it belongs. " +
                "Keep it in that column for every guess that follows."
        TileLesson.WrongSpot ->
            "This letter is in the dish, but not in that column. " +
                "Use it again in your next guess — somewhere else."
        TileLesson.NotInIt ->
            "This letter isn't in the dish at all. " +
                "Cross it off and don't spend another guess on it."
    }
    // The guess that produces the colour being explained, against ADOBO.
    val guess = when (lesson) {
        TileLesson.RightSpot -> "APPLE"
        TileLesson.WrongSpot -> "BLIMP"
        TileLesson.NotInIt -> "TRUCK"
    }
    val marked = when (lesson) {
        TileLesson.RightSpot -> setOf(0)
        TileLesson.WrongSpot -> setOf(0)
        TileLesson.NotInIt -> setOf(0, 1, 2, 3, 4)
    }
    val caption = when (lesson) {
        TileLesson.RightSpot ->
            "ADOBO starts with A, and so does APPLE — so the A turns green."
        TileLesson.WrongSpot ->
            "ADOBO has a B, but it's the fourth letter. BLIMP opens with it, " +
                "so the B turns yellow."
        TileLesson.NotInIt ->
            "None of these letters appear in ADOBO, so the whole row turns brown."
    }

    ParchmentCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WordmarkChip(
                    tile = WordmarkTile(
                        when (lesson) {
                            TileLesson.RightSpot -> 'A'
                            TileLesson.WrongSpot -> 'B'
                            TileLesson.NotInIt -> 'T'
                        },
                        fill
                    ),
                    size = 56.dp,
                    outline = WordmarkSolidOutline
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    title,
                    color = SheetInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    color = SheetMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(ExampleGround, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "IF THE DISH WERE $EXAMPLE_ANSWER",
                            color = SheetMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            guess.forEachIndexed { i, ch ->
                                val lit = i in marked
                                if (lit) {
                                    WordmarkChip(
                                        tile = WordmarkTile(ch, fill),
                                        size = 38.dp,
                                        outline = WordmarkSolidOutline
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Color(0xFFDCCBB0),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                WordmarkOutline,
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            ch.toString(),
                                            color = SheetInk.copy(alpha = 0.45f),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            caption,
                            color = SheetInk.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                KusinaButton(
                label = "Got it",
                onClick = onDismiss,
                tone = KusinaButtonTone.Terracotta
            )
        }
    }
}
