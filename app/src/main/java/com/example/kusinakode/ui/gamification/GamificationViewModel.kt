package com.example.kusinakode.ui.gamification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.KusinaKodeApp
import com.example.kusinakode.Session
import com.example.kusinakode.domain.gamification.BadgeRules
import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.BadgeType
import com.example.kusinakode.domain.model.PlayerProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** A badge slot for the UI: either earned (with its verification state) or still locked. */
data class BadgeSlot(
    val badgeId: String,
    val type: BadgeType,
    val title: String,
    val criteria: String,
    val earned: Badge?
) {
    val isEarned: Boolean get() = earned != null
}

data class GamificationUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val badges: List<BadgeSlot> = emptyList()
) {
    val earnedCount: Int get() = badges.count { it.isEarned }
}

/**
 * Read model over the gamification store, shared by Home, Profile and the
 * progress view.
 */
class GamificationViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as KusinaKodeApp).gamification

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    init {
        val userId = Session.userId

        // Catch up on dishes cleared before points existed, and grant any
        // badge they already earned, so the shelf is right on first open.
        viewModelScope.launch {
            (app as KusinaKodeApp).gamificationCoordinator.refresh()
        }

        viewModelScope.launch {
            combine(
                repository.progress(userId),
                repository.badges(userId)
            ) { progress, badges ->
                val byId = badges.associateBy { it.badgeId }
                GamificationUiState(
                    progress = progress,
                    // Show the whole catalog so players can see what's next.
                    badges = BadgeRules.catalog().map { (type, milestone, id) ->
                        BadgeSlot(
                            badgeId = id,
                            type = type,
                            title = milestone.title,
                            criteria = milestone.criteria,
                            earned = byId[id]
                        )
                    }
                )
            }.collect { _uiState.value = it }
        }
    }
}
