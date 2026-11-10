package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.pantry.DrawResult
import com.example.kusinakode.domain.pantry.PantrySnapshot
import com.example.kusinakode.domain.pantry.SellResult

interface PantryRepository {
    suspend fun snapshot(): Result<Pair<PantrySnapshot, Long>>
    suspend fun grant(levelId: Int, powerUpsUsed: Int = 0): Result<Int>
    /** [hintLevelId] asks for an ingredient from that dish, as a hint at it. */
    suspend fun draw(levelId: Int?, hintLevelId: Int? = null): Result<Triple<DrawResult, PantrySnapshot, Long>>
    /** Spends a banked wheel spin. Returns palayoks won and the new snapshot. */
    suspend fun spin(): Result<Pair<Int, PantrySnapshot>>

    /**
     * Buys one extra spin with KK and banks it. The price is the server's.
     *
     * Returns the snapshot and the balance left after the charge, so the
     * caller can see both the spin land and the KK go before turning the
     * wheel with [spin].
     */
    suspend fun buySpin(): Result<Pair<PantrySnapshot, Long>>
    suspend fun sell(ingredientId: String, qty: Int): Result<Triple<SellResult, PantrySnapshot, Long>>

    /**
     * Reports the first trivia answer on [levelId]'s dish. The server banks a
     * spin only when that first answer is [correct].
     */
    suspend fun triviaSpin(levelId: Int, answer: String, correct: Boolean): Result<TriviaSpinOutcome>
}

sealed interface TriviaSpinOutcome {
    /** A spin was banked; [snapshot] already counts it. */
    data class Granted(val snapshot: PantrySnapshot, val balanceKk: Long) : TriviaSpinOutcome
    /** This dish's trivia was answered before, so nothing is paid now. */
    data object AlreadyAnswered : TriviaSpinOutcome
    /** The answer was wrong. */
    data object NotGranted : TriviaSpinOutcome
    /** The server does not offer trivia spins yet. */
    data object NotAvailable : TriviaSpinOutcome
}
