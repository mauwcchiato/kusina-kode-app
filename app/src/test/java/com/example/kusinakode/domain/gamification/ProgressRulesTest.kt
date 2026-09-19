package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.model.RoundCompleted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {

    private fun round(
        levelId: Int = 1,
        correct: Boolean = true,
        attemptsUsed: Int = 3,
        attemptsAllowed: Int = 6,
        timeMs: Long = 200_000
    ) = RoundCompleted(
        levelId = levelId,
        finalGuess = "ADOBO",
        isCorrect = correct,
        timeTakenMs = timeMs,
        attemptsUsed = attemptsUsed,
        attemptsAllowed = attemptsAllowed
    )

    @Test
    fun `a win adds points and advances every counter`() {
        val after = ProgressRules.advance(PlayerProgress(), round())
        assertEquals(160, after.totalPoints) // base 100 + 3 spare rows
        assertEquals(1, after.roundsCompleted)
        assertEquals(1, after.currentStreak)
        assertEquals(1, after.bestStreak)
        assertTrue(1 in after.solvedLevels)
    }

    @Test
    fun `a loss resets the streak but keeps points and best streak`() {
        val won = ProgressRules.advance(PlayerProgress(), round(levelId = 1))
        val lost = ProgressRules.advance(won, round(levelId = 2, correct = false))

        assertEquals(0, lost.currentStreak)
        assertEquals(1, lost.bestStreak)
        assertEquals(won.totalPoints, lost.totalPoints)
        assertEquals(1, lost.roundsCompleted)
    }

    @Test
    fun `replaying a solved dish scores nothing`() {
        val first = ProgressRules.advance(PlayerProgress(), round(levelId = 3))
        val replay = ProgressRules.advance(first, round(levelId = 3, attemptsUsed = 1))

        assertEquals(first, replay)
    }

    @Test
    fun `points accumulate across separate rounds`() {
        var progress = PlayerProgress()
        progress = ProgressRules.advance(progress, round(levelId = 1, attemptsUsed = 6))
        progress = ProgressRules.advance(progress, round(levelId = 2, attemptsUsed = 6))

        assertEquals(200, progress.totalPoints)
        assertEquals(2, progress.roundsCompleted)
        assertEquals(2, progress.currentStreak)
    }

    @Test
    fun `perfect rounds are counted separately`() {
        val after = ProgressRules.advance(PlayerProgress(), round(attemptsUsed = 1))
        assertEquals(1, after.perfectRounds)
    }

    @Test
    fun `best streak survives a later break`() {
        var progress = PlayerProgress()
        (1..3).forEach { progress = ProgressRules.advance(progress, round(levelId = it)) }
        progress = ProgressRules.advance(progress, round(levelId = 9, correct = false))

        assertEquals(3, progress.bestStreak)
        assertEquals(0, progress.currentStreak)
    }
}
