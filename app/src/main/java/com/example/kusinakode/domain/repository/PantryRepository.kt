package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.pantry.DrawResult
import com.example.kusinakode.domain.pantry.PantrySnapshot
import com.example.kusinakode.domain.pantry.SellResult

interface PantryRepository {
    suspend fun snapshot(): Result<Pair<PantrySnapshot, Long>>
    suspend fun grant(levelId: Int, powerUpsUsed: Int = 0): Result<Int>
    suspend fun draw(levelId: Int?): Result<Triple<DrawResult, PantrySnapshot, Long>>
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
}
