package com.example.kusinakode.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Marks the window FLAG_SECURE while this composable is on screen, and clears
 * it again on the way out.
 *
 * Call it at the top of a screen that shows a credential - a password or a
 * one-time code - so that screen cannot be screenshotted, screen-recorded,
 * cast to a non-secure display, or captured in the recent-apps thumbnail.
 *
 * Scoped deliberately. FLAG_SECURE is a property of the single Activity window
 * the whole app shares, so setting it globally would also stop players from
 * screenshotting their badges and progress to share - the opposite of what a
 * game wants. Setting it in this effect and clearing it in onDispose keeps the
 * protection to just the screens that ask for it, and leaves every other screen
 * shareable.
 */
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            // Only clear it if nothing else re-set it; here each secure screen
            // owns the flag for its own lifetime, which is the common case.
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/** Unwraps the Activity behind a Compose LocalContext, or null if there isn't one. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
