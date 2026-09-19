package com.example.kusinakode.ui.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.branding.WordmarkChip
import com.example.kusinakode.ui.branding.WordmarkOutline
import com.example.kusinakode.ui.branding.WordmarkTile
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.game.TileCorrectGreen
import com.example.kusinakode.ui.game.TileSemiYellow
import com.example.kusinakode.ui.game.TileWrongBrown
import com.example.kusinakode.ui.theme.LightOrange

/** Height the board must leave so this row never collides with the tiles. */
val ColourLegendHeight = 74.dp

/**
 * A / D / X colour key. Same chips as the KUSINA KODE wordmark, so the
 * legend teaches the poster tiles the player has already seen.
 *
 * Lives under the grid on both the practice round and a scored dish.
 */
@Composable
fun ColourLesson(
    highContrast: Boolean,
    onOpenLesson: (TileLesson) -> Unit,
    modifier: Modifier = Modifier
) {
    val green = if (highContrast) Color(0xFF1D6FB8) else TileCorrectGreen
    val yellow = if (highContrast) Color(0xFFE07B00) else TileSemiYellow
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LessonChip('A', green, "right spot") { onOpenLesson(TileLesson.RightSpot) }
        LessonChip('D', yellow, "wrong spot") { onOpenLesson(TileLesson.WrongSpot) }
        LessonChip('X', TileWrongBrown, "not in it") { onOpenLesson(TileLesson.NotInIt) }
    }
}

@Composable
private fun LessonChip(letter: Char, fill: Color, label: String, onClick: () -> Unit) {
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = clickSfx(onClick))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        WordmarkChip(
            tile = WordmarkTile(letter, fill),
            size = 32.dp,
            outline = WordmarkOutline
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = LightOrange.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
