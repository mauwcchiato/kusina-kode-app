package com.example.kusinakode.ui.explore

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.game.ResumeViewModel
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.ui.levels.LevelsViewModel

@Composable
fun ExploreRoute(
    onLevelSelected: (Int) -> Unit,
    onViewDish: (Int) -> Unit,
    onHome: () -> Unit,
    onCompleted: () -> Unit,
    onProfile: () -> Unit,
    onLeadership: () -> Unit,
    vm: LevelsViewModel = viewModel(),
    onWallet: () -> Unit = {},
    onSettings: () -> Unit = {},
    initialRegion: Region? = null
) {
    val ui by vm.uiState.collectAsState()
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()
    // Reactive, so the map fills in as the account syncs.
    val completed = game.progress.solvedLevels.size

    // This screen survives on the back stack while a round is played and
    // abandoned, so re-check for resumable boards on the way back in.
    val resume: ResumeViewModel = viewModel()
    val resumable by resume.resumableLevels.collectAsState()
    LaunchedEffect(Unit) { resume.refresh() }

    when {
        ui.isLoading -> Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        ui.error != null -> Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = clickSfx { vm.loadUnlocks() }) {
                    Text("Retry")
                }
            }
        }
        else -> ExploreScreen(
            unlockedUpTo = ui.unlockedUpTo,
            completedUpTo = completed,
            resumableLevels = resumable,
            onPlayLevel = onLevelSelected,
            onViewDish = onViewDish,
            onHome = onHome,
            onProfile = onProfile,
            onLeaderboard = onLeadership,
            onWallet = onWallet,
            onLearn = onCompleted,
            onSettings = onSettings,
            initialRegion = initialRegion
        )
    }
}
