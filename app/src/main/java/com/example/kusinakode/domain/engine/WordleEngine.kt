package com.example.kusinakode.domain.engine

import com.example.kusinakode.domain.model.TileState

/**
 * Pure puzzle logic — no Android, network, or level-data dependencies, so it
 * unit-tests in isolation and works for any word length (adobo=5, pakbet=6,
 * sinigang=8).
 */
object WordleEngine {

    /**
     * Attempt rows per round — six for every dish, whatever its length.
     *
     * The master doc floated scaling this with word length (wordLength + 1);
     * the team settled on a flat six so the challenge reads the same on every
     * level. Kept as a function so revisiting that is a one-line change.
     */
    const val MAX_ATTEMPTS = 6

    fun attemptsFor(@Suppress("UNUSED_PARAMETER") wordLength: Int): Int = MAX_ATTEMPTS

    sealed interface GuessValidation {
        data object Valid : GuessValidation
        /** Not every cell filled in yet. */
        data object Incomplete : GuessValidation
        data class WrongLength(val expected: Int, val actual: Int) : GuessValidation
        data object NotLetters : GuessValidation
    }

    fun validate(guess: String, wordLength: Int): GuessValidation = when {
        guess.contains(' ') || guess.length < wordLength -> GuessValidation.Incomplete
        guess.length != wordLength -> GuessValidation.WrongLength(wordLength, guess.length)
        !guess.all { it.isLetter() } -> GuessValidation.NotLetters
        else -> GuessValidation.Valid
    }

    /**
     * Classifies each guess letter as Correct / SemiCorrect / Wrong.
     * Two-pass with a remaining-letter count so repeated letters are never
     * flagged more times than they occur in the answer.
     */
    fun evaluate(answer: String, guess: String): List<TileState> {
        val ans = answer.uppercase()
        val g = guess.uppercase()
        require(g.length == ans.length) {
            "Guess length ${g.length} does not match answer length ${ans.length}"
        }

        val result = MutableList(ans.length) { TileState.Wrong }
        val remaining = mutableMapOf<Char, Int>()

        for (i in ans.indices) {
            if (g[i] == ans[i]) {
                result[i] = TileState.Correct
            } else {
                remaining[ans[i]] = (remaining[ans[i]] ?: 0) + 1
            }
        }
        for (i in ans.indices) {
            if (result[i] == TileState.Correct) continue
            val left = remaining[g[i]] ?: 0
            if (left > 0) {
                result[i] = TileState.SemiCorrect
                remaining[g[i]] = left - 1
            }
        }
        return result
    }

    fun isWinningVerdict(verdict: List<TileState>): Boolean =
        verdict.isNotEmpty() && verdict.all { it == TileState.Correct }
}
