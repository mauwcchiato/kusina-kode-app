package com.example.kusinakode.domain.repository

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
}
