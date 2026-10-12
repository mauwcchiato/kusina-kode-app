package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.LeaderboardBoard
import com.example.kusinakode.domain.model.LeaderboardRow

/**
 * How much of history a board covers.
 *
 * [Today] and [Week] rank on dishes newly solved inside the window, not on
 * total points: points are stored as a running total with no history behind
 * them, so there is nothing to sum over a date range. Attempt timestamps are
 * real, so that is what the short windows count.
 */
enum class LeaderboardWindow(val apiValue: String) {
    Today("today"),
    Week("week"),
    AllTime("all")
}

interface LeaderboardRepository {
    /** Players ranked by correct answers (descending), ties broken by name. */
    suspend fun topPlayers(
        limit: Int = 10,
        window: LeaderboardWindow = LeaderboardWindow.AllTime
    ): Result<List<LeaderboardRow>>

    /**
     * The top rows plus [userId]'s own rank on the same board, so a player
     * outside the top 10 still sees where they stand. A source that cannot
     * rank one player falls back to the rows alone.
     */
    suspend fun board(
        limit: Int = 10,
        window: LeaderboardWindow = LeaderboardWindow.AllTime,
        userId: Int? = null
    ): Result<LeaderboardBoard> = topPlayers(limit, window).map { LeaderboardBoard(it) }
}
