package com.example.kusinakode.ui.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tracks whether a full-screen pantry overlay — the wheel, the draw ritual —
 * currently owns the screen.
 *
 * The floating spin puck is mounted above the whole navigation graph so the
 * inbox can open the wheel from anywhere, which also means it floats over
 * these overlays and cannot see them. This is how they tell it to step aside.
 *
 * A count rather than a flag: overlays can hand over to one another (the
 * ritual deals into the haul), and the puck must not reappear in the gap.
 */
object PantryOverlayHost {

    private val _openCount = MutableStateFlow(0)
    val openCount: StateFlow<Int> = _openCount.asStateFlow()

    fun acquire() = _openCount.update { it + 1 }

    fun release() = _openCount.update { (it - 1).coerceAtLeast(0) }
}

/** Claims the screen for as long as the calling composable is in the tree. */
@Composable
internal fun HoldsTheScreen() {
    DisposableEffect(Unit) {
        PantryOverlayHost.acquire()
        onDispose { PantryOverlayHost.release() }
    }
}
