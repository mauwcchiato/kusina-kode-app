// src/main/java/com/example/kusinakode/Unlocks.kt
package com.example.kusinakode

import android.content.Context
import com.example.kusinakode.Session

private const val PREFS_NAME       = "kusinakode_prefs"
private const val KEY_UNLOCK_LEVEL = "unlock_level"
private const val MAX_LEVEL        = 20

/**
 * Persists the highest level unlocked by the player (1..MAX_LEVEL).
 * Defaults to 1 on first run.
 */
object Unlocks {
    /** Read the saved unlock level, default is 1. */
    fun get(context: Context): Int {
        return UnlockManager.getUnlockedLevel(context, Session.userId)
    }

    /**
     * Legacy API used by some screens.
     *
     * STRICT: only allows progressive unlock by exactly +1 (prevents bulk-unlock bugs).
     */
    fun unlock(context: Context, level: Int) {
        val current = get(context)
        val target = level.coerceIn(1, MAX_LEVEL)
        if (target == current + 1) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Keep legacy key in sync for backward-compat (but do not rely on it for UI)
            prefs.edit().putInt(KEY_UNLOCK_LEVEL, target).apply()
        }
    }
}
