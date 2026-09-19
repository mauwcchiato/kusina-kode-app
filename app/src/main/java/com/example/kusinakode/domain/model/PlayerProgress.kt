package com.example.kusinakode.domain.model

/**
 * Running gamification totals for one player (Module 2, element i and iv).
 *
 * These are *computed* values for fast display and badge eligibility — they are
 * not the authority on what a player has legitimately earned. Per the
 * manuscript, that answer comes from the chain via Module 3's audit endpoints.
 */
data class PlayerProgress(
    val totalPoints: Int = 0,
    val roundsCompleted: Int = 0,
    /** Consecutive wins; any loss resets it to zero. */
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    /** Rounds solved on the very first guess. */
    val perfectRounds: Int = 0,
    /** Levels already scored, so replaying a dish can't farm points. */
    val solvedLevels: Set<Int> = emptySet(),

    // ---- Achievement tallies -------------------------------------------
    // Each counts only first-time solves, for the same reason points do:
    // a replay is practice, not a second chance at a badge.

    /** Solves that never turned a tile yellow (and weren't one-guess wins). */
    val seaOfGreenRounds: Int = 0,
    /** Solves that spent no power-up at all. */
    val cleanRounds: Int = 0,
    /** Solves that landed on the very last available row. */
    val lastPlatings: Int = 0,
    /** Solves finished inside [com.example.kusinakode.domain.gamification.BadgeRules.FAST_ROUND_MS]. */
    val fastRounds: Int = 0,
    /** Region names with at least one dish solved. */
    val regionsSolved: Set<String> = emptySet(),
    /** Dishes whose KODEX entry the player has actually opened. */
    val dishesRead: Set<Int> = emptySet()
)
