package com.example.kusinakode.ui.tutorial

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.R
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.game.TileCorrectGreen
import com.example.kusinakode.ui.game.TileSemiYellow
import com.example.kusinakode.ui.game.TileWrongBrown
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.LightOrange

private val Gold = Color(0xFFFFD24A)
private val GoldDeep = Color(0xFFD9A227)
private val Plate = Color(0xFF1C0C05)
private val PlateFace = Color(0xFF3A1A0A)
private val Ink = Color(0xFFFFF6E6)

/**
 * What KK is, how it is earned, and what it actually buys.
 *
 * Closes the loop the story and tutorial round opened: a player has now won a
 * dish and seen a badge mint, so this is the moment the currency means
 * something. Amounts are the real ones the chain pays out, pulled from the
 * same constants the game uses so this page cannot drift from the economy.
 */
@Composable
fun KkTutorialScreen(onFinish: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "kkGlow").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kkGlowAlpha"
    )
    val bob by rememberInfiniteTransition(label = "kkBob").animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "kkBobY"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF140804), Color(0xFF3A1608), Color(0xFF6A2E10))
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(118.dp)
                .graphicsLayer { translationY = -bob }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to Gold.copy(alpha = glow * 0.55f),
                            1f to Color.Transparent
                        ),
                        radius = size.minDimension * 0.62f
                    )
                }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_kk_pixel),
                contentDescription = "KK coin",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(84.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        SurfaceKicker("KITCHEN CURRENCY")
        Spacer(Modifier.height(8.dp))
        Text(
            "Earned by cooking. Never bought with real money.",
            color = LightOrange.copy(alpha = 0.85f),
            fontFamily = BeVietnamPro,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(22.dp))
        SectionBanner("QUEST REWARDS", "HOW YOU EARN IT")
        Spacer(Modifier.height(10.dp))
        RewardRow(Icons.Default.Star, "Solve a new dish", "+10", "FIRST CLEAR", TileCorrectGreen)
        RewardRow(Icons.Default.WorkspacePremium, "Earn a badge", "+25", accent = GoldDeep)
        RewardRow(Icons.Default.Public, "Finish an island", "+15", accent = TileCorrectGreen)
        RewardRow(Icons.Default.CalendarMonth, "Daily login", "+5", "DAILY", TileSemiYellow)
        RewardRow(Icons.Default.Storefront, "Sell spare ingredients", "VARIES", accent = GoldDeep)

        Spacer(Modifier.height(22.dp))
        SectionBanner("THE MARKET", "WHERE IT GOES")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StallTile(Icons.Default.Restaurant, "Pantry", "INGREDIENTS", Modifier.weight(1f))
            StallTile(Icons.Default.Movie, "Reel", "FILMS", Modifier.weight(1f))
            StallTile(Icons.Default.Face, "Atelier", "LOOKS", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        SectionBanner("IN-ROUND POWER-UPS", "SPEND MID-COOK")
        Spacer(Modifier.height(10.dp))
        RewardRow(
            Icons.Default.Visibility,
            "Reveal",
            "-${PowerUp.REVEAL_LETTER.coinCost}",
            "LOCKS ONE LETTER",
            TileCorrectGreen,
            debit = true
        )
        RewardRow(
            Icons.Default.Bolt,
            "Bomb",
            "-${PowerUp.BOMB.coinCost}",
            "CLEARS WRONG KEYS",
            TileSemiYellow,
            debit = true
        )
        RewardRow(
            Icons.Default.Star,
            "Instant Solve",
            "-${PowerUp.INSTANT_SOLVE.coinCost}",
            "FINISHES THE DISH",
            TileWrongBrown,
            debit = true
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Only KK is ever spent — never your XP or points.",
            color = LightOrange.copy(alpha = 0.7f),
            fontFamily = BeVietnamPro,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = clickSfx(onFinish),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlayNowBrown,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Enter the kitchen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SurfaceKicker(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Gold.copy(alpha = 0.16f))
            .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = Gold,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun SectionBanner(kicker: String, title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            kicker,
            color = Gold.copy(alpha = 0.75f),
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 2.2.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            title,
            color = Ink,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun RewardRow(
    icon: ImageVector,
    title: String,
    amount: String,
    note: String? = null,
    accent: Color = GoldDeep,
    debit: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Plate)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(5.dp)
                .height(58.dp)
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.22f))
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Ink,
                fontFamily = BeVietnamPro,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (note != null) {
                Text(
                    note,
                    color = accent.copy(alpha = 0.9f),
                    fontFamily = BeVietnamPro,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
        }
        AmountChip(amount, debit)
    }
}

@Composable
private fun AmountChip(amount: String, debit: Boolean) {
    val fill = if (debit) TileWrongBrown else GoldDeep
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill.copy(alpha = 0.22f))
            .border(1.dp, fill.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            amount,
            color = if (debit) Color(0xFFFFC9A8) else Gold,
            fontFamily = BeVietnamPro,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.width(4.dp))
        Image(
            painter = painterResource(R.drawable.ic_kk_pixel),
            contentDescription = "KK",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun StallTile(
    icon: ImageVector,
    name: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Plate)
            .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PlateFace)
                .border(1.5.dp, Gold.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            color = Ink,
            fontFamily = BeVietnamPro,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            tag,
            color = Gold.copy(alpha = 0.75f),
            fontFamily = BeVietnamPro,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
}
