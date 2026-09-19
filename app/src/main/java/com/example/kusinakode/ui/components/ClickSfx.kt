package com.example.kusinakode.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.kusinakode.SoundFx

/**
 * Wraps a button's action so the press is heard.
 *
 * Compose gives no central hook for this: Material's own buttons take a plain
 * `onClick` and their ripple is a visual, not an audible, affordance. Rather
 * than repeat the same two lines at every call site, wrap the lambda:
 *
 * ```
 * Button(onClick = clickSfx { save() }) { … }
 * Button(onClick = clickSfx(onBack)) { … }
 * ```
 *
 * [SoundFx.tap] already respects the player's Sound FX and haptics settings,
 * so a muted player hears nothing and feels nothing.
 *
 * Use it only on controls that do something. A card that opens a detail page
 * counts; a tile in the word grid does not, because the grid plays its own
 * letter cue and would otherwise double up.
 */
@Composable
fun clickSfx(onClick: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    return {
        SoundFx.tap(ctx)
        onClick()
    }
}
