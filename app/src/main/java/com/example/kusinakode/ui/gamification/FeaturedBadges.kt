package com.example.kusinakode.ui.gamification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which badges the player has chosen to show on their profile.
 *
 * Per account rather than per device: the badges are the account's, so the
 * shelf should look the same wherever they sign in. Stored as an ordered list
 * of badge ids, so the player controls the order as well as the choice.
 *
 * An empty selection is not "show nothing" - it means the player has never
 * chosen, and the profile falls back to its default fill. That distinction
 * matters, otherwise clearing the picker would blank the shelf.
 */
object FeaturedBadges {

    private const val PREFS = "kusinakode_prefs"
    private const val SEPARATOR = ","

    /** The shelf on the profile holds this many. */
    const val SLOTS = 4

    private val _featured = MutableStateFlow<List<String>>(emptyList())
    val featured: StateFlow<List<String>> = _featured.asStateFlow()

    private fun key(userId: Int?) = "featured_badges_${userId ?: "guest"}"

    /** Reads the stored choice into the flow. Call when the profile opens. */
    fun load(context: Context, userId: Int?) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(userId), null)
        _featured.value = raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
    }

    /**
     * Stores [badgeIds] as the player's showcase, keeping their order and
     * trimming anything past [SLOTS].
     */
    fun set(context: Context, userId: Int?, badgeIds: List<String>) {
        val trimmed = badgeIds.distinct().take(SLOTS)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(userId), trimmed.joinToString(SEPARATOR))
            .apply()
        _featured.value = trimmed
    }

    /**
     * The badges to show, given everything the player holds.
     *
     * Chosen badges come first, in the player's order. A chosen badge that is
     * somehow no longer earned is dropped rather than shown as locked, and any
     * spare slot is filled the default way - earned first, then locked, so a
     * new player still sees something to aim at.
     */
    fun <T> resolve(
        all: List<T>,
        chosenIds: List<String>,
        idOf: (T) -> String,
        isEarned: (T) -> Boolean
    ): List<T> {
        val byId = all.associateBy(idOf)
        // distinct() here as well as in [set]: a stored value can predate that
        // guard, and a repeated badge on the shelf looks like a bug.
        val chosen = chosenIds.distinct().mapNotNull { byId[it] }.filter(isEarned)
        if (chosen.size >= SLOTS) return chosen.take(SLOTS)

        val used = chosen.map(idOf).toSet()
        val filler = all.filter { idOf(it) !in used }
            .sortedByDescending { isEarned(it) }
        return (chosen + filler).take(SLOTS)
    }
}
