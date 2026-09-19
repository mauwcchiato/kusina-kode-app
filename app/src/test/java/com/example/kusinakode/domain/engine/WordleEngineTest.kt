package com.example.kusinakode.domain.engine

import com.example.kusinakode.domain.engine.WordleEngine.GuessValidation
import com.example.kusinakode.domain.model.TileState.Correct
import com.example.kusinakode.domain.model.TileState.SemiCorrect
import com.example.kusinakode.domain.model.TileState.Wrong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Manuscript unit-test targets for the Word Puzzle Game Engine:
 * letter input validation and color-coded feedback logic, for any word length.
 */
class WordleEngineTest {

    // ---- Letter input validation ----

    @Test
    fun `incomplete guess is rejected for any word length`() {
        assertEquals(GuessValidation.Incomplete, WordleEngine.validate("ADO  ", 5))
        assertEquals(GuessValidation.Incomplete, WordleEngine.validate("ADOB", 5))
        assertEquals(GuessValidation.Incomplete, WordleEngine.validate("SINIGAN ", 8))
        assertEquals(GuessValidation.Incomplete, WordleEngine.validate("", 6))
    }

    @Test
    fun `non-letter characters are rejected`() {
        assertEquals(GuessValidation.NotLetters, WordleEngine.validate("ADOB1", 5))
        assertEquals(GuessValidation.NotLetters, WordleEngine.validate("Pak-et", 6))
    }

    @Test
    fun `complete alphabetic guess is valid at 5, 6 and 8 letters`() {
        assertEquals(GuessValidation.Valid, WordleEngine.validate("ADOBO", 5))
        assertEquals(GuessValidation.Valid, WordleEngine.validate("PAKBET", 6))
        assertEquals(GuessValidation.Valid, WordleEngine.validate("SINIGANG", 8))
    }

    // ---- Attempts: a flat six rows for every dish ----

    @Test
    fun `every word length gets six attempts`() {
        listOf(4, 5, 6, 7, 8, 9, 12).forEach { length ->
            assertEquals(
                "word length $length should still allow 6 tries",
                6,
                WordleEngine.attemptsFor(length)
            )
        }
    }

    // ---- Color-coded feedback ----

    @Test
    fun `all letters correct when guess equals answer, case-insensitive`() {
        val verdict = WordleEngine.evaluate("adobo", "ADOBO")
        assertEquals(listOf(Correct, Correct, Correct, Correct, Correct), verdict)
        assertTrue(WordleEngine.isWinningVerdict(verdict))
    }

    @Test
    fun `mixed verdict classifies correct, misplaced and absent letters`() {
        // ADOBO vs BOMBA: B exact at index 3; O and A present elsewhere; B(0) and M absent.
        val verdict = WordleEngine.evaluate("ADOBO", "BOMBA")
        assertEquals(listOf(Wrong, SemiCorrect, Wrong, Correct, SemiCorrect), verdict)
        assertFalse(WordleEngine.isWinningVerdict(verdict))
    }

    @Test
    fun `repeated guess letter is not flagged more times than it appears in answer`() {
        // BULALO has L at 2 and 4. Guessing all Ls must yield Correct only there,
        // never SemiCorrect for the extra Ls (the old WordleLogic got this wrong).
        val verdict = WordleEngine.evaluate("BULALO", "LLLLLL")
        assertEquals(listOf(Wrong, Wrong, Correct, Wrong, Correct, Wrong), verdict)
    }

    @Test
    fun `misplaced repeated letters consume answer occurrences left to right`() {
        // ADOBO has two Os. Guess OOAAA: neither O is in the right spot, both
        // answer Os are free, so exactly two SemiCorrect Os.
        val verdict = WordleEngine.evaluate("ADOBO", "OOAAA")
        assertEquals(SemiCorrect, verdict[0])
        assertEquals(SemiCorrect, verdict[1])
        // One A is genuinely present (index 0 of answer) — only one flagged.
        assertEquals(1, verdict.drop(2).count { it == SemiCorrect })
    }

    @Test
    fun `evaluate works for 8-letter words`() {
        val verdict = WordleEngine.evaluate("SINIGANG", "SINIGANG")
        assertEquals(8, verdict.size)
        assertTrue(WordleEngine.isWinningVerdict(verdict))
    }

    @Test
    fun `evaluate rejects mismatched guess length`() {
        assertThrows(IllegalArgumentException::class.java) {
            WordleEngine.evaluate("ADOBO", "PAKBET")
        }
    }
}
