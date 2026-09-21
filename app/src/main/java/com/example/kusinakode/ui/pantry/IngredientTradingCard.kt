package com.example.kusinakode.ui.pantry

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.kusinakode.R
import com.example.kusinakode.domain.pantry.DrawResult
import com.example.kusinakode.domain.pantry.Ingredient
import com.example.kusinakode.domain.pantry.Rarity
import com.example.kusinakode.ui.theme.BeVietnamPro

/**
 * The four collectible frames are one 5:7 shape with a transparent art
 * window, so a card is the photograph laid down first and the frame dropped
 * on top of it. Every measurement below is a fraction of the card, taken off
 * the source art, because each tier cuts its window and bands in a slightly
 * different place.
 */
internal const val CARD_ASPECT = 0.6997f

internal data class CardFace(
    @DrawableRes val frame: Int,
    /** The transparent window the photograph shows through. */
    val windowLeft: Float,
    val windowTop: Float,
    val windowWidth: Float,
    val windowHeight: Float,
    /** Name banner across the top. */
    val titleTop: Float,
    val titleBottom: Float,
    /** Second banner, under the art. */
    val subtitleTop: Float,
    val subtitleBottom: Float,
    /** The body panel at the foot of the card. */
    val bodyTop: Float,
    val bodyBottom: Float,
    val bannerInk: Color,
    val bodyInk: Color,
    val accentInk: Color,
    /** Painted behind the photograph so the window never shows the backdrop. */
    val windowBacking: Color
)

internal fun cardFaceFor(rarity: Rarity): CardFace = when (rarity) {
    Rarity.COMMON -> CardFace(
        frame = R.drawable.card_frame_common,
        windowLeft = 0.2075f, windowTop = 0.1316f, windowWidth = 0.5886f, windowHeight = 0.4116f,
        titleTop = 0.0605f, titleBottom = 0.1087f,
        subtitleTop = 0.5681f, subtitleBottom = 0.6159f,
        bodyTop = 0.6352f, bodyBottom = 0.9151f,
        bannerInk = Color(0xFF2A2F35),
        bodyInk = Color(0xFF33383D),
        accentInk = Color(0xFF5C6672),
        windowBacking = Color(0xFFE6E8EA)
    )
    Rarity.UNCOMMON -> CardFace(
        frame = R.drawable.card_frame_uncommon,
        windowLeft = 0.2383f, windowTop = 0.1259f, windowWidth = 0.5235f, windowHeight = 0.4277f,
        titleTop = 0.0602f, titleBottom = 0.1087f,
        subtitleTop = 0.5677f, subtitleBottom = 0.6158f,
        bodyTop = 0.6350f, bodyBottom = 0.9100f,
        bannerInk = Color(0xFF123D1D),
        bodyInk = Color(0xFF1B4526),
        accentInk = Color(0xFF2E7D4A),
        windowBacking = Color(0xFFDCEBDC)
    )
    Rarity.RARE -> CardFace(
        frame = R.drawable.card_frame_rare,
        windowLeft = 0.2125f, windowTop = 0.1599f, windowWidth = 0.5750f, windowHeight = 0.4252f,
        titleTop = 0.0723f, titleBottom = 0.1191f,
        subtitleTop = 0.5988f, subtitleBottom = 0.6487f,
        bodyTop = 0.6675f, bodyBottom = 0.9145f,
        bannerInk = Color.White,
        bodyInk = Color.White,
        accentInk = Color(0xFFBFD8F2),
        windowBacking = Color(0xFFE7EEF8)
    )
    Rarity.LEGENDARY -> CardFace(
        frame = R.drawable.card_frame_legendary,
        windowLeft = 0.2125f, windowTop = 0.1595f, windowWidth = 0.5750f, windowHeight = 0.4256f,
        titleTop = 0.0731f, titleBottom = 0.1209f,
        subtitleTop = 0.6022f, subtitleBottom = 0.6480f,
        bodyTop = 0.6675f, bodyBottom = 0.9141f,
        bannerInk = Color(0xFFF7E3A6),
        bodyInk = Color(0xFFF0DCC0),
        accentInk = Color(0xFFE8C36A),
        windowBacking = Color(0xFF2A0D33)
    )
}

/** Text sits inside the banner ends; the body panel is a touch wider. */
private const val BANNER_INSET = 0.12f
private const val BODY_INSET = 0.105f
private const val BADGE_SIZE = 0.19f

/**
 * A drawn ingredient as a collectible card. [headline] is the banner line at
 * the top of the body panel, [footer] the small line at its foot.
 */
@Composable
internal fun IngredientTradingCard(
    ingredient: Ingredient,
    headline: String,
    body: String,
    footer: String,
    modifier: Modifier = Modifier
) {
    val face = cardFaceFor(ingredient.rarity)
    // Sized to fit the space on BOTH axes.
    //
    // Callers hand this fillMaxWidth(), and a fixed aspect ratio then makes
    // height purely a function of width — 1.43x it. That is fine on a phone,
    // where there is always more height than width, and wrong on anything
    // wide and short: an unfolded foldable is 841x701dp, so the card was
    // computed about 1200dp tall and ran off the top and bottom of the
    // screen with its title and lore cut in half.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
      val cardWidth = minOf(maxWidth, maxHeight * CARD_ASPECT)
      BoxWithConstraints(Modifier.width(cardWidth).aspectRatio(CARD_ASPECT)) {
        val w = maxWidth
        val h = maxHeight

        // The photograph goes down first — the frame's window is a hole.
        Box(
            Modifier
                .offset(x = w * face.windowLeft, y = h * face.windowTop)
                .size(width = w * face.windowWidth, height = h * face.windowHeight)
                .background(face.windowBacking)
        ) {
            IngredientPhoto(
                ingredient,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Image(
            painter = painterResource(face.frame),
            contentDescription = ingredient.rarity.label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // The medallion the reveal card has always worn. It rides the right
        // edge of the art a fifth of the way down — clear of the name banner
        // above and the local-name banner below, on every window shape.
        val badge = w * BADGE_SIZE
        PixelArt(
            res = ingredient.rarity.badge,
            contentDescription = ingredient.rarity.label,
            modifier = Modifier
                .offset(
                    x = w * (face.windowLeft + face.windowWidth - 0.045f) - badge / 2f,
                    y = h * (face.windowTop + 0.20f * face.windowHeight) - badge / 2f
                )
                .size(badge)
        )

        CardRegion(w, h, face.titleTop, face.titleBottom, BANNER_INSET) {
            BannerText(ingredient.name, face.bannerInk, w, 0.058f, fitsAt = 24)
        }

        CardRegion(w, h, face.subtitleTop, face.subtitleBottom, BANNER_INSET) {
            BannerText(ingredient.localName, face.bannerInk, w, 0.050f, fitsAt = 22)
        }

        CardRegion(w, h, face.bodyTop, face.bodyBottom, BODY_INSET) {
            Text(
                headline,
                color = face.accentInk,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (w.value * 0.030f).sp,
                letterSpacing = (w.value * 0.005f).sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(h * 0.012f))
            Text(
                body,
                color = face.bodyInk,
                fontFamily = BeVietnamPro,
                fontSize = (w.value * 0.040f).sp,
                lineHeight = (w.value * 0.054f).sp,
                textAlign = TextAlign.Center,
                maxLines = 4
            )
            Spacer(Modifier.height(h * 0.012f))
            Text(
                footer,
                color = face.accentInk,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Bold,
                fontSize = (w.value * 0.026f).sp,
                letterSpacing = (w.value * 0.004f).sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
      }
    }
}

/** The frame alone — for the cards stacked behind the one being read. */
@Composable
internal fun IngredientCardBack(rarity: Rarity, modifier: Modifier = Modifier) {
    // Fitted on both axes for the same reason as the face above: these sit
    // in the same stack and must not be the one thing that overflows.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(cardFaceFor(rarity).frame),
            contentDescription = null,
            modifier = Modifier
                .width(minOf(maxWidth, maxHeight * CARD_ASPECT))
                .aspectRatio(CARD_ASPECT),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun CardRegion(
    cardWidth: Dp,
    cardHeight: Dp,
    top: Float,
    bottom: Float,
    inset: Float,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .offset(x = cardWidth * inset, y = cardHeight * top)
            .size(
                width = cardWidth * (1f - 2f * inset),
                height = cardHeight * (bottom - top)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/**
 * Banner text is pinned to one line inside a fixed bar, and the catalog holds
 * names up to 36 characters, so long ones step down rather than wrap or clip.
 */
@Composable
private fun BannerText(
    text: String,
    ink: Color,
    cardWidth: Dp,
    sizeFraction: Float,
    fitsAt: Int
) {
    val shrink = (fitsAt.toFloat() / text.length.coerceAtLeast(fitsAt)).coerceAtLeast(0.6f)
    Text(
        text,
        color = ink,
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.ExtraBold,
        fontSize = (cardWidth.value * sizeFraction * shrink).sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false
    )
}
