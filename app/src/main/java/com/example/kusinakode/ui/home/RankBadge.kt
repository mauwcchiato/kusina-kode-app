package com.example.kusinakode.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.gamification.ChefRank
import com.example.kusinakode.ui.components.ParchmentCard

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
private val LadderHere = Color(0xFFE9C98F)

/** Keeps the art but drains its colour, for ranks not reached yet. */
private val Locked = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Every chef rank, lowest first, with where each starts. The player's own is
 * highlighted; ranks above it are greyed; a line under says how far the next
 * one is. [solved] and [total] count only dishes currently shown.
 */
@Composable
fun RankLadderDialog(solved: Int, total: Int, onDismiss: () -> Unit) {
    val current = ChefRank.forSolved(solved, total)
    val ladder = ChefRank.ladder(total)
    Dialog(onDismissRequest = onDismiss) {
        ParchmentCard(contentPadding = 18.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Chef Ranks",
                    color = LadderInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Solve dishes on any island to climb.",
                    color = LadderSub,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                ladder.forEach { rank ->
                    val here = rank.title == current.title
                    val reached = solved >= rank.from
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (here) LadderHere else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(rankBadgeRes(rank.title)),
                            contentDescription = null,
                            colorFilter = if (reached) null else Locked,
                            modifier = Modifier
                                .size(52.dp)
                                .alpha(if (reached) 1f else 0.55f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                rank.title,
                                color = LadderInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when {
                                    rank.title == ChefRank.MASTER -> "Every dish · $total"
                                    rank.from == 0 -> "Where every cook starts"
                                    else -> "${rank.from} dishes solved"
                                },
                                color = LadderSub,
                                fontSize = 12.sp
                            )
                        }
                        if (here) {
                            Text(
                                "YOU",
                                color = LadderInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                val next = current.next
                Text(
                    if (next == null) "You've cooked every dish. Mabuhay, Kusina Master!"
                    else {
                        val left = (next.from - solved).coerceAtLeast(1)
                        "$left more ${if (left == 1) "dish" else "dishes"} to ${next.title}"
                    },
                    color = LadderInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
