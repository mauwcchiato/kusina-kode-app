package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.gamification.LeaderboardRules
import com.example.kusinakode.domain.model.LeaderboardBoard
import com.example.kusinakode.domain.model.LeaderboardRow
import com.example.kusinakode.domain.model.LeaderboardStanding
import com.example.kusinakode.domain.repository.LeaderboardRepository
import com.example.kusinakode.domain.repository.LeaderboardWindow

class RemoteLeaderboardRepository : LeaderboardRepository {

    override suspend fun topPlayers(
        limit: Int,
        window: LeaderboardWindow
    ): Result<List<LeaderboardRow>> = board(limit, window, userId = null).map { it.rows }

    override suspend fun board(
        limit: Int,
        window: LeaderboardWindow,
        userId: Int?
    ): Result<LeaderboardBoard> = runCatching {
        val resp = KusinaApi.getLeaderboard(limit, window.apiValue, me = userId)
        // Ordering lives in LeaderboardRules so it can be tested directly. A
        // windowed board ranks on what was cooked in the window, not on the
        // lifetime total the all-time board uses.
        val ranked: (List<LeaderboardRow>) -> List<LeaderboardRow> =
            if (window == LeaderboardWindow.AllTime) LeaderboardRules::rank
            else LeaderboardRules::rankBySolves
        LeaderboardBoard(
            rows = ranked(
                (resp.data ?: emptyList()).map {
                    LeaderboardRow(
                        name = it.name,
                        correctCount = it.correct_count,
                        points = it.total_points,
                        frameId = it.frame_id?.takeIf { id -> id.isNotBlank() },
                        characterId = it.character_id?.takeIf { id -> id.isNotBlank() }
                    )
                }
            ),
            // The server ranks the player with the same ordering as the board,
            // so this is their real place even when it is far below the top 10.
            me = resp.me?.let {
                LeaderboardStanding(
                    rank = it.rank,
                    row = LeaderboardRow(
                        name = it.name,
                        correctCount = it.correct_count,
                        points = it.total_points,
                        frameId = it.frame_id?.takeIf { id -> id.isNotBlank() },
                        characterId = it.character_id?.takeIf { id -> id.isNotBlank() }
                    )
                )
            }
        )
    }.mapNetworkError()
}
