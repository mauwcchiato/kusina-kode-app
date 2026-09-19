package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.GameEvents
import com.example.kusinakode.domain.GamificationEvents
import com.example.kusinakode.domain.RoundScored
import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.model.RoundCompleted
import com.example.kusinakode.domain.repository.GamificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The seam between Module 1 and Module 2: subscribes to finished rounds,
 * folds them into the player's progress, and announces any badge earned.
 *
 * The game engine knows nothing about this — it only reports that a round
 * ended. Nothing here writes reward records either; earning a badge raises
 * an event that Module 3 turns into an on-chain award.
 */
/** Moves computed totals between the device and the backend. */
interface GamificationSync {
    suspend fun pushProgress(userId: Int, progress: PlayerProgress)
    suspend fun pushBadge(userId: Int, badge: Badge)

    /** The account's server-side totals, or null if it has no record yet. */
    suspend fun fetchProgress(userId: Int): PlayerProgress?

    suspend fun fetchBadges(userId: Int): List<Badge>
}

class GamificationCoordinator(
    private val repository: GamificationRepository,
    private val currentUserId: () -> Int?,
    /** Levels already cleared under the pre-gamification build. */
    private val previouslySolved: () -> Set<Int> = { emptySet() },
    private val sync: GamificationSync? = null
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            GameEvents.roundCompletions.collect { event -> handle(event) }
        }
        scope.launch {
            GameEvents.dishesRead.collect { levelId -> handleDishRead(levelId) }
        }
    }

    /**
     * Brings the device in line with the account.
     *
     * The server is the source of truth: if it holds a record for this
     * player, it replaces whatever is on the device, so signing in anywhere
     * shows the same points, streak and badges. Only an account the server
     * has never seen falls back to bootstrapping from local progress —
     * which is what seeding was actually for.
     */
    suspend fun refresh() {
        val userId = currentUserId()

        val remoteSync = sync
        if (userId != null && remoteSync != null) {
            val remote = runCatching { remoteSync.fetchProgress(userId) }.getOrNull()
            if (remote != null) {
                val badges = runCatching { remoteSync.fetchBadges(userId) }.getOrDefault(emptyList())
                repository.replaceFromServer(userId, remote, badges)
                grantOwedBadges(userId)
                return
            }
        }

        // No server record: bootstrap from dishes cleared before points existed.
        val before = repository.progress(userId).first()
        repository.ensureSeeded(userId, previouslySolved())
        val after = repository.progress(userId).first()
        if (after != before) {
            userId?.let { sync?.pushProgress(it, after) }
        }
        grantOwedBadges(userId)
    }

    /**
     * Charges points for a power-up and pushes the new total.
     *
     * Spending has to go through here rather than straight to the repository:
     * [refresh] lets the server win, so a purchase the backend never heard
     * about is simply handed back the next time any screen syncs.
     */
    suspend fun spend(amount: Int): Boolean {
        val userId = currentUserId()
        if (!repository.spendPoints(userId, amount)) return false
        val after = repository.progress(userId).first()
        userId?.let { sync?.pushProgress(it, after) }
        return true
    }

    /** Visible for testing: applies one round without needing the event bus. */
    /**
     * Folds an opened KODEX entry into progress and awards any knowledge
     * badge it completes. Reading the same dish twice changes nothing.
     */
    suspend fun handleDishRead(levelId: Int) {
        val userId = currentUserId()
        val before = repository.progress(userId).first()
        val after = ProgressRules.markDishRead(before, levelId)
        if (after == before) return
        repository.saveProgress(userId, after)

        val alreadyEarned = repository.badges(userId).first().map { it.badgeId }.toSet()
        val newBadges = BadgeRules.newlyEarned(after, alreadyEarned, userId)
        if (newBadges.isNotEmpty()) {
            repository.awardBadges(userId, newBadges)
            newBadges.forEach { GamificationEvents.publish(it) }
        }
    }

    suspend fun handle(event: RoundCompleted) {
        val userId = currentUserId()
        // If CompletedManager already recorded THIS win, don't seed it as an
        // old dish — that made the first solve look like a replay and skipped KK.
        val prior = previouslySolved()
        val seed = if (event.isCorrect && event.levelId == prior.maxOrNull()) {
            prior - setOf(event.levelId)
        } else prior
        repository.ensureSeeded(userId, seed)

        val before: PlayerProgress = repository.progress(userId).first()
        val after = ProgressRules.advance(before, event)

        val pointsAwarded = after.totalPoints - before.totalPoints
        val wasReplay = event.isCorrect && event.levelId in before.solvedLevels

        if (after == before) {
            // Nothing changed (a replay, or a loss with no streak to reset):
            // still report the outcome so the win screen can explain itself.
            if (event.isCorrect) {
                GamificationEvents.publish(
                    RoundScored(event.levelId, 0, emptyList(), wasReplay)
                )
            }
            return
        }

        repository.saveProgress(userId, after)
        userId?.let { sync?.pushProgress(it, after) }

        val earned = grantOwedBadges(userId)

        if (event.isCorrect) {
            GamificationEvents.publish(
                RoundScored(event.levelId, pointsAwarded, earned, wasReplay)
            )
        }
    }

    /**
     * Awards every badge the player's current progress qualifies for but
     * hasn't been granted yet — whether it was earned this round or by
     * dishes solved before the points system existed.
     */
    private suspend fun grantOwedBadges(userId: Int?): List<Badge> {
        val progress = repository.progress(userId).first()
        val heldIds = repository.badges(userId).first().map { it.badgeId }.toSet()
        val earned = BadgeRules.newlyEarned(progress, heldIds, userId)
        if (earned.isEmpty()) return emptyList()

        repository.awardBadges(userId, earned)
        earned.forEach { badge ->
            userId?.let { sync?.pushBadge(it, badge) }
            // Announced, not silently stored — Module 3 listens for these.
            GamificationEvents.publish(badge)
        }
        return earned
    }
}
