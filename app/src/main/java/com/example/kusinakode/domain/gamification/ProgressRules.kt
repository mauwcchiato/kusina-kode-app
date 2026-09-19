package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.model.RoundCompleted

/**
 * Folds a finished round into a player's running totals (Module 2, elements
 * i and iv). Pure, so the same sequence of rounds always produces the same
 * progress — the "consistent across sessions" acceptance criterion.
 */
object ProgressRules {

    /**
     * Returns [current] advanced by [event].
     *
     * - A loss resets the streak and scores nothing.
     * - A first-time solve scores points and advances every counter.
     * - Re-solving a dish already cleared scores nothing and does not inflate
     *   the round count, but leaves the streak intact — replays are practice,
     *   not a points farm.
     */
    fun advance(current: PlayerProgress, event: RoundCompleted): PlayerProgress {
        if (!event.isCorrect) return current.copy(currentStreak = 0)

        if (event.levelId in current.solvedLevels) return current

        val points = ScoreRules.pointsFor(
            attemptsUsed = event.attemptsUsed,
            attemptsAllowed = event.attemptsAllowed,
            timeTakenMs = event.timeTakenMs
        )
        val streak = current.currentStreak + 1

        // Instant Solve fills the answer in for you, so it cannot satisfy an
        // achievement about how the board was beaten.
        val beatenHonestly = !event.wasInstantSolve
        val seaOfGreen = beatenHonestly &&
            !event.usedPresentTile &&
            // A first-guess win has no yellow by definition; that is already
            // One-Shot Wonder, so it must not also pay out here.
            event.attemptsUsed >= 2
        val clean = beatenHonestly && event.powerUpsUsed == 0
        val lastPlating = beatenHonestly && event.attemptsUsed == event.attemptsAllowed
        val fast = beatenHonestly && event.timeTakenMs in 1 until BadgeRules.FAST_ROUND_MS

        return current.copy(
            totalPoints = current.totalPoints + points,
            roundsCompleted = current.roundsCompleted + 1,
            currentStreak = streak,
            bestStreak = maxOf(current.bestStreak, streak),
            perfectRounds = current.perfectRounds +
                if (ScoreRules.isPerfect(event.attemptsUsed)) 1 else 0,
            solvedLevels = current.solvedLevels + event.levelId,
            seaOfGreenRounds = current.seaOfGreenRounds + if (seaOfGreen) 1 else 0,
            cleanRounds = current.cleanRounds + if (clean) 1 else 0,
            lastPlatings = current.lastPlatings + if (lastPlating) 1 else 0,
            fastRounds = current.fastRounds + if (fast) 1 else 0,
            regionsSolved = if (event.region.isBlank()) current.regionsSolved
            else current.regionsSolved + event.region
        )
    }

    /** Records that the player opened a dish's KODEX entry. */
    fun markDishRead(current: PlayerProgress, levelId: Int): PlayerProgress =
        if (levelId in current.dishesRead) current
        else current.copy(dishesRead = current.dishesRead + levelId)
}
