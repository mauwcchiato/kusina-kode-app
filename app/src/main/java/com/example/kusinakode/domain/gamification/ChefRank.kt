package com.example.kusinakode.domain.gamification

/**
 * The player's kitchen rank, earned from dishes solved across every island.
 *
 * It replaces the old global "LVL", which counted levels across the whole game
 * and stopped meaning anything once each island got its own Level 1, 2, 3.
 * The steps follow the round badges in [BadgeRules] (Line Cook at 5, Sous Chef
 * at 10, Head Chef at 20), so a rank and its badge always arrive together.
 * Kusina Master is every dish, whatever the admin panel has added.
 */
object ChefRank {

    data class Rank(
        val title: String,
        /** Dishes solved where this rank starts. */
        val from: Int,
        /** The next rank up, or null at the top. */
        val next: Rank? = null
    )

    private val ladder = listOf(
        "Kusinero" to 0,
        "Line Cook" to 5,
        "Sous Chef" to 10,
        "Head Chef" to 20
    )

    const val MASTER = "Kusina Master"

    /**
     * Every rank a player can reach in a game of [total] dishes, lowest first,
     * ending at Kusina Master. The same steps [forSolved] climbs.
     */
    fun ladder(total: Int): List<Rank> =
        ladder.filter { (_, from) -> total <= 0 || from < total }.map { (t, f) -> Rank(t, f) } +
            (if (total > 0) listOf(Rank(MASTER, total)) else emptyList())

    /** The rank for [solved] dishes out of [total] in the game. */
    fun forSolved(solved: Int, total: Int): Rank {
        val all = total > 0 && solved >= total
        if (all) return Rank(MASTER, total)
        // A threshold at or past the whole game would be unreachable before
        // Kusina Master, so it is not a step on a short catalogue.
        val steps = ladder.filter { (_, from) -> total <= 0 || from < total }
        val idx = steps.indexOfLast { (_, from) -> solved >= from }.coerceAtLeast(0)
        val (title, from) = steps[idx]
        val next = steps.getOrNull(idx + 1)?.let { (t, f) -> Rank(t, f) }
            ?: if (total > 0) Rank(MASTER, total) else null
        return Rank(title, from, next)
    }
}
