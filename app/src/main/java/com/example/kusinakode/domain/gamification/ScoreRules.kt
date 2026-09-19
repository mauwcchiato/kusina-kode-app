package com.example.kusinakode.domain.gamification

/**
 * Points earned for solving a dish (Module 2, element i).
 *
 * Pure and deterministic so the same round always scores the same, which is
 * what "consistent across sessions" in the acceptance criteria means. Kept
 * bucketed rather than continuous so a score is easy to explain to a player
 * — and easy to re-derive if the chain ever needs to audit it.
 */
object ScoreRules {

    /** Awarded for solving the dish at all. */
    const val BASE_POINTS = 100

    /** Each unused attempt row is worth this much. */
    const val POINTS_PER_UNUSED_ATTEMPT = 20

    /** Solving on the very first guess. */
    const val PERFECT_BONUS = 50

    // Speed tiers, in seconds.
    const val BLAZING_SECONDS = 30
    const val QUICK_SECONDS = 60
    const val STEADY_SECONDS = 120

    const val BLAZING_BONUS = 60
    const val QUICK_BONUS = 40
    const val STEADY_BONUS = 20

    /** A round solved on the first guess. */
    fun isPerfect(attemptsUsed: Int): Boolean = attemptsUsed == 1

    fun speedBonus(timeTakenMs: Long): Int {
        val seconds = timeTakenMs / 1000
        return when {
            seconds < BLAZING_SECONDS -> BLAZING_BONUS
            seconds < QUICK_SECONDS -> QUICK_BONUS
            seconds < STEADY_SECONDS -> STEADY_BONUS
            else -> 0
        }
    }

    /**
     * Points for a solved round. Losing a round scores nothing — call this
     * only for wins.
     */
    fun pointsFor(attemptsUsed: Int, attemptsAllowed: Int, timeTakenMs: Long): Int {
        val unused = (attemptsAllowed - attemptsUsed).coerceAtLeast(0)
        val efficiency = unused * POINTS_PER_UNUSED_ATTEMPT
        val perfect = if (isPerfect(attemptsUsed)) PERFECT_BONUS else 0
        return BASE_POINTS + efficiency + perfect + speedBonus(timeTakenMs)
    }
}
