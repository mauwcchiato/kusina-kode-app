package com.example.kusinakode.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.gamification.ChefRank
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.ui.theme.BeVietnamPro

/** The badge art for a [ChefRank] title. Each badge carries its name on the ribbon. */
@DrawableRes
fun rankBadgeRes(title: String): Int = when (title) {
    "Line Cook" -> R.drawable.rank_line_cook
    "Sous Chef" -> R.drawable.rank_sous_chef
    "Head Chef" -> R.drawable.rank_head_chef
    ChefRank.MASTER -> R.drawable.rank_kusina_master
    else -> R.drawable.rank_kusinero
}

/**
 * The player's chef rank as its badge, in place of the title as text. With
 * [onClick] it opens the rank ladder ([RankLadderDialog]).
 */
@Composable
fun RankBadge(
    title: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    Image(
        painter = painterResource(rankBadgeRes(title)),
        contentDescription = "Rank: $title",
        modifier = modifier
            .size(size)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable {
                    SoundFx.tap(ctx)
                    onClick()
                }
            )
    )
}

private val LadderInk = Color(0xFF4A2A14)
private val LadderSub = Color(0xFF8A6A4E)
private val LadderGold = Color(0xFFD9A227)
private val LadderTrackOff = Color(0xFFD8C6A8)
/** A warm wash for the player's own row: clearly marked, still parchment. */
private val LadderHereFill = Color(0xFFF3DDB0)
private val LadderYouFill = Color(0xFF6F3913)

/** Keeps the art but drains its colour, for ranks not reached yet. */
private val Locked = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

private val LadderRowHeight = 62.dp
private val LadderBadge = 44.dp
private val LadderRail = 56.dp

/**
 * Every chef rank as one ladder, lowest first. A track runs through the
 * badges - gold up to the player's rank, faint beyond it - and each row ends in
 * the same place: a tick for a rank passed, a YOU tag on the player's own, a
 * lock on the ones still ahead. The player's row also says how far the next
 * rank is. [solved] and [total] count only dishes currently shown.
 */
@Composable
fun RankLadderDialog(solved: Int, total: Int, onDismiss: () -> Unit) {
    val current = ChefRank.forSolved(solved, total)
    val ladder = ChefRank.ladder(total)
    val hereIndex = ladder.indexOfFirst { it.title == current.title }.coerceAtLeast(0)
    Dialog(onDismissRequest = onDismiss) {
        ParchmentCard(contentPadding = 18.dp) {
            Column {
                // The same header as the Trophy Case: gold label, title, one line.
                Text(
                    "CHEF RANKS",
                    color = LadderGold,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Climb the kitchen",
                    color = LadderInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Solve dishes on any island to rise.",
                    color = LadderInk.copy(alpha = 0.62f),
                    fontFamily = BeVietnamPro,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))

                ladder.forEachIndexed { i, rank ->
                    LadderRow(
                        rank = rank,
                        total = total,
                        solved = solved,
                        here = i == hereIndex,
                        reached = i <= hereIndex,
                        trackAbove = if (i == 0) null else i <= hereIndex,
                        trackBelow = if (i == ladder.lastIndex) null else i < hereIndex,
                        next = if (i == hereIndex) current.next else null
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/**
 * One rung. [trackAbove] / [trackBelow] draw the ladder's rail into the rows
 * either side - gold when that stretch has been climbed, null at the ends.
 */
@Composable
private fun LadderRow(
    rank: ChefRank.Rank,
    total: Int,
    solved: Int,
    here: Boolean,
    reached: Boolean,
    trackAbove: Boolean?,
    trackBelow: Boolean?,
    next: ChefRank.Rank?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(LadderRowHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(if (here) LadderHereFill else Color.Transparent)
            .then(
                if (here) Modifier.border(1.5.dp, LadderGold, RoundedCornerShape(14.dp))
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The rail and the badge on it.
        Box(
            Modifier
                .width(LadderRail)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f
                    val w = 3.dp.toPx()
                    trackAbove?.let { climbed ->
                        drawLine(
                            if (climbed) LadderGold else LadderTrackOff,
                            Offset(x, 0f), Offset(x, size.height / 2f), w
                        )
                    }
                    trackBelow?.let { climbed ->
                        drawLine(
                            if (climbed) LadderGold else LadderTrackOff,
                            Offset(x, size.height / 2f), Offset(x, size.height), w
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(rankBadgeRes(rank.title)),
                contentDescription = null,
                colorFilter = if (reached) null else Locked,
                modifier = Modifier
                    .size(if (here) LadderBadge + 4.dp else LadderBadge)
                    .alpha(if (reached) 1f else 0.6f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rank.title,
                color = if (reached) LadderInk else LadderSub,
                fontFamily = BeVietnamPro,
                fontWeight = if (here) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                when {
                    here && next != null -> {
                        val left = (next.from - solved).coerceAtLeast(1)
                        "$left more ${if (left == 1) "Dish" else "Dishes"} to ${next.title}"
                    }
                    here && rank.title == ChefRank.MASTER -> "Every dish cooked. Mabuhay!"
                    rank.title == ChefRank.MASTER -> "Every Dish · $total"
                    rank.from == 0 -> "Where every cook starts"
                    else -> "${rank.from} Dishes Solved"
                },
                color = if (here) LadderYouFill else LadderSub,
                fontFamily = BeVietnamPro,
                fontWeight = if (here) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
        // One marker per row, always in the same place.
        Box(Modifier.width(52.dp).padding(end = 10.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                here -> Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LadderYouFill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "YOU",
                        color = Color(0xFFFFF3DD),
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    )
                }
                reached -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Reached",
                    tint = LadderGold,
                    modifier = Modifier.size(18.dp)
                )
                else -> Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = LadderTrackOff,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
