package com.example.kusinakode

import android.content.Context

/**
 * The answer the player gave to each dish's trivia, stored per user on-device
 * so a reopened dish shows the card already answered instead of asking again.
 * Same SharedPreferences pattern as [FavoritesManager].
 *
 * The option text is stored rather than its position: the options are
 * shuffled from the dish dataset, so a position could point at a different
 * ingredient after the dataset changes. An answer that no longer matches any
 * option simply reads as unanswered.
 */
object TriviaAnswersManager {
    private const val PREFS_NAME = "kusinakode_prefs"

    private fun key(userId: Int?, levelId: Int): String {
        val who = if (userId != null && userId > 0) "user_$userId" else "guest"
        return "trivia_answer_${who}_$levelId"
    }

    fun answer(context: Context, userId: Int?, levelId: Int): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(userId, levelId), null)

    fun save(context: Context, userId: Int?, levelId: Int, answer: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key(userId, levelId), answer)
            .apply()
    }
}
