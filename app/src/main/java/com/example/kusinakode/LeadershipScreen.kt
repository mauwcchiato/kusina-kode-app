package com.example.kusinakode

import com.example.kusinakode.ui.components.readableWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.domain.model.LeaderboardRow
import com.example.kusinakode.domain.repository.LeaderboardWindow
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.rewards.HeaderCircleButton
import com.example.kusinakode.ui.shop.AvatarPortrait
import com.example.kusinakode.ui.shop.ChefLook
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.RegionChipInk
import com.example.kusinakode.ui.theme.RegionChipOn

private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
private val CardCream = Color(0xFFFFFBF3)
private val CardEdge = Color(0xFFE0C48A)
private val WellFill = Color(0xFFFFF8EC)
private val WellRing = Color(0xFF6A3B18)

/** Matches the cream the brown pill already uses for its own label, so the
 *  padlock and SOON sit at the same weight as the words beside them. */
private val InkOnBrown = Color(0xFFFFF6E6)
private val Gold = Color(0xFFE8B430)
private val Silver = Color(0xFFD2CBC0)
private val Bronze = Color(0xFFC47A3A)

/** Kusina Masters leaderboard with a podium for the top three (Frame 14). */
@Composable
fun LeadershipScreen(
    entries: List<LeaderboardRow>,
    /** Which period is showing, so the board can label its numbers. */
    window: LeaderboardWindow = LeaderboardWindow.AllTime,
    onSelectWindow: (LeaderboardWindow) -> Unit = {},
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLevels: () -> Unit,
    onCompleted: () -> Unit,
    onWallet: () -> Unit = {}
) {
    val myName = Session.displayName ?: ""
    // A short window ranks on dishes cooked in it; only the all-time board is
    // measured in points.
    val byPoints = window == LeaderboardWindow.AllTime
    fun score(row: LeaderboardRow) =
        if (byPoints) "${row.points} points"
        else "${row.correctCount} " + if (row.correctCount == 1) "dish" else "dishes"

    fun isMe(row: LeaderboardRow) = row.name.equals(myName, ignoreCase = true)

    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Profile,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = onLevels,
                onWallet = onWallet,
                onCompleted = onCompleted
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCircleButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = clickSfx { onBack() }
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Kusina Masters",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.size(36.dp))
                    }

                    PeriodStrip(window = window, onSelect = onSelectWindow)

                    Spacer(Modifier.height(20.dp))

                    // Podium: 2nd — 1st — 3rd. Weighted so three slots always
                    // share the width, however narrow the phone.
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PodiumSlot(
                            scoreLabel = entries.getOrNull(1)?.let(::score).orEmpty(),
                            row = entries.getOrNull(1),
                            rank = 2,
                            avatarSize = 56.dp,
                            blockHeight = 58.dp,
                            isMe = entries.getOrNull(1)?.let(::isMe) == true,
                            modifier = Modifier.weight(1f)
                        )
                        PodiumSlot(
                            scoreLabel = entries.getOrNull(0)?.let(::score).orEmpty(),
                            row = entries.getOrNull(0),
                            rank = 1,
                            avatarSize = 76.dp,
                            blockHeight = 86.dp,
                            isMe = entries.getOrNull(0)?.let(::isMe) == true,
                            modifier = Modifier.weight(1f)
                        )
                        PodiumSlot(
                            scoreLabel = entries.getOrNull(2)?.let(::score).orEmpty(),
                            row = entries.getOrNull(2),
                            rank = 3,
                            avatarSize = 56.dp,
                            blockHeight = 46.dp,
                            isMe = entries.getOrNull(2)?.let(::isMe) == true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            // Header spans the screen; the content below is capped so a tablet
            // gets a readable column rather than full-width rows.
            Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {


            Column(
                Modifier
                    .offset(y = (-16).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(CreamBg)
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                if (entries.isEmpty() && isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(28.dp),
                            color = TextDark,
                            strokeWidth = 2.dp
                        )
                    }
                } else if (entries.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CardCream,
                        border = BorderStroke(1.dp, CardEdge),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when (window) {
                                LeaderboardWindow.Today ->
                                    "No dishes cooked yet today. Solve one and you'll top the board."
                                LeaderboardWindow.Week ->
                                    "No dishes cooked yet this week. Solve one and you'll top the board."
                                LeaderboardWindow.AllTime ->
                                    "No cooks on the board yet."
                            },
                            color = HintGray,
                            fontFamily = BeVietnamPro,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                entries.drop(3).forEachIndexed { i, row ->
                    RankRow(
                        rank = i + 4,
                        row = row,
                        scoreLabel = score(row),
                        isMe = isMe(row)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))
                // Not built yet, and now it says so by being switched off rather
                // than by a toast: enabled = false takes the ripple with it, so
                // the button does not answer a press it cannot honour.
                //
                // The disabled face is the same brown drained of most of its
                // strength, not grey. Grey would be the only cool colour on a
                // warm screen and would read as a rendering fault; a faded brown
                // reads as "off". The padlock and SOON carry the reason.
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayNowBrown,
                        contentColor = InkOnBrown,
                        disabledContainerColor = PlayNowBrown.copy(alpha = 0.38f),
                        disabledContentColor = InkOnBrown.copy(alpha = 0.62f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        // No explicit tint: an explicit one survives the disabled
                        // state and leaves a bright padlock on a faded pill.
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Challenge a Friend",
                        fontFamily = BeVietnamPro,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SOON",
                        fontFamily = BeVietnamPro,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        
            }
        }
    }
}

@Composable
private fun PeriodStrip(
    window: LeaderboardWindow,
    onSelect: (LeaderboardWindow) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0x33FFF8EC),
        border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PeriodTab("Today", selected = window == LeaderboardWindow.Today, modifier = Modifier.weight(1f)) {
                onSelect(LeaderboardWindow.Today)
            }
            PeriodTab("Weekly", selected = window == LeaderboardWindow.Week, modifier = Modifier.weight(1f)) {
                onSelect(LeaderboardWindow.Week)
            }
            PeriodTab("All Time", selected = window == LeaderboardWindow.AllTime, modifier = Modifier.weight(1f)) {
                onSelect(LeaderboardWindow.AllTime)
            }
        }
    }
}

@Composable
private fun PeriodTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) RegionChipOn else Color.Transparent,
        modifier = modifier.clickable(onClick = clickSfx(onClick))
    ) {
        Text(
            label,
            color = if (selected) Color.White else LightOrange.copy(alpha = 0.9f),
            fontFamily = BeVietnamPro,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun PodiumSlot(
    /** Pre-formatted so the podium uses the same units as the list. */
    scoreLabel: String,
    row: LeaderboardRow?,
    rank: Int,
    avatarSize: Dp,
    blockHeight: Dp,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val medal = medalColor(rank)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            Modifier
                .size(avatarSize + 8.dp)
                .border(3.dp, medal, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            BoardPortrait(row = row, isMe = isMe, size = avatarSize)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            (row?.name ?: "—") + if (isMe) " (You)" else "",
            color = Color.White,
            fontFamily = BeVietnamPro,
            fontSize = if (rank == 1) 12.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (row != null) scoreLabel else "",
            color = medal.copy(alpha = 0.95f),
            fontFamily = BeVietnamPro,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(blockHeight)
                .drawBehind { drawPodiumBlock(rank) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$rank",
                color = Color.White,
                fontFamily = BeVietnamPro,
                fontSize = if (rank == 1) 28.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun RankRow(
    rank: Int,
    row: LeaderboardRow,
    scoreLabel: String,
    isMe: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardCream,
        border = BorderStroke(1.dp, if (isMe) RegionChipOn else CardEdge),
        shadowElevation = if (isMe) 3.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(WellFill)
                    .border(1.5.dp, if (isMe) RegionChipOn else WellRing, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$rank",
                    color = TextDark,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            AvatarCircle(
                name = row.name,
                size = 40.dp,
                isMe = isMe,
                frameId = row.frameId,
                characterId = row.characterId
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.name,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isMe) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RegionChipOn,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            "YOU",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                scoreLabel,
                color = RegionChipInk,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Every cook's look, not just yours — Ranks reads the equipped chef/frame
 * the server stored when they tapped Wear.
 */
@Composable
private fun BoardPortrait(
    row: LeaderboardRow?,
    isMe: Boolean,
    size: Dp
) {
    val ctx = LocalContext.current
    val tick by ChefLook.revision.collectAsState()
    androidx.compose.runtime.key(tick) {
        AvatarPortrait(
            initial = row?.name?.trim()?.take(1)?.uppercase() ?: "—",
            frameId = row?.frameId ?: if (isMe) ChefLook.equippedFrame(ctx)?.id else null,
            characterId = row?.characterId ?: if (isMe) ChefLook.equippedCharacter(ctx)?.id else null,
            size = size
        )
    }
}

@Composable
private fun AvatarCircle(
    name: String,
    size: Dp,
    isMe: Boolean = false,
    frameId: String? = null,
    characterId: String? = null
) {
    val ctx = LocalContext.current
    val tick by ChefLook.revision.collectAsState()
    androidx.compose.runtime.key(tick) {
        AvatarPortrait(
            initial = name.trim().take(1).uppercase(),
            frameId = frameId ?: if (isMe) ChefLook.equippedFrame(ctx)?.id else null,
            characterId = characterId ?: if (isMe) ChefLook.equippedCharacter(ctx)?.id else null,
            size = size
        )
    }
}

private fun medalColor(rank: Int): Color = when (rank) {
    1 -> Gold
    2 -> Silver
    else -> Bronze
}

private fun DrawScope.drawPodiumBlock(rank: Int) {
    val radius = 14.dp.toPx()
    val medal = when (rank) {
        1 -> Color(0xFFFFE3A3)
        2 -> Color(0xFFF4EEE6)
        else -> Color(0xFFFFD090)
    }
    val edge = when (rank) {
        1 -> Color(0xFFE2A44A)
        2 -> Color(0xFFD8D0C4)
        else -> Color(0xFFD08948)
    }
    val bodyH = size.height
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset.Zero,
        size = Size(size.width, bodyH),
        cornerRadius = CornerRadius(radius)
    )
    drawRoundRect(
        color = medal.copy(alpha = 0.16f),
        topLeft = Offset.Zero,
        size = Size(size.width, bodyH),
        cornerRadius = CornerRadius(radius)
    )
    drawRoundRect(
        color = edge.copy(alpha = 0.70f),
        topLeft = Offset(1.2.dp.toPx(), 1.2.dp.toPx()),
        size = Size(size.width - 2.4.dp.toPx(), bodyH - 2.4.dp.toPx()),
        cornerRadius = CornerRadius(radius - 1.2.dp.toPx()),
        style = Stroke(width = 1.8.dp.toPx())
    )
}
