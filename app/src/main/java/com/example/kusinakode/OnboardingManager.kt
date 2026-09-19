package com.example.kusinakode

import android.content.Context

/**
 * Remembers whether this device has ever seen the feature onboarding.
 * Cold start still uses it; a new account on a returning device gets the
 * same screens again on the Create Account path (see AppNavigator).
 */
object OnboardingManager {
    private const val PREFS_NAME = "kusinakode_prefs"
    private const val KEY_DONE = "onboarding_done_v2"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DONE, true)
            .apply()
    }
}
