package com.example.kusinakode.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.data.repository.RemoteLeaderboardRepository
import com.example.kusinakode.domain.model.LeaderboardRow
import com.example.kusinakode.domain.repository.LeaderboardRepository
import com.example.kusinakode.domain.repository.LeaderboardWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val entries: List<LeaderboardRow> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val window: LeaderboardWindow = LeaderboardWindow.AllTime
)

class LeaderboardViewModel(
    private val leaderboardRepository: LeaderboardRepository = RemoteLeaderboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard(window: LeaderboardWindow = _uiState.value.window) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                window = window,
                entries = if (window == it.window) it.entries else emptyList()
            )
        }
        viewModelScope.launch {
            leaderboardRepository.topPlayers(window = window)
                .onSuccess { rows -> _uiState.update { it.copy(isLoading = false, entries = rows) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    /** Switches the board's period and reloads it from the server. */
    fun selectWindow(window: LeaderboardWindow) {
        if (window == _uiState.value.window) return
        loadLeaderboard(window)
    }
}
