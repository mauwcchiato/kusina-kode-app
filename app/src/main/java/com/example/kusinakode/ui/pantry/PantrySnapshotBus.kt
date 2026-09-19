package com.example.kusinakode.ui.pantry

import com.example.kusinakode.domain.pantry.PantrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The last pantry snapshot anyone fetched, shared across every screen showing one.
 *
 * Five surfaces read the pantry — the Kodex, the shelf, the inbox, the round's
 * pot ritual and the floating spin puck — and each builds its own
 * [PantryViewModel] against its own navigation entry. That is the right scope
 * for a screen, but it meant a spin taken on the puck paid into a snapshot only
 * the puck could see: the Market Run card sat behind it still showing the old
 * count until the screen was rebuilt.
 *
 * So the snapshot is published here whenever a repository call returns a fresh
 * one, and every view model watches. Screens stay independent; the number they
 * quote does not.
 *
 * Only results from the server are published. A view model updating itself from
 * this bus never writes back, so there is no loop.
 */
object PantrySnapshotBus {

    private val _snapshot = MutableStateFlow<PantrySnapshot?>(null)
    val snapshot: StateFlow<PantrySnapshot?> = _snapshot.asStateFlow()

    private val _balanceKk = MutableStateFlow<Long?>(null)
    val balanceKk: StateFlow<Long?> = _balanceKk.asStateFlow()

    fun publish(snapshot: PantrySnapshot) {
        _snapshot.value = snapshot
    }

    fun publish(snapshot: PantrySnapshot, balanceKk: Long) {
        _snapshot.value = snapshot
        _balanceKk.value = balanceKk
    }

    /** Signing out must not leave the next account reading these numbers. */
    fun clear() {
        _snapshot.value = null
        _balanceKk.value = null
    }
}
