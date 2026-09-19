package com.example.kusinakode.ui.shop

import com.example.kusinakode.ui.story.PixelSprite
import com.example.kusinakode.ui.story.SpriteChef

/** Pixel chefs for the atelier — Filipino kitchen pride, not a letter in a circle. */

val SpriteLola = PixelSprite(
    listOf(
        "................",
        ".....QQQQQQ.....",
        "....QQQQQQQQ....",
        "...QQNNNNNNQQ...",
        "...QNNEE.EENN...",
        "...QNNNNNNNNQ...",
        "....NNNNNNNN....",
        ".....NNNNNN.....",
        "...IIIIIIIIII...",
        "..IXIIIIIIIXI...",
        "..IIIIIIIIIIII..",
        "..II........II.."
    )
)

val SpriteFarmer = PixelSprite(
    listOf(
        "................",
        "......HHHH......",
        "....HHHHHHHH....",
        "...HHHHHHHHHH...",
        "....TTNNNNTT....",
        "...TNEE..EENT...",
        "...TNNNNNNNNT...",
        "....NNNNNNNN....",
        ".....NNNNNN.....",
        "...AAAAAAAAAA...",
        "..AAAAAAAAAAAA..",
        "..AA........AA.."
    )
)

val SpriteBarongChef = PixelSprite(
    listOf(
        "................",
        "....TTTTTTTT....",
        "...TTTTTTTTTT...",
        "...TTNNNNNNTT...",
        "...NNEE..EENN...",
        "...NNNNNNNNNN...",
        "....NNNNNNNN....",
        ".....NNNNNN.....",
        "...IIIIIIIIII...",
        "..IYYIIIIIIYYI..",
        "..IIIIIIIIIIII..",
        "..II........II.."
    )
)

val SpriteFiestaCook = PixelSprite(
    listOf(
        "................",
        "....X.TTTTTT....",
        "...XTTTTTTTT....",
        "...TTNNNNNNTT...",
        "...NNEE..EENN...",
        "...NNNNNNNNNN...",
        "....NNNNNNNN....",
        ".....NNNNNN.....",
        "...MMOOMMOOMM...",
        "..MMYYMMYYMMMM..",
        "..MMMMMMMMMMMM..",
        "..MM........MM.."
    )
)

fun chefSpriteFor(characterId: String?): PixelSprite = when (characterId) {
    "avc_lola" -> SpriteLola
    "avc_farmer" -> SpriteFarmer
    "avc_barong" -> SpriteBarongChef
    "avc_fiesta" -> SpriteFiestaCook
    else -> SpriteChef
}
