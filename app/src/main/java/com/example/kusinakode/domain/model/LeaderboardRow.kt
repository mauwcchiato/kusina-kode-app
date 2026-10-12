package com.example.kusinakode.domain.model

data class LeaderboardRow(
    val name: String,
    val correctCount: Int,
    /** Accumulated gamification points — the primary ranking measure. */
    val points: Int = 0,
    val frameId: String? = null,
    val characterId: String? = null
)

/** Where the signed-in player sits on a board, even when it is below the top 10. */
data class LeaderboardStanding(val rank: Int, val row: LeaderboardRow)

/** One board as shown: the top rows, plus the player's own place on it. */
data class LeaderboardBoard(
    val rows: List<LeaderboardRow>,
    /** Null when the player is not on this board (e.g. nothing cooked today). */
    val me: LeaderboardStanding? = null
)
