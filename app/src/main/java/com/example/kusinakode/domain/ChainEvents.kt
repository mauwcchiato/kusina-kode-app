package com.example.kusinakode.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the reward pipeline is doing right now, in words a player can read.
 *
 * The four-phase pipeline (encrypt/hash, sign, append, execute) takes real
 * time. Without something on screen the app just looks frozen, so every stage
 * reports itself here and the UI narrates it. This carries no crypto detail —
 * only a label, and the TxHash once there is one to show.
 */
sealed interface ChainActivity {

    /** Nothing in flight. */
    data object Idle : ChainActivity

    /** Phases 1-3: the reward is being encrypted, signed and appended. */
    data class Validating(val label: String) : ChainActivity

    /** Phase 4 returned a receipt. */
    data class Minted(
        val label: String,
        val txHash: String,
        val amountKk: Long?
    ) : ChainActivity

    /**
     * The pipeline could not finish. Play is never blocked by this — the
     * badge stays pending and the next sync retries it.
     */
    data class Failed(val label: String) : ChainActivity
}

/**
 * App-wide stream of reward-pipeline progress.
 *
 * A StateFlow rather than a SharedFlow: a player who opens the app mid-mint
 * should see the current stage, not miss the event because they were on
 * another screen when it fired.
 */
object ChainEvents {
    private val _activity = MutableStateFlow<ChainActivity>(ChainActivity.Idle)
    val activity: StateFlow<ChainActivity> = _activity.asStateFlow()

    fun report(activity: ChainActivity) {
        _activity.value = activity
    }

    /** Clears a finished banner once the player has had a chance to read it. */
    fun clear() {
        _activity.value = ChainActivity.Idle
    }
}
