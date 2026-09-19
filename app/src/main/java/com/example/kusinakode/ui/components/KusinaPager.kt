package com.example.kusinakode.ui.components

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Burnt = Color(0xFFCC6B1F)
private val Ink = Color(0xFF3E2723)
private val AtelierBrown = Color(0xFF8E4B31)
private val TabStroke = Color(0xFFEAE2CF)

/** Compact prev / page / next strip used on Rewards lists. */
@Composable
fun KusinaPager(
    page: Int,
    pageCount: Int,
    onPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        PagerArrow(
            enabled = page > 0,
            forward = false,
            onClick = { onPage(page - 1) }
        )
        Text(
            "${page + 1} / $pageCount",
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        PagerArrow(
            enabled = page < pageCount - 1,
            forward = true,
            onClick = { onPage(page + 1) }
        )
    }
}

@Composable
private fun PagerArrow(enabled: Boolean, forward: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Burnt else Color(0xFFE8D9C4))
            .clickable(enabled = enabled, onClick = clickSfx(onClick)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = if (forward) "Next page" else "Previous page",
            tint = if (enabled) Color.White else Ink.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun KusinaSegmentTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White,
        border = BorderStroke(1.dp, TabStroke),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { i, label ->
                val on = selected == i
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) AtelierBrown else Color.Transparent)
                        .clickable(onClick = clickSfx { onSelect(i) })
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (on) Color.White else Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
