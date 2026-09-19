package com.example.kusinakode.ui.shop

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.domain.shop.ShopItem

/**
 * A jar of something, sitting on a shelf.
 *
 * The pantry used to be a list of rows, which read like a shop receipt rather
 * than a kitchen. Drawing the goods as jars on real planks makes the KODEX
 * feel like a place you keep things — and a locked jar you can still *see* is
 * a much better argument for earning KK than a greyed-out line of text.
 */

/** Contents colour per ingredient, so each jar reads as itself at a glance. */
internal fun pantryFill(itemId: String): Pair<Color, Color> = when (itemId) {
    // fill, cap
    "pan_calamansi" -> Color(0xFF7CB342) to Color(0xFF33691E)
    "pan_bagoong"   -> Color(0xFFC1553B) to Color(0xFF7B2E1B)
    "pan_suka"      -> Color(0xFFF4EBD0) to Color(0xFF9E8B62)
    "pan_pandan"    -> Color(0xFF2E7D4F) to Color(0xFF14532D)
    "pan_ube"       -> Color(0xFF7B3FA0) to Color(0xFF4A1D63)
    "pan_achuete"   -> Color(0xFFE2711D) to Color(0xFF8C3F0A)
    else            -> Color(0xFFB98A4B) to Color(0xFF6F3913)
}

/**
 * One shelf: the goods standing on it, then the plank they stand on.
 *
 * The plank is drawn after the jars so its front edge overlaps their bases —
 * that overlap is the whole trick that makes them read as standing on it
 * rather than floating above it.
 */
@Composable
fun PantryShelf(
    items: List<ShopItem>,
    owned: Set<String>,
    busyId: String?,
    onItemClick: (ShopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                PantryJar(
                    item = item,
                    unlocked = item.id in owned,
                    busy = busyId == item.id,
                    onClick = { onItemClick(item) }
                )
            }
        }
        ShelfPlank()
    }
}

@Composable
fun PantryHeroJar(itemId: String, unlocked: Boolean = true) {
    val (fill, cap) = pantryFill(itemId)
    val item = com.example.kusinakode.domain.shop.KusinaShop.item(itemId)
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 86.dp, height = 108.dp).alpha(if (unlocked) 1f else 0.55f)) {
            drawJar(fill, cap)
        }
        Text(
            item?.emoji.orEmpty(),
            fontSize = 22.sp,
            modifier = Modifier.offset(y = 18.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJar(fill: Color, cap: Color) {
    val w = size.width
    val h = size.height
    val capH = h * 0.16f
    val bodyTop = capH * 0.85f
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(fill.copy(alpha = 0.92f), fill, fill.copy(alpha = 0.72f))
        ),
        topLeft = Offset(w * 0.08f, bodyTop),
        size = Size(w * 0.84f, h - bodyTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.28f),
        topLeft = Offset(w * 0.16f, bodyTop + h * 0.08f),
        size = Size(w * 0.10f, h * 0.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
    )
    drawRoundRect(
        color = cap,
        topLeft = Offset(w * 0.16f, 0f),
        size = Size(w * 0.68f, capH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
    )
    drawRoundRect(
        color = Color(0xFFFDF6E7).copy(alpha = 0.93f),
        topLeft = Offset(w * 0.12f, h * 0.50f),
        size = Size(w * 0.76f, h * 0.30f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f)
    )
}

@Composable
private fun PantryJar(
    item: ShopItem,
    unlocked: Boolean,
    busy: Boolean,
    onClick: () -> Unit
) {
    val (fill, cap) = pantryFill(item.id)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(enabled = !busy) { onClick() }
            .padding(bottom = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(width = 60.dp, height = 76.dp)
                    .alpha(if (unlocked) 1f else 0.55f)
            ) {
                val w = size.width
                val h = size.height
                val capH = h * 0.16f
                val bodyTop = capH * 0.85f

                // Glass body
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            fill.copy(alpha = 0.92f),
                            fill,
                            fill.copy(alpha = 0.72f)
                        )
                    ),
                    topLeft = Offset(w * 0.08f, bodyTop),
                    size = Size(w * 0.84f, h - bodyTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.16f)
                )
                // Highlight down the left of the glass
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f),
                    topLeft = Offset(w * 0.16f, bodyTop + h * 0.08f),
                    size = Size(w * 0.10f, h * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
                )
                // Lid
                drawRoundRect(
                    color = cap,
                    topLeft = Offset(w * 0.16f, 0f),
                    size = Size(w * 0.68f, capH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f)
                )
                // Paper label
                drawRoundRect(
                    color = Color(0xFFFDF6E7).copy(alpha = 0.93f),
                    topLeft = Offset(w * 0.12f, h * 0.50f),
                    size = Size(w * 0.76f, h * 0.30f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f)
                )
            }

            // The emoji rides on the paper label.
            Text(
                item.emoji,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 14.dp)
                    .alpha(if (unlocked) 1f else 0.6f)
            )

            if (!unlocked) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF4A2409)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFF1E2C6),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            item.title,
            color = Color(0xFF3E2723),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                busy -> Color(0xFF9E8B62)
                unlocked -> Color(0xFF3D6B3A)
                else -> ShopBurnt
            }
        ) {
            Text(
                when {
                    busy -> "…"
                    unlocked -> "READ"
                    else -> "${item.coinCost} KK"
                },
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

/** The wooden board the goods stand on, with a lip and a shadow under it. */
@Composable
private fun ShelfPlank() {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFB07D4E),
                            Color(0xFF8A5A2F),
                            Color(0xFF6E441F)
                        )
                    )
                )
        )
        // Front lip catches the light differently from the top face.
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(Color(0xFF57351A))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x33000000), Color(0x00000000))
                    )
                )
        )
    }
}
