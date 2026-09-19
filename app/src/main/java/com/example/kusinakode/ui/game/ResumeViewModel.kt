package com.example.kusinakode.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.Session
import com.example.kusinakode.data.repository.LocalRoundStateRepository
import com.example.kusinakode.domain.repository.RoundStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Which levels have a half-finished board waiting.
 *
 * Shared by Home and Explore so both call the same round "Continue" — kept
 * out of LevelsViewModel because Home has no business triggering that
 * screen's unlock sync just to label a button.
 */
class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val roundStore: RoundStateRepository =
        LocalRoundStateRepository(application) { Session.userId }

    private val _resumableLevels = MutableStateFlow<Set<Int>>(emptySet())
    val resumableLevels: StateFlow<Set<Int>> = _resumableLevels.asStateFlow()

    init {
        refresh()
    }

    /**
     * Re-reads the saved rounds. Screens call this when they come back into
     * view: a round can be started and abandoned while they sit alive on the
     * back stack, so init alone would leave a stale label.
     */
    fun refresh() {
        viewModelScope.launch {
            _resumableLevels.value = (1..LevelProvider.levelCount)
                .filter { roundStore.load(it)?.isInProgress == true }
                .toSet()
        }
    }
}
