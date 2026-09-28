package com.example.kusinakode

import android.content.Context
import com.example.kusinakode.domain.model.Region

/**
 * Remembers which island the player last opened a round in, per user.
 *
 * The Home "Continue / Play" card uses this so it follows the region you are
 * actually working on: after playing a Visayas level, Home points at the next
 * Visayas level rather than scanning back to Luzon. Same SharedPreferences
 * pattern as [UnlockManager] / [CompletedManager]; it holds only a region name,
 * never progress — progress stays keyed to the global level id elsewhere.
 */
object LastPlayedStore {
    private const val PREFS = "last_played"
    // Guests (null / non-positive id) share one slot, same as the other stores.
    private fun key(userId: Int?) =
        if (userId != null && userId > 0) "region_$userId" else "region_guest"

    /** Record the region of [levelId] as the one the player is on. */
    fun setLevel(context: Context, userId: Int?, levelId: Int) {
        val region = runCatching { LevelProvider.forLevel(levelId).region.name }.getOrNull() ?: return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(userId), region)
            .apply()
    }

    /** The region last played, or null if the player has not started one yet. */
    fun region(context: Context, userId: Int?): Region? {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(userId), null) ?: return null
        return runCatching { Region.valueOf(name) }.getOrNull()
    }
}
