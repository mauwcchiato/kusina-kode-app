package com.example.kusinakode.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.Session
import com.example.kusinakode.SoundFx
import com.example.kusinakode.data.repository.RemotePantryRepository
import com.example.kusinakode.domain.pantry.DrawResult
import com.example.kusinakode.domain.pantry.Ingredient
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.domain.pantry.PantrySnapshot
import com.example.kusinakode.domain.pantry.Rarity
import com.example.kusinakode.domain.repository.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PantryUiState(
    val snapshot: PantrySnapshot = PantrySnapshot(),
    val balanceKk: Long = 0,
    val loading: Boolean = false,
    val drawing: Boolean = false,
    val sellingId: String? = null,
    /** Set while the wheel is turning. */
    val spinning: Boolean = false,
    /** Set while a paid spin is being bought, before the wheel starts. */
    val buyingSpin: Boolean = false,
    /** Palayoks the last spin paid, until the player dismisses it. */
    val spinWon: Int? = null,
    /** Set while the whole stack of bauls is being emptied in one go. */
    val drawingAll: Boolean = false,
    /**
     * Progress through an OPEN ALL run.
     *
     * The run is one network round-trip per jar, so with a full shelf it is
     * a genuinely long wait. Without a count the pot just rattles and the
     * player cannot tell working from stuck.
     */
    val drawnSoFar: Int = 0,
    val drawTarget: Int = 0,
    val reveal: DrawResult? = null,
    /** Every jar from OPEN ALL, shown as a swipeable haul. */
    val haul: List<DrawResult> = emptyList(),
    /** Jars drawn this session that the player has not opened yet. */
    val newIngredientIds: Set<String> = emptySet(),
    val inspecting: Ingredient? = null,
    val notice: String? = null,
    /** Bauls granted for the current dish win. 0 if this load did not grant. */
    val lastGranted: Int = 0,
    val isGuest: Boolean = Session.userId == null,
    val filter: Rarity? = null
) {
    val visible: List<PantryEntry>
        get() {
            val base = if (filter == null) snapshot.entries
            else snapshot.entries.filter { it.ingredient.rarity == filter }
            return pinNewToTop(base)
        }

    /** Newest draws first so NEW badges sit together at the top of the shelf. */
    fun pinNewToTop(entries: List<PantryEntry>): List<PantryEntry> {
        if (newIngredientIds.isEmpty()) return entries
        val rank = newIngredientIds.toList()
        return entries.sortedByDescending { rank.indexOf(it.ingredient.id) }
    }

    fun countOf(rarity: Rarity): Int =
        snapshot.entries.filter { it.ingredient.rarity == rarity }.sumOf { it.qty }
}

class PantryViewModel(
    private val repository: PantryRepository,
    private val levelId: Int?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState(isGuest = Session.userId == null))
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        load()
        // Another screen's pantry call is this screen's news too — a spin taken
        // on the floating puck has to reach the Market Run card behind it.
        viewModelScope.launch {
            PantrySnapshotBus.snapshot.collect { shared ->
                if (shared != null && shared != _uiState.value.snapshot) {
                    _uiState.update { it.copy(snapshot = shared) }
                }
            }
        }
        viewModelScope.launch {
            PantrySnapshotBus.balanceKk.collect { shared ->
                if (shared != null && shared != _uiState.value.balanceKk) {
                    _uiState.update { it.copy(balanceKk = shared) }
                }
            }
        }
    }

    fun load(powerUpsUsed: Int = 0) {
        if (Session.userId == null) {
            _uiState.update { it.copy(isGuest = true, loading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, isGuest = false) }
            var granted = 0
            if (levelId != null && levelId > 0) {
                repository.grant(levelId, powerUpsUsed).onSuccess { granted = it }
            }
            repository.snapshot()
                .onSuccess { (snap, balance) ->
                    PantrySnapshotBus.publish(snap, balance)
                    _uiState.update {
                        it.copy(
                            snapshot = snap,
                            balanceKk = balance,
                            loading = false,
                            lastGranted = granted
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(loading = false, notice = e.message ?: "Could not load the pantry")
                    }
                }
        }
    }

    /**
     * Spends a banked spin. The prize is rolled server-side, so the wheel
     * animation is choreography over an answer the client is simply told.
     */
    fun spin() {
        val state = _uiState.value
        if (state.spinning || state.isGuest || state.snapshot.spinsAvailable <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(spinning = true, notice = null) }
            repository.spin()
                .onSuccess { (won, snap) ->
                    PantrySnapshotBus.publish(snap)
                    _uiState.update {
                        it.copy(spinning = false, spinWon = won, snapshot = snap)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(spinning = false, notice = e.message ?: "The wheel would not turn")
                    }
                }
        }
    }

    /**
     * Buys a spin with KK and turns it in one gesture.
     *
     * The two are one action to the player, so a purchase that lands but fails
     * to roll would leave them paid-up and staring at a still wheel. Banking
     * first and spinning second means the spin is already theirs if the roll
     * has to be retried.
     */
    fun buyAndSpin() {
        val state = _uiState.value
        if (state.spinning || state.isGuest || state.buyingSpin) return
        viewModelScope.launch {
            _uiState.update { it.copy(buyingSpin = true, notice = null) }
            repository.buySpin()
                .onSuccess { (snap, balance) ->
                    SoundFx.coin()
                    // Balance too: the charge has cleared by the time this
                    // returns, and a wallet still showing the old figure while
                    // the wheel turns is the app telling a small lie.
                    PantrySnapshotBus.publish(snap, balance)
                    _uiState.update {
                        it.copy(buyingSpin = false, snapshot = snap, balanceKk = balance)
                    }
                    spin()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            buyingSpin = false,
                            notice = e.message ?: "Could not buy a spin"
                        )
                    }
                }
        }
    }

    /** Clears the wheel result once the player has seen it. */
    fun dismissSpin() {
        _uiState.update { it.copy(spinWon = null) }
    }

    fun draw() {
        if (_uiState.value.drawing || _uiState.value.isGuest) return
        if ((_uiState.value.snapshot.drawsAvailable) <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(drawing = true, notice = null) }
            repository.draw(levelId)
                .onSuccess { (result, snap, balance) ->
                    PantrySnapshotBus.publish(snap, balance)
                    _uiState.update {
                        it.copy(
                            drawing = false,
                            reveal = result,
                            snapshot = snap,
                            balanceKk = balance,
                            newIngredientIds = it.newIngredientIds + setOfNotNull(result.ingredient?.id)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(drawing = false, notice = e.message ?: "The palayok would not open")
                    }
                }
        }
    }

    /**
     * Opens every remaining market run from one baul. There is no bulk
     * endpoint, so this walks the same draw the ritual uses; the haul
     * list is what the swipeable review shows afterwards.
     */
    fun drawAll() {
        val state = _uiState.value
        if (state.drawing || state.drawingAll || state.isGuest) return
        if (state.snapshot.drawsAvailable <= 0) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    drawingAll = true,
                    reveal = null,
                    haul = emptyList(),
                    notice = null,
                    drawnSoFar = 0,
                    drawTarget = state.snapshot.drawsAvailable
                )
            }
            val haul = mutableListOf<DrawResult>()
            val fresh = mutableSetOf<String>()
            var failure: String? = null
            var remaining = state.snapshot.drawsAvailable
            while (remaining > 0) {
                val result = repository.draw(levelId)
                val payload = result.getOrNull()
                if (payload == null) {
                    failure = result.exceptionOrNull()?.message ?: "The palayok would not open"
                    break
                }
                val (draw, snap, balance) = payload
                haul += draw
                draw.ingredient?.id?.let { fresh += it }
                remaining = minOf(remaining - 1, draw.drawsLeft)
                PantrySnapshotBus.publish(snap, balance)
                // Published per jar, not at the end: this is the only signal
                // the overlay has that the wait is moving.
                _uiState.update {
                    it.copy(snapshot = snap, balanceKk = balance, drawnSoFar = haul.size)
                }
            }
            _uiState.update {
                it.copy(
                    drawingAll = false,
                    drawnSoFar = 0,
                    drawTarget = 0,
                    haul = haul,
                    newIngredientIds = it.newIngredientIds + fresh,
                    notice = when {
                        failure != null && haul.isEmpty() -> failure
                        failure != null -> "Opened ${haul.size} before the market closed: $failure"
                        else -> null
                    }
                )
            }
        }
    }

    fun sell(id: String, qty: Int = 1) {
        sellMany(listOf(id to qty))
    }

    /**
     * Sells each stack in order. There is no bulk endpoint, so a failure
     * mid-way keeps whatever already went through.
     */
    fun sellMany(sales: List<Pair<String, Int>>) {
        val clean = sales.filter { it.second > 0 }
        if (clean.isEmpty() || _uiState.value.sellingId != null || _uiState.value.isGuest) return
        viewModelScope.launch {
            val firstName = _uiState.value.snapshot.entries
                .firstOrNull { it.ingredient.id == clean.first().first }
                ?.ingredient?.name
            var jars = 0
            var earned = 0
            var failure: String? = null
            for ((id, qty) in clean) {
                _uiState.update { it.copy(sellingId = id, notice = null) }
                val result = repository.sell(id, qty)
                val payload = result.getOrNull()
                if (payload == null) {
                    failure = result.exceptionOrNull()?.message ?: "Could not sell that ingredient"
                    break
                }
                val (sold, snap, balance) = payload
                jars += sold.sold
                earned += sold.kkAwarded
                val inspecting = _uiState.value.inspecting
                PantrySnapshotBus.publish(snap, balance)
                _uiState.update {
                    it.copy(
                        snapshot = snap,
                        balanceKk = balance,
                        inspecting = if (inspecting?.id == id && sold.qtyLeft <= 0) null else inspecting
                    )
                }
            }
            _uiState.update {
                it.copy(
                    sellingId = null,
                    notice = when {
                        failure != null && jars == 0 -> failure
                        failure != null -> "Sold $jars ingredients for $earned KK, then: $failure"
                        jars == 1 && clean.size == 1 ->
                            "Sold 1 ${firstName ?: "ingredient"} for $earned KK"
                        else -> "Sold $jars ingredients for $earned KK"
                    }
                )
            }
        }
    }

    fun inspect(ingredient: Ingredient?) {
        _uiState.update { state ->
            val seen = if (ingredient != null) state.newIngredientIds - ingredient.id
            else state.newIngredientIds
            state.copy(inspecting = ingredient, newIngredientIds = seen)
        }
    }

    fun setFilter(rarity: Rarity?) {
        _uiState.update { it.copy(filter = rarity) }
    }

    fun dismissReveal() {
        _uiState.update { it.copy(reveal = null) }
    }

    fun dismissHaul() {
        _uiState.update { it.copy(haul = emptyList()) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    companion object {
        fun factory(levelId: Int? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PantryViewModel(RemotePantryRepository(), levelId) as T
                }
            }
    }
}
