package com.example.kusinakode.ui.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.Session
import com.example.kusinakode.data.repository.RemoteAttemptHistoryRepository
import com.example.kusinakode.domain.model.AttemptRecord
import com.example.kusinakode.domain.repository.AttemptHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttemptHistoryUiState(
    val entries: List<AttemptRecord> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class AttemptHistoryViewModel(
    private val repository: AttemptHistoryRepository = RemoteAttemptHistoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttemptHistoryUiState())
    val uiState: StateFlow<AttemptHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val userId = Session.userId
        if (userId == null || userId <= 0) {
            _uiState.value = AttemptHistoryUiState(isLoading = false, error = "Log in to see your history.")
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.recentAttempts(userId)
                .onSuccess { rows -> _uiState.value = AttemptHistoryUiState(entries = rows, isLoading = false) }
                .onFailure { e ->
                    _uiState.value = AttemptHistoryUiState(isLoading = false, error = e.message)
                }
        }
    }
}
