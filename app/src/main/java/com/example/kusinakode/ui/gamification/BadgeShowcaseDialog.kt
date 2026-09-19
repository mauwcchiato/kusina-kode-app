package com.example.kusinakode.ui.gamification

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.Session
import com.example.kusinakode.SoundFx
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HintGray

private val TicketGold = Color(0xFFD9A227)
private val TicketInk = Color(0xFF3E2723)
private val TicketFill = Color(0xFFF6E7C8)
private val TicketEdge = Color(0xFFE0C48A)
private val TicketSeam = Color(0xFFC9B08A)
private val TicketWell = Color(0xFFFFF8EC)

/**
 * Lets the player pick which badges their profile shows.
 *
 * Only earned badges can be chosen - the shelf is a trophy case, not a wish
 * list. Selection order is display order, so the picked tiles carry their
 * position number rather than a generic tick, and a live preview at the top
 * shows the shelf exactly as it will look once saved.
 *
 * Locked badges are still listed, with what earns them, because a trophy case
 * you cannot fill yet should still tell you what to aim at.
 */
@Composable
fun BadgeShowcaseDialog(
    badges: List<BadgeSlotView>,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val stored by FeaturedBadges.featured.collectAsState()
    val earned = remember(badges) { badges.filter { it.isEarned } }
    val locked = remember(badges) { badges.filter { !it.isEarned } }

    // Set when a tap was refused because the shelf is already full.
    var full by remember { mutableStateOf(false) }

    // Seed from the stored choice, falling back to whatever the shelf is
    // showing now, so opening the picker never looks like a reset.
    var picked by remember(stored, earned) {
        mutableStateOf(
            stored.filter { id -> earned.any { it.badgeId == id } }
                .ifEmpty { earned.take(FeaturedBadges.SLOTS).map { it.badgeId } }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ParchmentCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
        ) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    "TROPHY CASE",
                    color = TicketGold,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Choose your badges",
                    color = TicketInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (earned.isEmpty()) {
                        "Solve a dish to earn your first badge."
                    } else {
                        "Tap to pick up to ${FeaturedBadges.SLOTS}. " +
                            "The number shows where each one lands."
                    },
                    color = TicketInk.copy(alpha = 0.62f),
                    fontFamily = BeVietnamPro,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .stitchedWell()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text(
                        "ON YOUR PROFILE",
                        color = TicketGold,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(FeaturedBadges.SLOTS) { i ->
                            val id = picked.getOrNull(i)
                            val slot = id?.let { s -> badges.firstOrNull { it.badgeId == s } }
                            PreviewSlot(slot, Modifier.weight(1f))
                        }
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (earned.isNotEmpty()) {
                        SectionLabel("EARNED · ${earned.size}")
                        BadgeGrid(earned, picked) { slot ->
                            SoundFx.tap(ctx)
                            when {
                                slot.badgeId in picked -> {
                                    picked = picked - slot.badgeId
                                    full = false
                                }
                                picked.size < FeaturedBadges.SLOTS -> {
                                    picked = picked + slot.badgeId
                                    full = false
                                }
                                else -> full = true
                            }
                        }
                    }
                    if (locked.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        SectionLabel("STILL TO EARN · ${locked.size}")
                        BadgeGrid(locked, picked) { }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    if (full) {
                        "Shelf is full · tap one to remove it"
                    } else {
                        "${picked.size} of ${FeaturedBadges.SLOTS} chosen"
                    },
                    color = if (full) Color(0xFFB00020) else TicketInk.copy(alpha = 0.55f),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KusinaButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        tone = KusinaButtonTone.Parchment,
                        height = 50.dp,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    KusinaButton(
                        label = "Save",
                        onClick = {
                            FeaturedBadges.set(ctx, Session.userId, picked)
                            SoundFx.play(ctx, SoundFx.Cue.Coin)
                            onDismiss()
                        },
                        tone = KusinaButtonTone.Terracotta,
                        height = 50.dp,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1.15f)
                    )
                }
            }
        }
    }
}

private fun Modifier.stitchedWell(): Modifier = drawBehind {
    val r = 16.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(TicketWell, TicketFill)),
        cornerRadius = CornerRadius(r)
    )
    drawRoundRect(
        color = TicketEdge,
        cornerRadius = CornerRadius(r),
        style = Stroke(width = 1.6.dp.toPx())
    )
    val inset = 7.dp.toPx()
    drawRoundRect(
        color = TicketSeam,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(r - inset),
        style = Stroke(
            width = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(7.dp.toPx(), 4.dp.toPx())
            )
        )
    )
}

/** One of the four shelf positions, as it will appear on the profile. */
@Composable
private fun PreviewSlot(slot: BadgeSlotView?, modifier: Modifier = Modifier) {
    val medal = slot?.let { BadgeArt.forBadge(it.badgeId) }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (slot == null) TicketFill.copy(alpha = 0.65f) else Color.White)
            .drawBehind {
                drawRoundRect(
                    color = TicketEdge,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (medal != null) {
            Image(
                painter = painterResource(medal),
                contentDescription = slot.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(5.dp)
            )
        } else {
            Text(
                "+",
                color = TicketInk.copy(alpha = 0.35f),
                fontFamily = BeVietnamPro,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TicketInk.copy(alpha = 0.55f),
        fontFamily = BeVietnamPro,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

@Composable
private fun BadgeGrid(
    slots: List<BadgeSlotView>,
    picked: List<String>,
    onTap: (BadgeSlotView) -> Unit
) {
    slots.chunked(3).forEach { row ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            row.forEach { slot ->
                val order = picked.indexOf(slot.badgeId)
                PickerTile(
                    slot = slot,
                    order = if (order >= 0) order + 1 else null,
                    modifier = Modifier.weight(1f)
                ) { onTap(slot) }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PickerTile(
    slot: BadgeSlotView,
    order: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val medal = BadgeArt.forBadge(slot.badgeId)
    val selected = order != null

    val scale by animateFloatAsState(if (selected) 1.04f else 1f, spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    ), label = "tile_scale")
    val border by animateColorAsState(
        if (selected) PlayNowBrown else TicketEdge,
        label = "tile_border"
    )

    Column(
        modifier.clickable(enabled = slot.isEarned, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (slot.isEarned) Color.White else Color.White.copy(alpha = 0.5f),
                border = BorderStroke(if (selected) 2.5.dp else 1.dp, border),
                shadowElevation = if (selected) 3.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Box(
                    Modifier.aspectRatio(1f).padding(9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (medal != null && slot.isEarned) {
                        Image(
                            painter = painterResource(medal),
                            contentDescription = slot.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (medal != null) {
                        Image(
                            painter = painterResource(medal),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.18f)
                        )
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = HintGray.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = HintGray.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (order != null) {
                Box(
                    Modifier
                        .size(24.dp)
                        .offset(x = 6.dp, y = (-6).dp)
                        .clip(CircleShape)
                        .background(PlayNowBrown),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$order",
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            slot.title,
            color = if (slot.isEarned) TicketInk else HintGray.copy(alpha = 0.75f),
            fontFamily = BeVietnamPro,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (!slot.isEarned) {
            Text(
                slot.criteria,
                color = HintGray.copy(alpha = 0.7f),
                fontFamily = BeVietnamPro,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** The bits of a badge slot the picker needs, so it doesn't depend on the ViewModel. */
data class BadgeSlotView(
    val badgeId: String,
    val title: String,
    val criteria: String,
    val isEarned: Boolean
)
