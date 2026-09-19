package com.example.kusinakode.data.repository

import com.example.kusinakode.domain.gamification.GamificationCoordinator
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.repository.GamificationRepository
import com.example.kusinakode.domain.repository.PointsWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Legacy XP wallet. Power-ups must not use this — KKCoin only
 * ([RemoteKkWallet]). Kept so old call sites still compile.
 */
class GamificationPointsWallet(
    private val repository: GamificationRepository,
    private val coordinator: GamificationCoordinator,
    private val userId: () -> Int?
) : PointsWallet {

    override fun balance(): Flow<Int> =
        repository.progress(userId()).map { it.totalPoints }

    override suspend fun spend(powerUp: PowerUp): Boolean = false

    override suspend fun refresh() {}
}
