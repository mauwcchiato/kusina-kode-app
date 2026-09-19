package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.gamification.PowerUp
import kotlinx.coroutines.flow.Flow

/**
 * Lets the game screen spend KKCoin on power-ups without knowing how
 * the custodial wallet is stored. XP/points are never the shop currency.
 */
interface PointsWallet {
    fun balance(): Flow<Int>

    /** Debits [powerUp] in KK if affordable; returns false when short. */
    suspend fun spend(powerUp: PowerUp): Boolean

    suspend fun refresh()
}
