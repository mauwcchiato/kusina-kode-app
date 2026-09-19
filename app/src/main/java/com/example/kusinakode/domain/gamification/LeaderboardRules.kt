package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.LeaderboardRow

/**
 * Ranking for the Kusina Masters board (Module 2, element iii).
 *
 * Pure so the ordering can be tested without a server: the manuscript's
 * acceptance criterion is that ranking is accurate and updates correctly as
 * players earn, and that's a property of this comparison, not of the query.
 */
object LeaderboardRules {

    /**
     * Highest points first. Ties break on dishes solved, then on name so the
     * order is stable rather than arbitrary between refreshes.
     */
    fun rank(rows: List<LeaderboardRow>): List<LeaderboardRow> =
        rows.sortedWith(
            compareByDescending<LeaderboardRow> { it.points }
                .thenByDescending { it.correctCount }
                .thenBy { it.name.lowercase() }
        )

    /**
     * Ranking for a time-windowed board, where the measure is dishes solved
     * inside the window rather than lifetime points.
     *
     * Points still break ties — between two cooks who each solved three dishes
     * today, the more accomplished one sits higher — but they cannot outrank
     * someone who simply cooked more in the period.
     */
    fun rankBySolves(rows: List<LeaderboardRow>): List<LeaderboardRow> =
        rows.sortedWith(
            compareByDescending<LeaderboardRow> { it.correctCount }
                .thenByDescending { it.points }
                .thenBy { it.name.lowercase() }
        )

    /** 1-based position of [name], or null when they aren't on the board. */
    fun positionOf(rows: List<LeaderboardRow>, name: String): Int? =
        rank(rows).indexOfFirst { it.name.equals(name, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?.plus(1)
}
