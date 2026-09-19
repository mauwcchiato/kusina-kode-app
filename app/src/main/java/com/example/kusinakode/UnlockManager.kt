package com.example.kusinakode

import android.content.Context

/**
 * STRICT progressive unlock manager.
 *
 * Stores ONLY the highest level unlocked (1..MAX_LEVEL) in SharedPreferences.
 * Default is 1 (only level 1 is unlocked).
 */
object UnlockManager {
    private const val PREFS_NAME = "kusinakode_prefs"
    private const val KEY_HIGHEST_UNLOCKED_LEGACY = "unlock_level" // legacy (global)
    private const val KEY_MIGRATED_V1_BUG = "unlock_migrated_v1_all_unlocked_bug"
    private const val KEY_LAST_USER_ID = "unlock_last_user_id"
    /** Follows the catalog so adding dishes doesn't strand progress at 20. */
    private val MAX_LEVEL: Int get() = LevelProvider.levelCount

    private fun keyForUser(userId: Int): String = "unlock_level_user_$userId"

    fun getUnlockedLevel(context: Context, userId: Int?): Int {
        // If not logged in, keep local progression at the strict default.
        if (userId == null || userId <= 0) return 1

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Migrate legacy global value ONLY for the same last logged-in user.
        val userKey = keyForUser(userId)
        val hasUserKey = prefs.contains(userKey)
        if (!hasUserKey) {
            val lastUser = prefs.getInt(KEY_LAST_USER_ID, -1)
            if (lastUser == userId) {
                val legacy = prefs.getInt(KEY_HIGHEST_UNLOCKED_LEGACY, 1)
                prefs.edit().putInt(userKey, legacy).apply()
            } else {
                // New user on this device: start from default 1 (do not inherit someone else's progress)
                prefs.edit().putInt(userKey, 1).apply()
            }
        }

        // One-time fix: earlier buggy builds may have saved MAX_LEVEL (20) for everyone.
        // Apply per-user: if stored value is exactly MAX_LEVEL and we haven't migrated yet, reset to 1.
        val migrated = prefs.getBoolean(KEY_MIGRATED_V1_BUG, false)
        val raw = prefs.getInt(userKey, 1)
        if (!migrated && raw == MAX_LEVEL) {
            prefs.edit()
                .putInt(userKey, 1)
                .putBoolean(KEY_MIGRATED_V1_BUG, true)
                .apply()
            return 1
        }

        // Track last user for future migrations.
        prefs.edit().putInt(KEY_LAST_USER_ID, userId).apply()
        return raw.coerceIn(1, MAX_LEVEL)
    }

    /**
     * Sets highest unlocked after merging with server (or restoring session). Does not validate sequence.
     */
    fun setUnlockedLevelFromServerMerge(context: Context, userId: Int, level: Int) {
        if (userId <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clamped = level.coerceIn(1, MAX_LEVEL)
        prefs.edit()
            .putInt(keyForUser(userId), clamped)
            .putInt(KEY_LAST_USER_ID, userId)
            .apply()
    }

    /**
     * Unlock ONLY the next level (N+1), and only if the player completed the current highest unlocked N.
     */
    fun unlockNextLevel(context: Context, userId: Int?, currentLevel: Int): Int {
        val highest = getUnlockedLevel(context, userId)
        if (currentLevel != highest) return highest
        if (highest >= MAX_LEVEL) return highest

        val newHighest = (highest + 1).coerceAtMost(MAX_LEVEL)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = userId ?: return highest
        prefs.edit().putInt(keyForUser(uid), newHighest).apply()
        return newHighest
    }
}
