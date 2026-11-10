package com.example.kusinakode.ui.pantry

import android.app.Application
import android.content.Context

/**
 * The hint palayok's ingredient, kept for the dish it hints at.
 *
 * The palayok opened right after a win draws from the NEXT dish's ingredients.
 * Shown once on the card, the hint was gone by the time that dish was played,
 * so it is kept here, per account and per level, and the game screen deals it
 * as a card beside the power-ups. It is cleared once that dish is solved.
 * Kept on the phone only: nothing about it needs the server.
 */
object NextDishHints {
    private const val PREFS = "next_dish_hints"
    private var app: Application? = null

    fun install(application: Application) {
        app = application
    }

    private fun key(userId: Int, levelId: Int) = "u${userId}_L$levelId"

    fun save(userId: Int?, levelId: Int?, ingredientId: String?) {
        if (userId == null || levelId == null || ingredientId.isNullOrBlank()) return
        val prefs = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        prefs.edit().putString(key(userId, levelId), ingredientId).apply()
    }

    fun forLevel(userId: Int?, levelId: Int): String? {
        if (userId == null) return null
        val prefs = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return null
        return prefs.getString(key(userId, levelId), null)?.takeIf { it.isNotBlank() }
    }

    fun clear(userId: Int?, levelId: Int) {
        if (userId == null) return
        val prefs = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        prefs.edit().remove(key(userId, levelId)).apply()
    }
}
