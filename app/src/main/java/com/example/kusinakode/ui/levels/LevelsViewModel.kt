package com.example.kusinakode.ui.levels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.Session
import com.example.kusinakode.data.repository.DefaultUnlockRepository
import com.example.kusinakode.domain.repository.UnlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LevelsViewModel(application: Application) : AndroidViewModel(application) {

    private val unlockRepository: UnlockRepository = DefaultUnlockRepository(application)

    private val _uiState = MutableStateFlow(LevelsUiState())
    val uiState: StateFlow<LevelsUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = LevelsUiState(unlockedUpTo = localUnlocks())
        loadUnlocks()
    }

    private fun localUnlocks(): Int =
        unlockRepository.localHighestUnlocked(Session.userId?.takeIf { it > 0 })

    fun loadUnlocks() {
        viewModelScope.launch {
            val uid = Session.userId
            if (uid == null || uid <= 0) {
                _uiState.update {
                    it.copy(unlockedUpTo = unlockRepository.localHighestUnlocked(null), error = null)
                }
                return@launch
            }
            unlockRepository.syncFromServer(uid)
                .onSuccess { merged ->
                    _uiState.update { it.copy(unlockedUpTo = merged, error = null) }
                }
                .onFailure {
                    // Offline: keep whatever this phone already unlocked.
                    _uiState.update { it.copy(unlockedUpTo = unlockRepository.localHighestUnlocked(uid)) }
                }
        }
    }
}
