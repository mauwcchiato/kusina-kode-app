package com.example.kusinakode.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerUpRulesTest {

    @Test
    fun `bomb never clears a letter that is in the answer`() {
        val answer = "ADOBO"
        val blasted = PowerUpRules.lettersToBomb(answer, alreadyEliminated = emptySet())

        answer.forEach { letter ->
            assertTrue("$letter is in the answer and must survive", letter !in blasted)
        }
    }

    @Test
    fun `bomb leaves the agreed number of decoys standing`() {
        val answer = "ADOBO"
        val blasted = PowerUpRules.lettersToBomb(answer, alreadyEliminated = emptySet())

        val wrongLetters = ('A'..'Z').filter { it !in answer.toSet() }
        val survivors = wrongLetters.count { it !in blasted }

        assertEquals(PowerUpRules.DECOYS_LEFT_AFTER_BOMB, survivors)
    }

    @Test
    fun `letters already ruled out are not counted again`() {
        val answer = "ADOBO"
        val known = setOf('X', 'Y', 'Z')
        val blasted = PowerUpRules.lettersToBomb(answer, alreadyEliminated = known)

        known.forEach { assertTrue("$it was already out", it !in blasted) }
    }

    @Test
    fun `a nearly exhausted keyboard bombs nothing rather than misbehaving`() {
        val answer = "ADOBO"
        // Everything wrong is already eliminated except three letters.
        val remaining = setOf('Q', 'W', 'E')
        val alreadyOut = ('A'..'Z').filter { it !in answer.toSet() && it !in remaining }.toSet()

        val blasted = PowerUpRules.lettersToBomb(answer, alreadyOut)

        assertTrue("fewer candidates than decoys should clear nothing", blasted.isEmpty())
    }

    @Test
    fun `reveal and bomb are KK-priced, never XP`() {
        assertEquals(0, PowerUp.REVEAL_LETTER.pointCost)
        assertEquals(0, PowerUp.BOMB.pointCost)
        assertEquals(20, PowerUp.REVEAL_LETTER.coinCost)
        assertEquals(40, PowerUp.BOMB.coinCost)
        assertEquals(100, PowerUp.INSTANT_SOLVE.coinCost)
        assertTrue(PowerUp.INSTANT_SOLVE.isCoinPriced)
        assertEquals(0, PowerUp.INSTANT_SOLVE.pointCost)
    }

    @Test
    fun `a second hint waits one minute`() {
        assertEquals(60_000L, PowerUpRules.HINT_COOLDOWN_MS)
        assertEquals(1, PowerUpRules.MAX_REVEALS_PER_ROUND)
        assertEquals(0L, PowerUpRules.cooldownRemainingMs(null, 1_000L))
        assertEquals(60_000L, PowerUpRules.cooldownRemainingMs(1_000L, 1_000L))
        assertEquals(1_000L, PowerUpRules.cooldownRemainingMs(1_000L, 60_000L))
        assertEquals(0L, PowerUpRules.cooldownRemainingMs(1_000L, 61_000L))
        assertEquals("1:00", PowerUpRules.formatCooldown(60_000L))
        assertEquals("0:01", PowerUpRules.formatCooldown(1L))
    }
}
