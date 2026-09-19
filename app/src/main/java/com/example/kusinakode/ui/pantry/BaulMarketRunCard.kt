package com.example.kusinakode.ui.pantry

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.ui.theme.BeVietnamPro

private val WoodTop = Color(0xFF4A2A12)
private val WoodMid = Color(0xFF3A1E0C)
private val WoodEdge = Color(0xFF2A1408)
private val Gold = Color(0xFFE8C36A)
private val GoldDeep = Color(0xFFC4922E)
private val Parchment = Color(0xFFF3E2C2)
private val Ink = Color(0xFF3E2723)

/**
 * Centered pantry draw card: one baul and one Draw button. Tapping Draw
 * opens the ritual, where leftover runs can be opened all at once.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun BaulMarketRunCard(
    draws: Int,
    drawing: Boolean,
    drawingAll: Boolean,
    /** Wheel spins banked by daily logins. */
    spins: Int = 0,
    onSpin: () -> Unit = {},
    onDraw: () -> Unit,
    onOpenAll: () -> Unit
) {
    val enabled = draws > 0 && !drawing && !drawingAll
    val waiting = draws.coerceAtLeast(0)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(WoodTop, WoodMid, WoodEdge)))
            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        GoldCorners(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Parchment,
                shadowElevation = 2.dp
            ) {
                Text(
                    "PALAYOK MARKET RUN",
                    color = Ink,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            if (waiting > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stored from the dishes you have won",
                    color = Color.White.copy(alpha = 0.78f),
                    fontFamily = BeVietnamPro,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))
            PixelArt(
                res = if (drawing || drawingAll) R.drawable.baul_open else R.drawable.baul_closed,
                contentDescription = "Palayok",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp)
            )
            if (waiting > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (waiting == 1) "1 Palayok Waiting" else "$waiting Palayoks Waiting",
                    color = Parchment,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))
            GoldAction(
                label = if (drawing || drawingAll) "OPENING…" else "OPEN",
                enabled = enabled,
                onClick = onDraw,
                modifier = Modifier.fillMaxWidth(0.78f)
            )
            // The daily login banks a spin; this is where it is cashed in.
            if (spins > 0) {
                Spacer(Modifier.height(10.dp))
                GoldAction(
                    label = if (spins == 1) "SPIN THE WHEEL" else "SPIN THE WHEEL ×$spins",
                    enabled = !drawing && !drawingAll,
                    onClick = onSpin,
                    modifier = Modifier.fillMaxWidth(0.78f)
                )
            }
            // Only the spin still has something to explain; the palayok count
            // above already says what is waiting.
            if (spins > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your daily login earned a spin — it pays 3, 5 or 8 palayoks.",
                    color = Color.White.copy(alpha = 0.62f),
                    fontFamily = BeVietnamPro,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoldAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) Gold else Color(0xFF6B5340),
        shadowElevation = if (enabled) 3.dp else 0.dp,
        modifier = modifier.clickable(enabled = enabled, onClick = clickSfx(onClick))
    ) {
        Text(
            label,
            color = if (enabled) Ink else Color.White.copy(alpha = 0.45f),
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun GoldCorners(modifier: Modifier = Modifier) {
    Box(modifier) {
        val arm = 16.dp
        val thick = 3.dp
        val inset = 8.dp
        listOf(
            Alignment.TopStart,
            Alignment.TopEnd,
            Alignment.BottomStart,
            Alignment.BottomEnd
        ).forEach { corner ->
            Box(
                Modifier
                    .align(corner)
                    .padding(inset)
                    .size(arm)
            ) {
                val top = corner == Alignment.TopStart || corner == Alignment.TopEnd
                val start = corner == Alignment.TopStart || corner == Alignment.BottomStart
                Box(
                    Modifier
                        .align(if (start) Alignment.TopStart else Alignment.TopEnd)
                        .width(thick)
                        .height(arm)
                        .background(Gold)
                )
                Box(
                    Modifier
                        .align(if (top) Alignment.TopStart else Alignment.BottomStart)
                        .height(thick)
                        .fillMaxSize()
                        .then(
                            if (start) Modifier.padding(end = arm / 3)
                            else Modifier.padding(start = arm / 3)
                        )
                        .background(if (top) Gold else GoldDeep)
                )
            }
        }
    }
}
