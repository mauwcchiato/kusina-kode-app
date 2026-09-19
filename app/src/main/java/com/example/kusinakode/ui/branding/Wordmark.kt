package com.example.kusinakode.ui.branding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.game.TileCorrectGreen
import com.example.kusinakode.ui.game.TileSemiYellow
import com.example.kusinakode.ui.game.TileWrongBrown
import com.example.kusinakode.ui.theme.BeVietnamPro

/**
 * The "KUSINA / KODE" letter tiles, in one place.
 *
 * The auth poster and the splash both draw these, and they were drifting
 * apart as private copies. A tile is either a solid puzzle colour or "glass" -
 * transparent with a hairline border, the way an unfilled square reads on the
 * game board.
 *
 * The three solids are the board's own verdict colours, so the wordmark
 * teaches the legend before a player has seen it: green right, yellow
 * misplaced, brown absent. Transparent stays "typed but not submitted".
 */
val TileGreen = TileCorrectGreen
val TileYellow = TileSemiYellow
val TileTerracotta = TileWrongBrown

/** Cream hairline on an unfilled wordmark tile — the outline the poster reads. */
val WordmarkOutline = Color.White.copy(alpha = 0.45f)
/** Quiet inset on a solid wordmark tile sitting on wood or parchment. */
val WordmarkSolidOutline = Color.Black.copy(alpha = 0.18f)

data class WordmarkTile(val letter: Char, val bg: Color)

val KusinaTiles = listOf(
    WordmarkTile('K', TileGreen),
    WordmarkTile('U', Color.Transparent),
    WordmarkTile('S', TileYellow),
    WordmarkTile('I', Color.Transparent),
    WordmarkTile('N', TileTerracotta),
    WordmarkTile('A', Color.Transparent)
)

val KodeTiles = listOf(
    WordmarkTile('K', TileGreen),
    WordmarkTile('O', TileYellow),
    WordmarkTile('D', TileTerracotta),
    WordmarkTile('E', Color.Transparent)
)

/** Every tile in reading order, which is also the order the splash drops them. */
val AllWordmarkTiles: List<WordmarkTile> = KusinaTiles + KodeTiles

/**
 * One letter tile.
 *
 * [modifier] is applied to the tile box, so a caller can hang a graphicsLayer
 * on it and animate scale or alpha without this needing to know about it.
 */
@Composable
fun WordmarkChip(
    tile: WordmarkTile,
    size: Dp,
    modifier: Modifier = Modifier,
    /**
     * Override the poster stroke. Solid chips on a dark HUD need the cream
     * hairline the empty KUSINA tiles use, or the outline disappears.
     */
    outline: Color? = null
) {
    val glass = tile.bg == Color.Transparent
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(tile.bg)
            .border(
                1.dp,
                outline ?: if (glass) WordmarkOutline else WordmarkSolidOutline,
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            tile.letter.toString(),
            color = Color.White,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.48f).sp
        )
    }
}
