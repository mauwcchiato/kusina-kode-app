package com.example.kusinakode.ui.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.Session
import com.example.kusinakode.SoundFx
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.data.repository.ReelCatalogStore
import com.example.kusinakode.data.repository.publishedIds
import com.example.kusinakode.data.repository.toShopItems
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.shop.AvatarSlot
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.domain.shop.ShopItem
import com.example.kusinakode.domain.shop.ShopKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

data class ShopUiState(
    val balanceKk: Long = 0,
    val owned: Set<String> = emptySet(),
    val equipped: Map<AvatarSlot, String> = emptyMap(),
    val busyId: String? = null,
    val notice: String? = null,
    val reading: ShopItem? = null,
    val pendingBuy: ShopItem? = null,
    /**
     * The reel shelf, read from here rather than from KusinaShop directly so
     * a fetch that lands after the screen is open actually redraws it.
     * KusinaShop stays plain Kotlin with no Compose state in it.
     */
    val reels: List<ShopItem> = KusinaShop.documentaries
)

class ShopViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("kusinakode_prefs", 0)

    private val _uiState = MutableStateFlow(ShopUiState(equipped = loadEquipped()))
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            // The admin-managed reel catalogue, if the server has one yet.
            //
            // runCatching, not a failure path: reels/list.php is not built,
            // so today this always throws and the shelf quietly stays on the
            // reels compiled into the APK. The moment the endpoint answers,
            // the same code starts merging its rows in - no app release, no
            // flag to flip. A later outage lands here too and falls back to
            // the last catalogue saved on the device.
            runCatching { KusinaApi.getReels() }
                .onSuccess { rows ->
                    if (rows.isNotEmpty()) {
                        KusinaShop.applyRemoteReels(rows.toShopItems(), rows.publishedIds())
                        ReelCatalogStore.save(getApplication(), rows)
                        _uiState.update { it.copy(reels = KusinaShop.documentaries) }
                    }
                }

            runCatching { KusinaApi.getWalletBalance().data }
                .onSuccess { data ->
                    if (data != null) _uiState.update { it.copy(balanceKk = data.balance_kk) }
                }
            val remote = runCatching { KusinaApi.getShopOwned().data }.getOrNull()
            val remoteOwned = remote?.owned.orEmpty()
            val local = loadOwned()
            val merged = (remoteOwned + local).toSet()
            saveOwned(merged)
            val remoteEquip = ChefLook.parseEquipped(remote?.equipped.orEmpty())
            val equipped = if (remoteEquip.isNotEmpty()) remoteEquip else loadEquipped()
            saveEquipped(equipped)
            _uiState.update { it.copy(owned = merged, equipped = equipped) }
        }
    }

    fun requestBuy(item: ShopItem) {
        if (_uiState.value.busyId != null) return
        if (item.id in _uiState.value.owned) return
        _uiState.update { it.copy(pendingBuy = item, notice = null) }
    }

    fun cancelBuy() {
        _uiState.update { it.copy(pendingBuy = null) }
    }

    fun confirmBuy() {
        val item = _uiState.value.pendingBuy ?: return
        _uiState.update { it.copy(pendingBuy = null) }
        buy(item)
    }

    fun buy(item: ShopItem) {
        if (_uiState.value.busyId != null) return
        if (item.id in _uiState.value.owned) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyId = item.id, notice = null) }
            val ok = runCatching {
                ChainQueue.serialized {
                    val resp = KusinaApi.spendKk(item.id)
                    val tx = resp.tx_ref ?: error(resp.message ?: "Could not unlock")
                    val settled = KusinaApi.settleReward(tx)
                    if (settled?.status != "confirmed" && settled?.phase != 4) {
                        error("Unlock is still settling — try again in a moment")
                    }
                }
            }
            _uiState.update { it.copy(busyId = null) }
            ok.onSuccess {
                SoundFx.coin()
                val next = _uiState.value.owned + item.id
                saveOwned(next)
                var equipped = _uiState.value.equipped
                val slot = item.slot
                if (slot != null) {
                    equipped = equipped + (slot to item.id)
                    saveEquipped(equipped)
                    runCatching {
                        KusinaApi.equipLook(
                            characterId = equipped[AvatarSlot.CHARACTER],
                            frameId = equipped[AvatarSlot.FRAME]
                        )
                    }
                }
                val reveal = if (item.kind == ShopKind.ENCYCLOPEDIA) item else null
                _uiState.update {
                    it.copy(
                        owned = next,
                        equipped = equipped,
                        notice = if (reveal != null) null else if (item.kind == ShopKind.AVATAR) "Bought ${item.title}" else "Unlocked ${item.title}",
                        reading = reveal ?: it.reading
                    )
                }
                refresh()
            }.onFailure { e ->
                _uiState.update { it.copy(notice = e.message ?: "Need ${item.coinCost} KK") }
            }
        }
    }

    fun wearLook(characterId: String?, frameId: String?) {
        val owned = _uiState.value.owned
        val next = _uiState.value.equipped.toMutableMap()
        if (characterId == null) next.remove(AvatarSlot.CHARACTER)
        else if (characterId in owned) next[AvatarSlot.CHARACTER] = characterId
        if (frameId == null) next.remove(AvatarSlot.FRAME)
        else if (frameId in owned) next[AvatarSlot.FRAME] = frameId
        saveEquipped(next)
        _uiState.update { it.copy(equipped = next) }
        syncLook(next)
    }

    fun equip(item: ShopItem) {
        val slot = item.slot ?: return
        if (item.id !in _uiState.value.owned) return
        val next = _uiState.value.equipped.toMutableMap()
        if (next[slot] == item.id) next.remove(slot) else next[slot] = item.id
        saveEquipped(next)
        _uiState.update { it.copy(equipped = next) }
        syncLook(next)
    }

    fun openArticle(item: ShopItem) {
        if (item.id in _uiState.value.owned) _uiState.update { it.copy(reading = item) }
    }

    fun closeArticle() {
        _uiState.update { it.copy(reading = null) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun scope(): String = "user_${Session.userId ?: 0}"

    private fun loadOwned(): Set<String> {
        val raw = prefs.getString("shop_owned_${scope()}", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun saveOwned(owned: Set<String>) {
        val arr = JSONArray()
        owned.forEach { arr.put(it) }
        prefs.edit().putString("shop_owned_${scope()}", arr.toString()).apply()
    }

    private fun loadEquipped(): Map<AvatarSlot, String> {
        val prefix = "shop_equip_${scope()}_"
        return AvatarSlot.entries.mapNotNull { slot ->
            prefs.getString(prefix + slot.name, null)?.let { slot to it }
        }.toMap()
    }

    private fun saveEquipped(equipped: Map<AvatarSlot, String>) {
        ChefLook.applyEquipped(getApplication(), equipped)
    }

    private fun syncLook(equipped: Map<AvatarSlot, String>) {
        viewModelScope.launch {
            runCatching {
                KusinaApi.equipLook(
                    characterId = equipped[AvatarSlot.CHARACTER],
                    frameId = equipped[AvatarSlot.FRAME]
                )
            }
        }
    }
}
