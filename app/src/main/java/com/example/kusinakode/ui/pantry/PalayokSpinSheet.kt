package com.example.kusinakode.ui.pantry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the palayok spin sheet is showing.
 *
 * The sheet belongs to the floating puck, which is mounted once at the
 * navigation root — so a screen underneath has no handle on it. Rather than
 * pass a callback down through every route, the open state lives here and the
 * puck watches it. Anything that wants to offer a spin can say so.
 *
 * The inbox is the first caller: a "free palayok spin" row that navigated to
 * the rewards screen was asking the player to go find the thing it had just
 * told them about.
 */
object PalayokSpinSheet {

    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    fun open() {
        _open.value = true
    }

    fun close() {
        _open.value = false
    }
}
