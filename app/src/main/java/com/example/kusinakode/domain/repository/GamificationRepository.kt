package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

/**
 * Stores the player's computed gamification state. Local today; a remote
 * implementation can back it with /api/badges.php and the progress endpoint
 * without the UI changing.
 */
interface GamificationRepository {
    fun progress(userId: Int?): Flow<PlayerProgress>
    fun badges(userId: Int?): Flow<List<Badge>>

    suspend fun saveProgress(userId: Int?, progress: PlayerProgress)
    suspend fun awardBadges(userId: Int?, badges: List<Badge>)

    /**
     * Attaches a TxHash once Module 3 confirms the award on-chain, flipping
     * the badge from pending to blockchain-verified.
     */
    suspend fun markBadgeConfirmed(userId: Int?, badgeId: String, txHash: String)

    /**
     * One-time backfill of dishes cleared before the points system existed.
     *
     * Without this the gamification store starts blank, so re-solving an old
     * dish looks like a first solve and pays out again. Seeded levels count
     * toward milestones but award no points — the rounds that earned them
     * happened before there were points to earn.
     */
    suspend fun ensureSeeded(userId: Int?, solvedLevels: Set<Int>)

    /**
     * Replaces this device's copy with the account's server-side state, so
     * signing in on a new phone restores the real totals instead of
     * recomputing a local guess. Also retires the seeding bootstrap, which
     * only exists for accounts the server has never recorded.
     */
    suspend fun replaceFromServer(userId: Int?, progress: PlayerProgress, badges: List<Badge>)

    /** Deducts points for a power-up; false when the player can't afford it. */
    suspend fun spendPoints(userId: Int?, amount: Int): Boolean
}
