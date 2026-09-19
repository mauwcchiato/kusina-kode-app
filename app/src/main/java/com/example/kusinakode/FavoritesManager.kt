package com.example.kusinakode

import android.content.Context

/**
 * Dishes the player hearted in the KODEX, stored per user on-device.
 * Same SharedPreferences pattern as [CompletedManager]/[UnlockManager].
 */
object FavoritesManager {
    private const val PREFS_NAME = "kusinakode_prefs"

    private fun keyForUser(userId: Int?): String =
        if (userId != null && userId > 0) "favorites_user_$userId" else "favorites_guest"

    fun favorites(context: Context, userId: Int?): Set<Int> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(keyForUser(userId), emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    fun isFavorite(context: Context, userId: Int?, levelId: Int): Boolean =
        levelId in favorites(context, userId)

    /** Flips the heart for [levelId] and returns its new state. */
    fun toggle(context: Context, userId: Int?, levelId: Int): Boolean {
        val current = favorites(context, userId).toMutableSet()
        val nowFavorite = if (levelId in current) {
            current.remove(levelId); false
        } else {
            current.add(levelId); true
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            // Store a fresh set — SharedPreferences must not be handed a mutated instance.
            .putStringSet(keyForUser(userId), current.map { it.toString() }.toSet())
            .apply()
        return nowFavorite
    }
}
