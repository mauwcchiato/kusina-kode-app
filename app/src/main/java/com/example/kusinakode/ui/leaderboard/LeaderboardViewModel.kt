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
    val window: LeaderboardWindow = LeaderboardWindow.AllTime,
    /**
     * Every listed player's all-time dishes solved, by lower-cased name - what
     * their chef rank badge is worked out from. Kept apart from [entries]
     * because the Today and Weekly boards count only dishes cooked in that
     * period, which would put a Kusina Master down as a Kusinero.
     */
    val dishesByName: Map<String, Int> = emptyMap()
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
                .onSuccess { rows ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = rows,
                            dishesByName = if (window == LeaderboardWindow.AllTime) rows.dishCounts()
                            else it.dishesByName
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
            // A period board opened first still needs the all-time counts.
            if (window != LeaderboardWindow.AllTime && _uiState.value.dishesByName.isEmpty()) {
                leaderboardRepository.topPlayers(window = LeaderboardWindow.AllTime)
                    .onSuccess { rows -> _uiState.update { it.copy(dishesByName = rows.dishCounts()) } }
            }
        }
    }

    private fun List<LeaderboardRow>.dishCounts(): Map<String, Int> =
        associate { it.name.lowercase() to it.correctCount }

    /** Switches the board's period and reloads it from the server. */
    fun selectWindow(window: LeaderboardWindow) {
        if (window == _uiState.value.window) return
        loadLeaderboard(window)
    }
}
