package com.example.kusinakode.ui.explore

import androidx.compose.runtime.*
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

    ExploreScreen(
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
