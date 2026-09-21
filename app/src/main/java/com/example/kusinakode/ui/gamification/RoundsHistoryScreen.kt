package com.example.kusinakode.ui.gamification

import com.example.kusinakode.ui.components.readableWidth

import com.example.kusinakode.ui.components.FilterPillRow
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.rewards.HeaderCircleButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.SuccessGreen

/** Which rounds to keep on screen. */
internal enum class RoundFilter(val label: String) {
    ALL("All"),
    SOLVED("Solved"),
    UNSOLVED("Not solved")
}

/**
 * Every round the player has played. Progress keeps the newest handful and
 * sends the rest here, where the list can run long because the filters and
 * the tallies give it shape.
 */
@Composable
fun RoundsHistoryScreen(
    onBack: () -> Unit,
    gamification: GamificationViewModel = viewModel(),
    historyViewModel: AttemptHistoryViewModel = viewModel()
) {
    val game by gamification.uiState.collectAsState()
    val history by historyViewModel.uiState.collectAsState()
    var filter by rememberSaveable { mutableStateOf(RoundFilter.ALL) }

    val all = remember(history.entries) { foldIntoRounds(history.entries) }
    val rounds = remember(all, filter) {
        when (filter) {
            RoundFilter.ALL -> all
            RoundFilter.SOLVED -> all.filter { it.solved }
            RoundFilter.UNSOLVED -> all.filterNot { it.solved }
        }
    }
    val solved = all.count { it.solved }
    // Guesses per win is the number a player can actually act on — a win in
    // two is a different kind of win from one in seven.
    val averageGuesses = all.filter { it.solved }
        .map { it.guesses }
        .takeIf { it.isNotEmpty() }
        ?.average()

    Scaffold(containerColor = ProgressCream) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
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
                        onClick = clickSfx(onBack)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "All Rounds",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            if (rounds.size == 1) "1 round" else "${rounds.size} rounds",
                            color = LightOrange.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(14.dp))
                FilterPillRow(
                    options = RoundFilter.entries.toList(),
                    selected = filter,
                    labelOf = { it.label },
                    onSelect = { filter = it }
                )
                if (all.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TallyTile("SOLVED", "$solved of ${all.size}", SuccessGreen, Modifier.weight(1f))
                        TallyTile(
                            "AVG GUESSES",
                            averageGuesses?.let { "%.1f".format(it) } ?: "—",
                            ProgressBurnt,
                            Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            when {
                history.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(Modifier.size(28.dp), color = ProgressBurnt) }

                rounds.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (all.isEmpty()) "No rounds recorded yet." else "No rounds in this filter.",
                        color = HintGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.align(Alignment.CenterHorizontally).readableWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        rounds,
                        key = { "${it.levelId}-${it.endedAt}-${it.guesses}" }
                    ) { round ->
                        RoundRow(
                            round = round,
                            revealed = round.levelId in game.progress.solvedLevels
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TallyTile(label: String, value: String, ink: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                label,
                color = HintGray,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(value, color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}
