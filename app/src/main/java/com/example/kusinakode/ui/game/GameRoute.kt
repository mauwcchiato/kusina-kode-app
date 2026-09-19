package com.example.kusinakode.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.domain.GamificationEvents
import com.example.kusinakode.domain.RoundScored

@Composable
fun GameRoute(
    level: Int,
    onLevelSelect: () -> Unit,
    onGoHome: () -> Unit,
    onPlayLevel: (Int) -> Unit,
    onViewDish: (Int) -> Unit,
    onOpenDocumentary: () -> Unit = {}
) {
    val context = LocalContext.current
    // Keyed per level so navigating to another level starts a fresh round.
    val viewModel: GameViewModel = viewModel(
        key = "game_$level",
        factory = GameViewModel.Factory(level, context.applicationContext)
    )
    val uiState by viewModel.uiState.collectAsState()

    // What Module 2 credited for this round, so the win screen can show it.
    val scored by produceState<RoundScored?>(initialValue = null, level) {
        GamificationEvents.roundsScored.collect { event ->
            if (event.levelId == level) value = event
        }
    }

    val hasNextLevel = level < LevelProvider.levelCount

    GameScreen(
        uiState = uiState,
        onKey = viewModel::onKey,
        onBackspace = viewModel::onBackspace,
        onEnter = viewModel::onEnter,
        onRestart = viewModel::restart,
        onReveal = viewModel::useReveal,
        onBomb = viewModel::useBomb,
        onSolve = viewModel::useSolve,
        onDismissPowerUpMessage = viewModel::dismissPowerUpMessage,
        onSetPaused = viewModel::setPaused,
        onExit = onLevelSelect,
        onBackToMap = onLevelSelect,
        onGoHome = onGoHome,
        onPlayNext = if (hasNextLevel) ({ onPlayLevel(level + 1) }) else null,
        onViewDish = { onViewDish(level) },
        onOpenDocumentary = onOpenDocumentary,
        scored = scored
    )
}
