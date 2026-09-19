package com.example.kusinakode.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.domain.engine.WordleEngine
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.ui.game.GameTile
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.DarkBrown

private val BodyInk = DarkBrown.copy(alpha = 0.88f)
private val Seam = DarkBrown.copy(alpha = 0.22f)

/**
 * The "How to Play" explainer, shared by the InstructionScreen and the
 * in-game help dialog so the tutorial reads the same everywhere.
 * Caller provides scrolling/background; this only lays out content.
 */
@Composable
fun HowToPlayContent(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle("How to Play")
        BodyText(
            "Guess the name of a Filipino dish. You get " +
                "${WordleEngine.MAX_ATTEMPTS} tries."
        )

        SectionDivider()
        SectionTitle("What the Colours Mean")
        BodyText("After every guess the tiles change colour:")
        Spacer(Modifier.height(12.dp))
        TileLegendRow('A', TileState.Correct, "Right letter, right spot")
        Spacer(Modifier.height(10.dp))
        TileLegendRow('D', TileState.SemiCorrect, "In the dish, wrong spot")
        Spacer(Modifier.height(10.dp))
        TileLegendRow('K', TileState.Wrong, "Not in the dish")
        Spacer(Modifier.height(12.dp))
        BodyText(
            "You are only told the region, so open with any real word. " +
                "Then let the colours pick your next one."
        )

        SectionDivider()
        SectionTitle("Stuck? Spend KK")
        BodyText("Tap the cards above the keyboard:")
        Spacer(Modifier.height(12.dp))
        KkTip(
            "Reveal",
            PowerUp.REVEAL_LETTER.coinCost,
            "Locks in one correct letter."
        )
        Spacer(Modifier.height(12.dp))
        KkTip(
            "Bomb",
            PowerUp.BOMB.coinCost,
            "Clears wrong letters off the keyboard."
        )
        Spacer(Modifier.height(12.dp))
        KkTip(
            "Solve",
            PowerUp.INSTANT_SOLVE.coinCost,
            "Finishes the dish."
        )

        SectionDivider()
        SectionTitle("When You Win")
        BodyText(
            "You unlock the dish's story and recipe, earn KK, and your badge " +
                "is saved on the ledger."
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = DarkBrown,
            letterSpacing = 0.2.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BodyText(text: String) {
    Text(
        text,
        style = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = BodyInk,
            lineHeight = 23.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = Seam, thickness = 1.dp)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun KkTip(name: String, cost: Int, whatItDoes: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "$name  ·  $cost KK",
            style = TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = DarkBrown,
                letterSpacing = 0.15.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            whatItDoes,
            style = TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = BodyInk,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
private fun TileLegendRow(letter: Char, state: TileState, explanation: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GameTile(
            letter = letter,
            state = state,
            // Static: this is a legend, not a live board.
            animated = false,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            explanation,
            style = TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = DarkBrown,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
