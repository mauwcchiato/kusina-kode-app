package com.example.kusinakode

import android.content.Context

/**
 * Tracks actual completion (solved levels), per user, on-device.
 *
 * Important: unlocking != completion.
 * - Completing level N should happen ONLY after a correct answer for level N.
 * - This is sequential, so we store only the highest completed level.
 */
object CompletedManager {
    private const val PREFS_NAME = "kusinakode_prefs"
    private const val KEY_LAST_USER_ID = "completed_last_user_id"
    private const val KEY_MIGRATED_V1 = "completed_migrated_v1"
    /** Follows the catalog so adding dishes doesn't strand progress at 20. */
    private val MAX_LEVEL: Int get() = LevelProvider.levelCount

    private fun keyForUser(userId: Int): String = "completed_level_user_$userId"

    fun getHighestCompleted(context: Context, userId: Int?): Int {
        if (userId == null || userId <= 0) return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val userKey = keyForUser(userId)

        // One-time migration: if we don't have a completed value yet for this user,
        // infer it conservatively from unlocked progress:
        // if highestUnlocked == K, user has definitely completed up to K-1.
        // (Level 20 completion cannot be inferred because unlocking stops at 20.)
        val migrated = prefs.getBoolean(KEY_MIGRATED_V1, false)
        if (!migrated && !prefs.contains(userKey)) {
            val highestUnlocked = UnlockManager.getUnlockedLevel(context, userId)
            val inferredCompleted = (highestUnlocked - 1).coerceIn(0, MAX_LEVEL)
            prefs.edit()
                .putInt(userKey, inferredCompleted)
                .putBoolean(KEY_MIGRATED_V1, true)
                .putInt(KEY_LAST_USER_ID, userId)
                .apply()
            return inferredCompleted
        }

        prefs.edit().putInt(KEY_LAST_USER_ID, userId).apply()
        return prefs.getInt(userKey, 0).coerceIn(0, MAX_LEVEL)
    }

    /**
     * Mark a level as completed, but only sequentially:
     * can only complete (highestCompleted + 1).
     */
    fun markCompleted(context: Context, userId: Int?, level: Int): Int {
        if (userId == null || userId <= 0) return 0
        if (level !in 1..MAX_LEVEL) return getHighestCompleted(context, userId)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userKey = keyForUser(userId)
        val highestCompleted = prefs.getInt(userKey, 0).coerceIn(0, MAX_LEVEL)

        // Strict sequential completion
        if (level != highestCompleted + 1) return highestCompleted

        prefs.edit()
            .putInt(userKey, level)
            .putInt(KEY_LAST_USER_ID, userId)
            .apply()

        return level
    }

    /**
     * Updates highest completed after merging with server (e.g. inferred from unlock progress).
     */
    fun setHighestCompletedFromServerMerge(context: Context, userId: Int, level: Int) {
        if (userId <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clamped = level.coerceIn(0, MAX_LEVEL)
        prefs.edit()
            .putInt(keyForUser(userId), clamped)
            .putInt(KEY_LAST_USER_ID, userId)
            .apply()
    }
}

