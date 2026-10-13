package com.example.kusinakode.ui.pantry

import android.app.Application
import android.content.Context

/**
 * The last ingredients that came out of a palayok, newest first.
 * The KODEX pantry row shows these three, not the sample photos.
 */
object RecentPotDraws {
    private const val PREFS = "recent_pot_draws"
    private const val KEY = "ids"
    private var app: Application? = null

    fun install(application: Application) {
        app = application
    }

    fun note(id: String) {
        val prefs = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        val older = prefs.getString(KEY, "").orEmpty()
            .split(',')
            .filter { it.isNotBlank() && it != id }
        prefs.edit().putString(KEY, (listOf(id) + older).take(3).joinToString(",")).apply()
    }

    fun ids(): List<String> {
        val prefs = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return emptyList()
        return prefs.getString(KEY, "").orEmpty().split(',').filter { it.isNotBlank() }
    }
}
