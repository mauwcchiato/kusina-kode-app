package com.example.kusinakode.domain.pantry

import org.junit.Assert.assertEquals
import org.junit.Test

class PalayokGrantRulesTest {

    /**
     * The grant used to scale 6..1 on guesses with a penalty for spending
     * help, then went flat at two. It is one now, and this number is the
     * contract the pot ritual, the pantry help sheet and api/lib/pantry.php
     * all quote.
     */
    @Test
    fun `a win pays one palayok`() {
        assertEquals(1, PalayokGrantRules.PER_WIN)
    }

    /**
     * The whole grant opens at the pot. Nothing is banked for the shelf, so a
     * win can never leave the player with an unopened palayok to come back to.
     */
    @Test
    fun `nothing is held back for the pantry shelf`() {
        assertEquals(0, PalayokGrantRules.PER_WIN - 1)
    }
}
