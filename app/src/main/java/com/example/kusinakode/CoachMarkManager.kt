package com.example.kusinakode

import android.content.Context

/**
 * Remembers which guided walkthroughs an account has already seen.
 *
 * Unlike [OnboardingManager], which runs once per *device* before anyone has
 * logged in, coach marks explain a logged-in player's own dashboard — streak,
 * badges, leaderboard tier. So completion is stored per user id: a second
 * account on the same phone still gets shown around.
 */
object CoachMarkManager {
    private const val PREFS_NAME = "kusinakode_prefs"

    /** The first-login tour of the Home dashboard. Bumped when the
     *  spotlight layout changed so cooks who saw the old, mis-aimed
     *  version still get the corrected walkthrough. */
    const val TOUR_HOME = "home_v3"

    private fun key(tour: String, userId: Int?): String =
        "coach_${tour}_${userId ?: 0}"

    fun isDone(context: Context, tour: String, userId: Int? = Session.userId): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(tour, userId), false)

    fun markDone(context: Context, tour: String, userId: Int? = Session.userId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(tour, userId), true)
            .apply()
    }

    /** Lets a player replay a tour from the help screen. */
    fun reset(context: Context, tour: String, userId: Int? = Session.userId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key(tour, userId))
            .apply()
    }
}
