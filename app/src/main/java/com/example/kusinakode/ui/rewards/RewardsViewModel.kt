package com.example.kusinakode.ui.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.KusinaKodeApp
import com.example.kusinakode.KusinaNotifications
import com.example.kusinakode.Session
import com.example.kusinakode.api.EarnBadgeData
import com.example.kusinakode.api.EarnIslandData
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.api.RewardHistoryItem
import com.example.kusinakode.api.alreadyClaimed
import com.example.kusinakode.data.repository.RemotePantryRepository
import com.example.kusinakode.data.repository.RemoteWalletRepository
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.repository.WalletRepository
import com.example.kusinakode.ui.pantry.PantrySnapshotBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewardsUiState(
    val balanceKk: Long = 0,
    val isLive: Boolean = false,
    val dailyClaimable: Boolean = false,
    val dailyClaimed: Boolean = false,
    val dailyAmount: Long = 5,
    val dishAmount: Long = 10,
    val islands: List<EarnIslandData> = emptyList(),
    val badges: List<EarnBadgeData> = emptyList(),
    val history: List<RewardHistoryItem> = emptyList(),
    val claimBusy: Boolean = false,
    val notice: String? = null
)

class RewardsViewModel(app: Application) : AndroidViewModel(app) {

    private val walletRepository: WalletRepository = RemoteWalletRepository()

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            walletRepository.walletStatus(Session.userId).onSuccess { status ->
                _uiState.update { it.copy(balanceKk = status.balanceKk, isLive = status.isLive) }
            }
            runCatching { KusinaApi.getEarnStatus() }
                .onSuccess { resp ->
                    val data = resp.data ?: return@onSuccess
                    val solved = runCatching {
                        (getApplication() as KusinaKodeApp).gamification
                            .progress(Session.userId)
                            .first()
                            .solvedLevels
                    }.getOrDefault(emptySet())
                    _uiState.update {
                        it.copy(
                            dailyClaimable = data.daily?.claimable == true,
                            dailyClaimed = data.daily?.claimed == true,
                            dailyAmount = data.daily?.amount_kk ?: 5,
                            dishAmount = data.round_win_kk,
                            islands = mergeIslandEarnRows(data.islands, solved),
                            badges = data.badges
                        )
                    }
                }
            runCatching { KusinaApi.getRewardHistory() }
                .onSuccess { resp ->
                    if (resp.status == "success") {
                        _uiState.update { it.copy(history = resp.data) }
                    }
                }
        }
    }

    fun claimDaily() {
        claim("daily")
    }

    fun claimIsland(islandId: String) {
        claim("island", island = islandId)
    }

    fun claimBadge(badgeId: String) {
        claim("badge", badgeId = badgeId)
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun claim(kind: String, island: String? = null, badgeId: String? = null) {
        if (_uiState.value.claimBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(claimBusy = true, notice = null) }
            val result = runCatching {
                ChainQueue.serialized {
                    val resp = KusinaApi.claimKk(kind, island, badgeId)
                    if (resp.alreadyClaimed()) return@serialized resp
                    if (resp.status != "success" || resp.tx_ref == null) {
                        error(resp.message ?: "Could not claim")
                    }
                    KusinaApi.settleReward(resp.tx_ref)
                    resp
                }
            }
            _uiState.update { it.copy(claimBusy = false) }
            result.onSuccess { resp ->
                val amount = when (kind) {
                    "daily" -> _uiState.value.dailyAmount
                    "badge" -> _uiState.value.badges.firstOrNull { it.id == badgeId }?.amount_kk ?: 25
                    else -> _uiState.value.islands.firstOrNull { row -> row.id == island }?.amount_kk ?: 15
                }
                val ctx = getApplication<Application>()
                if (kind == "island") {
                    val name = _uiState.value.islands.firstOrNull { row -> row.id == island }?.name
                        ?: island.orEmpty().replaceFirstChar { it.uppercase() }
                    KusinaNotifications.islandClaimed(ctx, name, amount)
                }
                _uiState.update { it.copy(notice = "Claimed +$amount KK") }
                refresh()
                // The daily also banks a palayok spin. Publish the pantry so
                // the inbox keeps a "spin waiting" row instead of vanishing
                // the free-spin notice with nothing to collect.
                if (kind == "daily") {
                    RemotePantryRepository().snapshot().onSuccess { (snap, balance) ->
                        PantrySnapshotBus.publish(snap, balance)
                    }
                }
                // D-28: Phase 4 has just written badges.tx_hash, but the badge
                // shelf keeps serving the copy it loaded before the claim. Pull
                // the confirmed record down so Profile flips pending -> verified
                // without waiting for an app restart.
                runCatching {
                    (getApplication() as KusinaKodeApp).gamificationCoordinator.refresh()
                }
            }.onFailure { e ->
                _uiState.update { it.copy(notice = e.message ?: "Could not claim") }
            }
        }
    }
}
