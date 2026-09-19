package com.example.kusinakode.ui.story

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * A sprite drawn as characters, one per pixel.
 *
 * Deliberately hand-authored rather than bitmap assets: the story scenes need
 * a chunky Filipino-folk-art feel that survives any screen density, and a
 * character grid stays readable and editable in review. '.' is transparent.
 */
data class PixelSprite(val rows: List<String>)

/** Shared palette so every scene keeps the same limited, poster-like range. */
private val Palette: Map<Char, Color> = mapOf(
    'P' to Color(0xFF8C4A24), // clay
    'R' to Color(0xFF5E2E12), // rim / dark wood
    'F' to Color(0xFFE8752A), // flame
    'O' to Color(0xFFB33A12), // embers
    '~' to Color(0xFFDCC9A8), // steam
    'B' to Color(0xFF5E2E12), // book cover
    'W' to Color(0xFFF4E7CE), // page
    'L' to Color(0xFFB9A282), // faded writing
    'D' to Color(0xFF3E2723), // device frame
    'G' to Color(0xFF3F8F4A), // correct tile / gold rim
    'Y' to Color(0xFFD9A227), // semi tile / medal
    'K' to Color(0xFF6B5B4F), // wrong tile
    'C' to Color(0xFFB08A3E), // chain link
    'S' to Color(0xFF2E6F8E), // sky / accent
    'N' to Color(0xFFE0A878), // skin
    'E' to Color(0xFF3E2723), // eyes / linework
    'H' to Color(0xFFC4A35A), // salakot straw
    'Q' to Color(0xFFC5BDB0), // gray hair
    'T' to Color(0xFF1A120C), // black hair
    'M' to Color(0xFF9B2C2C), // malong
    'A' to Color(0xFF3D6B3A), // farmer green
    'I' to Color(0xFFF7F1E6), // barong / whites
    'U' to Color(0xFF4A2409), // dark cloth
    'X' to Color(0xFFE91E63)  // sampaguita / fiesta flower
)

/**
 * Renders [sprite] scaled to the available width.
 *
 * Pixels are drawn a hair larger than their cell so no hairline seams show
 * between them on fractional densities. aspectRatio reserves the right height
 * from the sprite's own proportions, so callers just give it a width.
 */
@Composable
fun PixelArtImage(
    sprite: PixelSprite,
    modifier: Modifier = Modifier
) {
    val cols = sprite.rows.maxOfOrNull { it.length } ?: return
    val rowCount = sprite.rows.size
    if (cols == 0 || rowCount == 0) return

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rowCount.toFloat())
    ) {
        val px = size.width / cols
        val bleed = 0.5f
        sprite.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val color = Palette[ch] ?: return@forEachIndexed
                drawRect(
                    color = color,
                    topLeft = Offset(x * px, y * px),
                    size = Size(px + bleed, px + bleed)
                )
            }
        }
    }
}

/** The palayok over a cooking fire: knowledge kept alive in the kitchen. */
val SpritePot = PixelSprite(
    listOf(
        "................",
        "......~..~......",
        ".....~..~.......",
        "......~..~......",
        "..RRRRRRRRRRRR..",
        "..PPPPPPPPPPPP..",
        "..PPPPPPPPPPPP..",
        "..PPPPPPPPPPPP..",
        "...PPPPPPPPPP...",
        "...PPPPPPPPPP...",
        "....PPPPPPPP....",
        ".....PPPPPP.....",
        "................",
        "...F..FF..F.....",
        "..FFFFFFFFFF....",
        "..OOOOOOOOOO...."
    )
)

/** The handed-down recipe book, its writing fading out. */
val SpriteBook = PixelSprite(
    listOf(
        "................",
        "................",
        "..BBBBBBBBBBBB..",
        "..BWWWWWWWWWWB..",
        "..BW........WB..",
        "..BW.LLLLLL.WB..",
        "..BW........WB..",
        "..BW.LLLLLL.WB..",
        "..BW........WB..",
        "..BW.LLLL...WB..",
        "..BW........WB..",
        "..BW.LL.....WB..",
        "..BW........WB..",
        "..BWWWWWWWWWWB..",
        "..BBBBBBBBBBBB..",
        "................"
    )
)

/** The puzzle board on a phone: the same knowledge, made playable. */
val SpritePhone = PixelSprite(
    listOf(
        "................",
        "...DDDDDDDDDD...",
        "...D........D...",
        "...D.GGGGGG.D...",
        "...D........D...",
        "...D.YYYYYY.D...",
        "...D........D...",
        "...D.KKKKKK.D...",
        "...D........D...",
        "...D.GGGGGG.D...",
        "...D........D...",
        "...DDDDDDDDDD...",
        "................",
        "................",
        "................",
        "................"
    )
)

/** The medal on a chain: a reward that can be proven, not just displayed. */
val SpriteBadge = PixelSprite(
    listOf(
        "................",
        "......GGGG......",
        ".....GYYYYG.....",
        "....GYYYYYYG....",
        "....GYY..YYG....",
        "....GY....YG....",
        "....GYY..YYG....",
        "....GYYYYYYG....",
        ".....GYYYYG.....",
        "......GGGG......",
        ".....C....C.....",
        "....CC....CC....",
        "...CC......CC...",
        "..CC........CC..",
        "................",
        "................"
    )
)

/** Coins earned in the kitchen. */
val SpriteCoin = PixelSprite(
    listOf(
        "................",
        "......YYYY......",
        "....YYYYYYYY....",
        "...YYYYYYYYYY...",
        "..YYYY....YYYY..",
        "..YYY..YY..YYY..",
        "..YYY..YY..YYY..",
        "..YYY..YY..YYY..",
        "..YYY..YY..YYY..",
        "..YYYY....YYYY..",
        "...YYYYYYYYYY...",
        "....YYYYYYYY....",
        "......YYYY......",
        "................",
        "................",
        "................"
    )
)

/**
 * The chef who walks you through the tutorial round, and the default face on
 * every profile before one is bought.
 *
 * Reads as a cook rather than a generic villager: a toque whose crown flares
 * wider than its band, chef whites with a double-breasted button run, and a
 * red neckerchief at the throat. Laid out on the same 16x12 grid as the shop
 * chefs in ChefSprites.kt so the default portrait sits at exactly the same
 * scale as a purchased one.
 *
 * The tan silhouette outline is load-bearing, not decoration: both the toque
 * and the whites are near the cream the portrait is drawn on, so without it
 * the hat and jacket vanish and only a face is left floating.
 */
val SpriteChef = PixelSprite(
    listOf(
        "..LLLLLLLLLLLL..",
        ".LWWWWWWWWWWWWL.",
        ".LWWWWWWWWWWWWL.",
        "...LLLLLLLLLL...",
        "...NNNNNNNNNN...",
        "...NNEENNEENN...",
        "...NNNNNNNNNN...",
        ".....NNNNNN.....",
        "..LIIIIMMIIIIL..",
        "..LIKIIIIIIKIL..",
        "..LIKIIIIIIKIL..",
        "..LL........LL.."
    )
)

/** Mouth open — swapped in while the tutorial chef is speaking. */
val SpriteChefTalk = PixelSprite(
    listOf(
        "..LLLLLLLLLLLL..",
        ".LWWWWWWWWWWWWL.",
        ".LWWWWWWWWWWWWL.",
        "...LLLLLLLLLL...",
        "...NNNNNNNNNN...",
        "...NNEENNEENN...",
        "...NNN....NNN...",
        ".....NNNNNN.....",
        "..LIIIIMMIIIIL..",
        "..LIKIIIIIIKIL..",
        "..LIKIIIIIIKIL..",
        "..LL........LL.."
    )
)

/**
 * Someone eating, in four frames: spoon down, up, at the mouth, chewing.
 *
 * Used where showing a real dish would give a puzzle away — the Home hero
 * before a player has solved anything. Hand-authored rather than a GIF so it
 * costs nothing and matches the story mode's look.
 */
val SpriteKain: List<PixelSprite> = listOf(
    // 1 - spoon resting in the bowl, steam curling up
    PixelSprite(
        listOf(
            "................",
            "......WWWW......",
            ".....WWWWWW.....",
            "......NNNN......",
            ".....NNEENN.....",
            ".....NNNNNN.....",
            "......NNNN......",
            "....PPPPPPPP....",
            "...PPPPPPPPPP...",
            "...PP......PP...",
            "......~..~......",
            ".....~..~.......",
            "....MM..........",
            "...VVVVVVVVVV...",
            "....VVVVVVVV....",
            "..TTTTTTTTTTTT.."
        )
    ),
    // 2 - lifting
    PixelSprite(
        listOf(
            "................",
            "......WWWW......",
            ".....WWWWWW.....",
            "......NNNN......",
            ".....NNEENN.....",
            ".....NNNNNN.....",
            "......NNNN......",
            "....PPPPPPPP....",
            "...PPPPPPPPPP...",
            "...PP..MM..PP...",
            "......~..~......",
            ".......~........",
            "................",
            "...VVVVVVVVVV...",
            "....VVVVVVVV....",
            "..TTTTTTTTTTTT.."
        )
    ),
    // 3 - mouth open, spoon arriving
    PixelSprite(
        listOf(
            "................",
            "......WWWW......",
            ".....WWWWWW.....",
            "......NNNN......",
            ".....NNEENN.....",
            ".....NNNNNN.....",
            ".....NNEENN.....",
            "....PPPMMPPP....",
            "...PPPPPPPPPP...",
            "...PP......PP...",
            ".......~........",
            "................",
            "................",
            "...VVVVVVVVVV...",
            "....VVVVVVVV....",
            "..TTTTTTTTTTTT.."
        )
    ),
    // 4 - happy chew
    PixelSprite(
        listOf(
            "................",
            "......WWWW......",
            ".....WWWWWW.....",
            "......NNNN......",
            ".....NNEENN.....",
            ".....NNNNNN.....",
            "......NEEN......",
            "....PPPPPPPP....",
            "...PPPPPPPPPP...",
            "...PP......PP...",
            "................",
            "................",
            "................",
            "...VVVVVVVVVV...",
            "....VVVVVVVV....",
            "..TTTTTTTTTTTT.."
        )
    )
)
