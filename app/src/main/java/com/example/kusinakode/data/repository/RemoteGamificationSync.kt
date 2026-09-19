package com.example.kusinakode.data.repository

import android.util.Log
import com.example.kusinakode.api.BadgeRequest
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.api.ProgressRequest
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.gamification.GamificationSync
import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.BadgeType
import com.example.kusinakode.domain.model.PlayerProgress

/**
 * Pushes computed totals and badge awards to the backend so the leaderboard
 * can rank by points and the admin dashboard can see awards.
 *
 * Best-effort: play continues offline and the local store stays the UI's
 * source, so a failed sync never blocks a round. The server's unique key on
 * (user_id, badge_id) means a retried award is harmless.
 */
class RemoteGamificationSync : GamificationSync {

    override suspend fun pushProgress(userId: Int, progress: PlayerProgress) {
        runCatching {
            val resp = KusinaApi.postProgress(
                ProgressRequest(
                    user_id = userId,
                    total_points = progress.totalPoints,
                    rounds_completed = progress.roundsCompleted,
                    current_streak = progress.currentStreak,
                    best_streak = progress.bestStreak,
                    perfect_rounds = progress.perfectRounds
                )
            )
            val refs = (listOfNotNull(resp.tx_ref) + resp.tx_refs).distinct()
            // One chain write at a time — see ChainQueue / D-31.
            refs.forEach { ref -> ChainQueue.serialized { KusinaApi.settleReward(ref) } }
            // Islands wait for a CLAIM tap on Rewards, like the daily. Auto-
            // minting them hid CLAIMED and skipped the inbox / system nudge.
        }.onFailure { Log.w(TAG, "progress sync failed: ${it.localizedMessage}") }
    }

    override suspend fun pushBadge(userId: Int, badge: Badge) {
        runCatching {
            KusinaApi.postBadge(
                BadgeRequest(
                    user_id = userId,
                    badge_id = badge.badgeId,
                    badge_type = badge.badgeType.name,
                    title = badge.title,
                    milestone_criteria = badge.milestoneCriteria
                )
            )
        }.onFailure { Log.w(TAG, "badge sync failed: ${it.localizedMessage}") }
    }

    override suspend fun fetchProgress(userId: Int): PlayerProgress? =
        runCatching {
            val resp = KusinaApi.getProgress(userId)
            resp.data?.let {
                PlayerProgress(
                    totalPoints = it.total_points,
                    roundsCompleted = it.rounds_completed,
                    currentStreak = it.current_streak,
                    bestStreak = it.best_streak,
                    perfectRounds = it.perfect_rounds,
                    solvedLevels = it.solved_levels.toSet()
                )
            }
        }.onFailure {
            Log.w(TAG, "progress fetch failed: ${it.localizedMessage}")
        }.getOrNull()

    override suspend fun fetchBadges(userId: Int): List<Badge> =
        runCatching {
            val resp = KusinaApi.getBadges(userId)
            resp.data.orEmpty().map {
                Badge(
                    badgeId = it.badge_id,
                    userId = it.user_id ?: userId,
                    badgeType = runCatching { BadgeType.valueOf(it.badge_type) }
                        .getOrDefault(BadgeType.ROUNDS),
                    title = it.title,
                    milestoneCriteria = it.milestone_criteria,
                    txHash = it.tx_hash,
                    awardedAt = it.awarded_at
                )
            }
        }.onFailure {
            Log.w(TAG, "badge fetch failed: ${it.localizedMessage}")
        }.getOrDefault(emptyList())

    private companion object {
        const val TAG = "GamificationSync"
    }
}
