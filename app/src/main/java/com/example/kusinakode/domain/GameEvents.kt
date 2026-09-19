package com.example.kusinakode.domain

import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.RoundCompleted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-wide stream of finished rounds. Modules 2 (gamification) and 3
 * (blockchain rewards) subscribe here instead of reaching into engine code.
 */
object GameEvents {
    private val _roundCompletions = MutableSharedFlow<RoundCompleted>(extraBufferCapacity = 16)
    val roundCompletions: SharedFlow<RoundCompleted> = _roundCompletions

    /**
     * A dish's KODEX entry was opened. Separate from rounds because reading
     * about a dish is its own kind of progress - the knowledge badges are
     * earned by studying, not by playing.
     */
    private val _dishesRead = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val dishesRead: SharedFlow<Int> = _dishesRead

    fun publish(event: RoundCompleted) {
        _roundCompletions.tryEmit(event)
    }

    fun publishDishRead(levelId: Int) {
        _dishesRead.tryEmit(levelId)
    }
}

/**
 * What the gamification layer awarded for a finished round: the points
 * credited and any badges unlocked.
 */
data class RoundScored(
    val levelId: Int,
    val pointsAwarded: Int,
    val badges: List<Badge>,
    /** True when the dish was already solved, so the round scored nothing. */
    val wasReplay: Boolean
)

/**
 * Milestones the gamification layer has just granted. Module 2 announces
 * badges here rather than writing them silently, so Module 3 can pick each
 * one up and fire the reward pipeline that produces its TxHash.
 */
object GamificationEvents {
    private val _badgesEarned = MutableSharedFlow<Badge>(extraBufferCapacity = 16)
    val badgesEarned: SharedFlow<Badge> = _badgesEarned

    private val _roundsScored = MutableSharedFlow<RoundScored>(
        replay = 1,
        extraBufferCapacity = 8
    )

    /** Replays its last value so a screen appearing after the round still sees it. */
    val roundsScored: SharedFlow<RoundScored> = _roundsScored

    fun publish(badge: Badge) {
        _badgesEarned.tryEmit(badge)
    }

    fun publish(scored: RoundScored) {
        _roundsScored.tryEmit(scored)
    }
}
