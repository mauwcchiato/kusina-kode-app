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
    const val TOUR_HOME = "home_v5"

    /** First-visit tours of each main screen, mirroring [TOUR_HOME]: each one
     *  fires the first time a cook opens that screen and spotlights what matters
     *  there. Bump a suffix if that screen's spotlight layout changes. */
    const val TOUR_EXPLORE = "explore_v4"
    const val TOUR_LEARN = "learn_v1"
    const val TOUR_WALLET = "wallet_v2"
    const val TOUR_PROFILE = "profile_v1"

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
