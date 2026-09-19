package com.example.kusinakode.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Module 2 acceptance criterion: "Points calculation is correct and
 * consistent across sessions."
 */
class ScoreRulesTest {

    @Test
    fun `solving on the last attempt still scores the base award`() {
        // 6 of 6 rows used, slow finish: no efficiency, no speed, no perfect.
        assertEquals(100, ScoreRules.pointsFor(attemptsUsed = 6, attemptsAllowed = 6, timeTakenMs = 200_000))
    }

    @Test
    fun `unused attempts add efficiency points`() {
        // 4 of 6 used = 2 spare rows = 40 points on top of the base.
        assertEquals(140, ScoreRules.pointsFor(4, 6, 200_000))
    }

    @Test
    fun `first-guess solve earns the perfect bonus`() {
        assertTrue(ScoreRules.isPerfect(1))
        assertFalse(ScoreRules.isPerfect(2))
        // base 100 + 5 spare rows (100) + perfect 50, slow so no speed bonus.
        assertEquals(250, ScoreRules.pointsFor(1, 6, 200_000))
    }

    @Test
    fun `speed bonus follows its tiers`() {
        assertEquals(ScoreRules.BLAZING_BONUS, ScoreRules.speedBonus(10_000))
        assertEquals(ScoreRules.QUICK_BONUS, ScoreRules.speedBonus(45_000))
        assertEquals(ScoreRules.STEADY_BONUS, ScoreRules.speedBonus(90_000))
        assertEquals(0, ScoreRules.speedBonus(200_000))
    }

    @Test
    fun `tier boundaries are exclusive at the upper edge`() {
        assertEquals(ScoreRules.BLAZING_BONUS, ScoreRules.speedBonus(29_999))
        assertEquals(ScoreRules.QUICK_BONUS, ScoreRules.speedBonus(30_000))
        assertEquals(ScoreRules.STEADY_BONUS, ScoreRules.speedBonus(60_000))
        assertEquals(0, ScoreRules.speedBonus(120_000))
    }

    @Test
    fun `identical rounds always score identically`() {
        val first = ScoreRules.pointsFor(2, 7, 25_000)
        val again = ScoreRules.pointsFor(2, 7, 25_000)
        assertEquals(first, again)
        // base 100 + 5 spare (100) + blazing 60; not perfect.
        assertEquals(260, first)
    }

    @Test
    fun `overrunning the allowance never subtracts points`() {
        assertEquals(100, ScoreRules.pointsFor(attemptsUsed = 9, attemptsAllowed = 6, timeTakenMs = 200_000))
    }
}
