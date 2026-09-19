package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.BadgeType
import com.example.kusinakode.domain.model.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Module 2 acceptance criterion: "Badge awarding correctly triggers exactly
 * at each milestone condition (no early/late/duplicate awards)."
 */
class BadgeRulesTest {

    private fun idsFor(progress: PlayerProgress, held: Set<String> = emptySet()) =
        BadgeRules.newlyEarned(progress, held, userId = 7).map { it.badgeId }

    @Test
    fun `nothing is awarded before the first milestone`() {
        assertTrue(idsFor(PlayerProgress()).isEmpty())
    }

    @Test
    fun `round badge fires exactly on the threshold, not before`() {
        assertTrue("rounds_5" !in idsFor(PlayerProgress(roundsCompleted = 4)))
        assertTrue("rounds_5" in idsFor(PlayerProgress(roundsCompleted = 5)))
    }

    @Test
    fun `passing a milestone unnoticed still awards it later`() {
        // Jumping straight to 12 rounds must not skip the 1, 5 and 10 badges.
        val ids = idsFor(PlayerProgress(roundsCompleted = 12))
        assertTrue(ids.containsAll(listOf("rounds_1", "rounds_5", "rounds_10")))
        assertTrue("rounds_20" !in ids)
    }

    @Test
    fun `a badge already held is never awarded twice`() {
        val held = setOf("rounds_1", "rounds_5")
        val ids = idsFor(PlayerProgress(roundsCompleted = 5), held)
        assertTrue(ids.isEmpty())
    }

    @Test
    fun `streak badges follow the best streak, not the current one`() {
        // Streak was broken, but the achievement stands.
        val progress = PlayerProgress(currentStreak = 0, bestStreak = 3)
        assertTrue("streak_3" in idsFor(progress))
    }

    @Test
    fun `perfect badges track first-guess solves`() {
        assertTrue("perfect_1" in idsFor(PlayerProgress(perfectRounds = 1)))
        assertTrue("perfect_5" !in idsFor(PlayerProgress(perfectRounds = 4)))
        assertTrue("perfect_5" in idsFor(PlayerProgress(perfectRounds = 5)))
    }

    @Test
    fun `every badge family has at least one badge in the catalog`() {
        // The invariant, not a frozen list: declaring a BadgeType with no
        // badge in it is the bug worth catching, and adding a family should
        // not require editing this test.
        val types = BadgeRules.catalog().map { it.first }.toSet()
        assertEquals(BadgeType.values().toSet(), types)
    }

    @Test
    fun `the three families the manuscript names are all present`() {
        val types = BadgeRules.catalog().map { it.first }.toSet()
        assertTrue(BadgeType.ROUNDS in types)
        assertTrue(BadgeType.STREAK in types)
        assertTrue(BadgeType.PERFECT in types)
    }

    @Test
    fun `catalog ids are unique`() {
        val ids = BadgeRules.catalog().map { it.third }
        assertEquals(ids.size, ids.toSet().size)
    }
}
