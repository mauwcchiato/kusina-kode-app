package com.example.kusinakode.ui.game

import androidx.compose.ui.draw.clip
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.components.LevelImage

/**
 * The card you just won, revealed before the round is scored.
 *
 * Solving a dish earns its heritage card, and that used to be a line of text
 * buried in the win screen. Handing the card over first — flipped in, named,
 * claimed with a tap — makes the collectible feel like the prize it is, and
 * only then does the CORRECT! screen tally the points.
 *
 * Every card claims the same way, whatever the round also earned.
 */
@Composable
fun HeritageCardReveal(
    @Suppress("UNUSED_PARAMETER") levelNumber: Int,
    dishName: String,
    cardRes: Int,
    /** Server-hosted art for a panel-added level; null for the packaged ones. */
    cardUrl: String? = null,
    onClaim: () -> Unit
) {
    val flip = remember { Animatable(90f) }
    val rise = remember { Animatable(0.82f) }
    LaunchedEffect(Unit) {
        flip.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        rise.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    // One BoxWithConstraints for the whole overlay: the card is sized from the
    // room left after the label and the button, and the Column wraps its
    // content, so there is no gap between the card and Claim Card. Whatever is
    // left over sits as even margin above and below the group.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val ratio = 522f / 924f                   // the packaged cards' shape
        val chrome = 104.dp                       // label + both spacers + button
        val room = (maxHeight - chrome).coerceAtLeast(0.dp)
        val cardWidth = minOf(maxWidth, room * ratio)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "HERITAGE CARD UNLOCKED",
                color = Color(0xFFD9A227),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))

            LevelImage(
                url = cardUrl,
                fallback = cardRes,
                contentDescription = "$dishName heritage card",
                // Crop, not Fit: every card occupies the same 522x924 frame, and
                // an upload that is a different shape should be trimmed to match
                // rather than shrink to a stamp inside it.
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(cardWidth)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(14.dp))
                    .graphicsLayer {
                        rotationY = flip.value
                        scaleX = rise.value
                        scaleY = rise.value
                        cameraDistance = 16f * density
                    }
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = clickSfx(onClaim),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlayNowBrown,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(horizontal = 40.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Claim Card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
