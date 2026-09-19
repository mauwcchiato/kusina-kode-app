// File: app/src/main/java/com/example/kusinakode/ui/leaderboard/LeaderboardRoute.kt
package com.example.kusinakode.ui.leaderboard

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
import com.example.kusinakode.LeadershipScreen // or LeadershipScreen import
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

    Box(Modifier.fillMaxSize()) {
        when {
            // Only the first load takes over the screen. Switching period
            // keeps the board and its tabs on screen, so the control the
            // player just tapped doesn't vanish under a spinner.
            ui.isLoading && ui.entries.isEmpty() -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            ui.errorMessage != null -> {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ui.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = clickSfx { viewModel.loadLeaderboard() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                // Pass the real entries into your styled screen
                com.example.kusinakode.LeadershipScreen(
                    entries = ui.entries,
                    window = ui.window,
                    onSelectWindow = viewModel::selectWindow,
                    onBack = onBack,
                    onHome = onHome,
                    onProfile = onProfile,
                    onLevels = onLevels,
                    onCompleted = onCompleted,
                    onWallet = onWallet
                )
            }
        }
    }
}
