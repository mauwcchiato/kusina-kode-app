package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.LeaderboardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Module 2 acceptance criterion: "Leaderboard ranking is accurate and
 * updates correctly after new attempts."
 */
class LeaderboardRulesTest {

    private fun row(name: String, points: Int, solved: Int = 0) =
        LeaderboardRow(name = name, correctCount = solved, points = points)

    @Test
    fun `players are ordered by points, highest first`() {
        val ranked = LeaderboardRules.rank(
            listOf(row("Ana", 300), row("Ben", 900), row("Cy", 600))
        )
        assertEquals(listOf("Ben", "Cy", "Ana"), ranked.map { it.name })
    }

    @Test
    fun `dishes solved break a points tie`() {
        val ranked = LeaderboardRules.rank(
            listOf(row("Ana", 500, solved = 3), row("Ben", 500, solved = 7))
        )
        assertEquals(listOf("Ben", "Ana"), ranked.map { it.name })
    }

    @Test
    fun `name breaks a full tie so the order never shuffles`() {
        val rows = listOf(row("Zoe", 400, 2), row("Ana", 400, 2), row("Mia", 400, 2))
        val first = LeaderboardRules.rank(rows).map { it.name }
        val again = LeaderboardRules.rank(rows.reversed()).map { it.name }

        assertEquals(listOf("Ana", "Mia", "Zoe"), first)
        assertEquals("ranking must be stable regardless of input order", first, again)
    }

    @Test
    fun `players yet to score still appear, at the bottom`() {
        val ranked = LeaderboardRules.rank(
            listOf(row("Ana", 0), row("Ben", 120))
        )
        assertEquals(listOf("Ben", "Ana"), ranked.map { it.name })
    }

    @Test
    fun `earning points moves a player up the board`() {
        val before = listOf(row("Ana", 100), row("Ben", 500), row("Cy", 900))
        assertEquals(3, LeaderboardRules.positionOf(before, "Ana"))

        // Ana solves a few dishes.
        val after = listOf(row("Ana", 1000), row("Ben", 500), row("Cy", 900))
        assertEquals(1, LeaderboardRules.positionOf(after, "Ana"))
    }

    @Test
    fun `position lookup ignores case and reports absence`() {
        val rows = listOf(row("Chef Ian", 700), row("Ben", 200))
        assertEquals(1, LeaderboardRules.positionOf(rows, "chef ian"))
        assertNull(LeaderboardRules.positionOf(rows, "Nobody"))
    }

    @Test
    fun `an empty board ranks to nothing rather than failing`() {
        assertEquals(emptyList<LeaderboardRow>(), LeaderboardRules.rank(emptyList()))
    }
}
