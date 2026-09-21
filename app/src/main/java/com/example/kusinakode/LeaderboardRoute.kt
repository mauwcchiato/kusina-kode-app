// File: app/src/main/java/com/example/kusinakode/ui/leaderboard/LeaderboardRoute.kt
package com.example.kusinakode.ui.leaderboard

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.LeadershipScreen
import com.example.kusinakode.Session

@Composable
fun LeaderboardRoute(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLevels: () -> Unit,
    onCompleted: () -> Unit,
    onWallet: () -> Unit = {},
    viewModel: LeaderboardViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()

    // Refresh when entering / when account changes, so rank + points stay current.
    LaunchedEffect(Session.userId) {
        viewModel.loadLeaderboard()
    }

    LeadershipScreen(
        entries = ui.entries,
        window = ui.window,
        onSelectWindow = viewModel::selectWindow,
        isLoading = ui.isLoading,
        onBack = onBack,
        onHome = onHome,
        onProfile = onProfile,
        onLevels = onLevels,
        onCompleted = onCompleted,
        onWallet = onWallet
    )
}
