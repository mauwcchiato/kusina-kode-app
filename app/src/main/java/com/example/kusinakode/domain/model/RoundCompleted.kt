package com.example.kusinakode.domain.model

/**
 * Emitted when a round ends (won or out of attempts). Gamification (Module 2)
 * and Blockchain (Module 3) consume this — the game engine reports the outcome
 * and nothing about points or rewards.
 */
data class RoundCompleted(
    val levelId: Int,
    val finalGuess: String,
    val isCorrect: Boolean,
    val timeTakenMs: Long,
    val attemptsUsed: Int,
    /** Rows the board offered, so scoring can reward unused attempts. */
    val attemptsAllowed: Int,
    /** The dish's region, so exploration can be credited without a lookup. */
    val region: String = "",
    /** True if any guess ever showed a yellow (right letter, wrong place). */
    val usedPresentTile: Boolean = false,
    /** Power-ups spent during the round - hints, bombs, Instant Solve. */
    val powerUpsUsed: Int = 0,
    /** Instant Solve ended the round, so the board was not really beaten. */
    val wasInstantSolve: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
