package com.example.kusinakode.ui.gamification

import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.BottomNavTab
import com.example.kusinakode.KusinaBottomNav
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.domain.model.BadgeVerification
import com.example.kusinakode.domain.model.AttemptRecord
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault
import com.example.kusinakode.ui.theme.SuccessGreen
import com.example.kusinakode.ui.rewards.HeaderCircleButton
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

internal val ProgressBurnt = Color(0xFFCC6B1F)
internal val ProgressCream = Color(0xFFF1E6D2)
internal val ProgressInk = Color(0xFF3E2723)

/**
 * Module 2 element iv — progress tracking: totals, badge shelf and the
 * player's recent attempt history.
 */
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onExplore: () -> Unit,
    onLeaderboard: () -> Unit,
    onLearn: () -> Unit,
    onProfile: () -> Unit,
    onWallet: () -> Unit = {},
    onAllRounds: () -> Unit = {},
    gamification: GamificationViewModel = viewModel(),
    historyViewModel: AttemptHistoryViewModel = viewModel()
) {
    val game by gamification.uiState.collectAsState()
    val history by historyViewModel.uiState.collectAsState()
    val rounds = remember(history.entries) { foldIntoRounds(history.entries) }
    var openBadge by remember { mutableStateOf<BadgeSlot?>(null) }

    openBadge?.let { slot ->
        BadgeDetailDialog(slot = slot, onDismiss = { openBadge = null })
    }

    Scaffold(
        containerColor = ProgressCream,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Profile,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = onExplore,
                onWallet = onWallet,
                onCompleted = onLearn
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = clickSfx { onBack() }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "My Progress",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Points, badges and recent rounds",
                            color = LightOrange.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            // Header spans the screen; the content below is capped so a tablet
            // gets a readable column rather than full-width rows.
            Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {


            Column(Modifier.padding(16.dp)) {
                // ---- Totals ----
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile(Icons.Default.Bolt, "${game.progress.totalPoints}", "POINTS", Modifier.weight(1f))
                    SummaryTile(Icons.Default.RestaurantMenu, "${game.progress.roundsCompleted}", "SOLVED", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile(Icons.Default.LocalFireDepartment, "${game.progress.currentStreak}", "STREAK", Modifier.weight(1f))
                    SummaryTile(Icons.Default.MilitaryTech, "${game.progress.bestStreak}", "BEST RUN", Modifier.weight(1f))
                    SummaryTile(Icons.Default.Verified, "${game.progress.perfectRounds}", "PERFECT", Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // ---- Badge shelf ----
                Text(
                    "BADGES · ${game.earnedCount} of ${game.badges.size}",
                    color = HintGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                // A medal wall, not a list. Sixteen full-width rows was most of
                // the screen's scroll for information a player reads at a
                // glance; four across says the same in a quarter of the height.
                // Tapping one opens its name, criteria and verification.
                game.badges.filter { it.isEarned }.ifEmpty { null }?.let { earned ->
                    earned.chunked(4).forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { slot ->
                                BadgeTile(
                                    slot = slot,
                                    onClick = { openBadge = slot },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Keep the last row's medals the same size as the
                            // rows above instead of stretching them.
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } ?: Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No badges yet — solve a dish to earn your first.",
                        color = HintGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ---- Attempt history ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "RECENT ROUNDS",
                        color = HintGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (rounds.size > RECENT_ROUNDS) {
                        Text(
                            "See all ${rounds.size} ›",
                            color = ProgressBurnt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = clickSfx(onAllRounds))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                when {
                    history.isLoading -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(Modifier.size(28.dp), color = ProgressBurnt) }

                    history.entries.isEmpty() -> Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            history.error ?: "No rounds recorded yet.",
                            color = HintGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    else -> rounds.take(RECENT_ROUNDS).forEach { round ->
                        RoundRow(
                            round = round,
                            revealed = round.levelId in game.progress.solvedLevels,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        
            }
        }
    }
}

@Composable
private fun VerificationChip(confirmed: Boolean) {
    val color = if (confirmed) SuccessGreen else Color(0xFFB98428)
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (confirmed) Icons.Default.Verified else Icons.Default.Schedule,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (confirmed) "verified" else "pending",
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OutlineDefault.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = ProgressBurnt, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, color = ProgressInk, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = HintGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

/**
 * One sitting at one dish: however many guesses it took, and how it ended.
 *
 * The history the server returns is one row per *guess*, which read as a wall
 * of "Missed attempt" — four wrong guesses on the way to a win looked like
 * four failures. A player thinks in rounds, so the guesses are folded back
 * into the round they belong to.
 */
internal data class PlayedRound(
    val levelId: Int,
    val levelName: String,
    val guesses: Int,
    val solved: Boolean,
    val timeTakenMs: Long?,
    val endedAt: Long?
)

/**
 * A round is a consecutive run of guesses on one dish. It closes when the
 * dish changes, or when a guess lands — playing the same dish again after a
 * win is a fresh round, not a continuation.
 */
internal fun foldIntoRounds(attempts: List<AttemptRecord>): List<PlayedRound> {
    val ordered = attempts.sortedBy { it.attemptedAt ?: Long.MIN_VALUE }
    val runs = mutableListOf<MutableList<AttemptRecord>>()
    ordered.forEach { attempt ->
        val open = runs.lastOrNull()
        if (open == null || open.first().levelId != attempt.levelId || open.last().wasCorrect) {
            runs += mutableListOf(attempt)
        } else {
            open += attempt
        }
    }
    return runs
        .map { run ->
            val win = run.lastOrNull { it.wasCorrect }
            PlayedRound(
                levelId = run.first().levelId,
                levelName = run.first().levelName,
                guesses = run.size,
                solved = win != null,
                timeTakenMs = (win ?: run.last()).timeTakenMs,
                endedAt = run.last().attemptedAt
            )
        }
        .sortedByDescending { it.endedAt ?: Long.MIN_VALUE }
}

/** How many rounds the shelf shows before it stops. */
private const val RECENT_ROUNDS = 6

/** One medal on the wall: the art, its name, and a tick if the chain agrees. */
@Composable
private fun BadgeTile(
    slot: BadgeSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmed = slot.earned?.verification == BadgeVerification.CONFIRMED
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = clickSfx(onClick))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            // The medal itself once the badge is held; the generic icon only
            // if a badge has no art yet.
            val medal = BadgeArt.forBadge(slot.badgeId)
            if (medal != null) {
                Image(
                    painter = painterResource(medal),
                    contentDescription = slot.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(ProgressBurnt.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = ProgressBurnt,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (confirmed) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = SuccessGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            slot.title,
            color = ProgressInk,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The detail the medal wall no longer has room to print. */
@Composable
private fun BadgeDetailDialog(slot: BadgeSlot, onDismiss: () -> Unit) {
    val confirmed = slot.earned?.verification == BadgeVerification.CONFIRMED
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val medal = BadgeArt.forBadge(slot.badgeId)
                if (medal != null) {
                    Image(
                        painter = painterResource(medal),
                        contentDescription = slot.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(96.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = ProgressBurnt,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    slot.title,
                    color = ProgressInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    slot.criteria,
                    color = HintGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                VerificationChip(confirmed)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Close",
                    color = ProgressBurnt,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = clickSfx(onDismiss))
                        .padding(horizontal = 26.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/**
 * One round on the shelf. Shared with the full-history screen so a round
 * looks the same wherever it is read.
 *
 * [revealed] gates the dish name, which IS the answer. It is decided by the
 * level, not by this round: a level missed and later beaten can safely show
 * its name, and one never beaten must not — otherwise the history hands over
 * the answer to the very round the player still has to play.
 */
@Composable
internal fun RoundRow(
    round: PlayedRound,
    revealed: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OutlineDefault.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (round.solved) SuccessGreen.copy(alpha = 0.15f)
                        else HintGray.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (round.solved) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (round.solved) SuccessGreen else HintGray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (revealed) round.levelName else "Level ${round.levelId}",
                    fontWeight = FontWeight.Bold,
                    color = ProgressInk,
                    fontSize = 13.sp
                )
                val tries = if (round.guesses == 1) "1 guess" else "${round.guesses} guesses"
                Text(
                    if (round.solved) "Solved in $tries" else "$tries · not solved yet",
                    color = HintGray,
                    fontSize = 11.sp
                )
            }
            round.timeTakenMs?.let { ms ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF6E7C8),
                    border = BorderStroke(1.dp, Color(0xFFE0C48A))
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = PlayNowBrown,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatRoundClock(ms),
                            color = ProgressInk,
                            fontFamily = BeVietnamPro,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/** Stopwatch readout: 6s under a minute, 5:35 once it runs longer. */
internal fun formatRoundClock(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (minutes == 0L) "${seconds}s" else "%d:%02d".format(minutes, seconds)
}
