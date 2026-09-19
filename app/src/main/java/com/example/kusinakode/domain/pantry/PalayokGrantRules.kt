package com.example.kusinakode.domain.pantry

/**
 * How many palayoks a win is worth. The server is the authority on the grant
 * itself; this is the same number so the ritual copy cannot drift.
 *
 * One per win, opened at the pot. Nothing is held back for the shelf any more:
 * the second palayok meant every win quietly added to a pile the player had to
 * go somewhere else to deal with, so the reward for solving a dish was partly
 * an errand. What the pot gives is now the whole of it.
 *
 * Before that it scaled 6..1 on guesses with a penalty for spending help,
 * which meant a hard dish beaten on the sixth try — the one that most needed a
 * reward — paid least.
 */
object PalayokGrantRules {

    /** Opened at the pot, and that is all of it. */
    const val PER_WIN = 1
}
