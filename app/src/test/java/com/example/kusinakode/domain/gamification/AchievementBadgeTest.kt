package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.model.RoundCompleted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The seven achievement badges: what counts, and — more importantly — what
 * must not. Each one has a way to be cheated, and that is what is pinned here.
 */
class AchievementBadgeTest {

    private fun round(
        levelId: Int = 1,
        correct: Boolean = true,
        attemptsUsed: Int = 3,
        attemptsAllowed: Int = 6,
        timeMs: Long = 60_000,
        region: String = "LUZON",
        yellow: Boolean = false,
        powerUps: Int = 0,
        instantSolve: Boolean = false
    ) = RoundCompleted(
        levelId = levelId,
        finalGuess = "ADOBO",
        isCorrect = correct,
        timeTakenMs = timeMs,
        attemptsUsed = attemptsUsed,
        attemptsAllowed = attemptsAllowed,
        region = region,
        usedPresentTile = yellow,
        powerUpsUsed = powerUps,
        wasInstantSolve = instantSolve
    )

    private fun ids(progress: PlayerProgress): Set<String> =
        BadgeRules.newlyEarned(progress, emptySet(), userId = 1).map { it.badgeId }.toSet()

    // ---- Sea of Green --------------------------------------------------

    @Test
    fun `sea of green needs no yellow and more than one guess`() {
        val p = ProgressRules.advance(PlayerProgress(), round(attemptsUsed = 3, yellow = false))
        assertEquals(1, p.seaOfGreenRounds)
        assertTrue("sea_of_green" in ids(p))
    }

    @Test
    fun `a yellow tile anywhere rules out sea of green`() {
        val p = ProgressRules.advance(PlayerProgress(), round(yellow = true))
        assertEquals(0, p.seaOfGreenRounds)
        assertFalse("sea_of_green" in ids(p))
    }

    @Test
    fun `a one-guess win is not sea of green`() {
        // It has no yellow by definition, and is already One-Shot Wonder.
        val p = ProgressRules.advance(PlayerProgress(), round(attemptsUsed = 1, yellow = false))
        assertEquals(0, p.seaOfGreenRounds)
        assertTrue("perfect_1" in ids(p))
        assertFalse("sea_of_green" in ids(p))
    }

    @Test
    fun `instant solve cannot buy sea of green`() {
        val p = ProgressRules.advance(
            PlayerProgress(),
            round(attemptsUsed = 6, yellow = false, instantSolve = true, powerUps = 1)
        )
        assertEquals(0, p.seaOfGreenRounds)
    }

    // ---- Clean Kitchen -------------------------------------------------

    @Test
    fun `clean kitchen needs zero power-ups`() {
        assertEquals(1, ProgressRules.advance(PlayerProgress(), round(powerUps = 0)).cleanRounds)
        assertEquals(0, ProgressRules.advance(PlayerProgress(), round(powerUps = 1)).cleanRounds)
    }

    // ---- Last Plating --------------------------------------------------

    @Test
    fun `last plating is the final row only`() {
        val onLast = round(attemptsUsed = 6, attemptsAllowed = 6)
        val earlier = round(attemptsUsed = 5, attemptsAllowed = 6)
        assertEquals(1, ProgressRules.advance(PlayerProgress(), onLast).lastPlatings)
        assertEquals(0, ProgressRules.advance(PlayerProgress(), earlier).lastPlatings)
    }

    @Test
    fun `instant solve does not count as a last plating`() {
        // Instant Solve is scored as a last-row win, so without the guard it
        // would hand out this badge for free.
        val p = ProgressRules.advance(
            PlayerProgress(),
            round(attemptsUsed = 6, attemptsAllowed = 6, instantSolve = true)
        )
        assertEquals(0, p.lastPlatings)
    }

    // ---- Fast Thinker --------------------------------------------------

    @Test
    fun `fast thinker is under the threshold`() {
        assertEquals(1, ProgressRules.advance(PlayerProgress(), round(timeMs = 29_999)).fastRounds)
        assertEquals(0, ProgressRules.advance(PlayerProgress(), round(timeMs = 30_000)).fastRounds)
    }

    @Test
    fun `a zero duration does not count as fast`() {
        // A missing or unrecorded time must not read as an instant win.
        assertEquals(0, ProgressRules.advance(PlayerProgress(), round(timeMs = 0)).fastRounds)
    }

    // ---- World Explorer ------------------------------------------------

    @Test
    fun `world explorer needs every region`() {
        var p = PlayerProgress()
        listOf("LUZON", "VISAYAS", "MINDANAO").forEachIndexed { i, r ->
            p = ProgressRules.advance(p, round(levelId = i + 1, region = r))
        }
        assertFalse("world_explorer" in ids(p))

        p = ProgressRules.advance(p, round(levelId = 9, region = "PHILIPPINES"))
        assertEquals(BadgeRules.REGION_COUNT, p.regionsSolved.size)
        assertTrue("world_explorer" in ids(p))
    }

    @Test
    fun `solving twice in one region does not count twice`() {
        var p = ProgressRules.advance(PlayerProgress(), round(levelId = 1, region = "LUZON"))
        p = ProgressRules.advance(p, round(levelId = 2, region = "LUZON"))
        assertEquals(1, p.regionsSolved.size)
    }

    // ---- Knowledge -----------------------------------------------------

    @Test
    fun `knowledge badges count distinct dishes read`() {
        var p = PlayerProgress()
        repeat(BadgeRules.TRIVIA_DETECTIVE_READS) { p = ProgressRules.markDishRead(p, it + 1) }
        assertTrue("trivia_detective" in ids(p))
        assertFalse("knowledge_collector" in ids(p))

        repeat(BadgeRules.KNOWLEDGE_COLLECTOR_READS) { p = ProgressRules.markDishRead(p, it + 1) }
        assertTrue("knowledge_collector" in ids(p))
    }

    @Test
    fun `re-reading the same dish changes nothing`() {
        var p = ProgressRules.markDishRead(PlayerProgress(), 4)
        val once = p
        p = ProgressRules.markDishRead(p, 4)
        assertEquals(once, p)
        assertEquals(1, p.dishesRead.size)
    }

    // ---- Shared guarantees ---------------------------------------------

    @Test
    fun `a loss earns no achievement`() {
        val p = ProgressRules.advance(PlayerProgress(), round(correct = false, timeMs = 1_000))
        assertEquals(0, p.cleanRounds)
        assertEquals(0, p.fastRounds)
        assertTrue(p.regionsSolved.isEmpty())
    }

    @Test
    fun `replaying a solved dish cannot re-earn an achievement`() {
        val first = ProgressRules.advance(PlayerProgress(), round(levelId = 1, powerUps = 0))
        val replay = ProgressRules.advance(first, round(levelId = 1, powerUps = 0))
        assertEquals(first.cleanRounds, replay.cleanRounds)
    }

    @Test
    fun `achievements are awarded once`() {
        val p = ProgressRules.advance(PlayerProgress(), round(powerUps = 0))
        val held = ids(p)
        assertTrue("clean_kitchen" in held)
        val again = BadgeRules.newlyEarned(p, held, userId = 1)
        assertTrue(again.isEmpty())
    }
}
