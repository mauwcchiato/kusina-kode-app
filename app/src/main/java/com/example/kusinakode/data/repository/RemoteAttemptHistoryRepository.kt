package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.model.AttemptRecord
import com.example.kusinakode.domain.repository.AttemptHistoryRepository

class RemoteAttemptHistoryRepository : AttemptHistoryRepository {

    override suspend fun recentAttempts(userId: Int, limit: Int): Result<List<AttemptRecord>> =
        runCatching {
            val resp = KusinaApi.getAttemptHistory(userId, limit)
            if (resp.status != "success") {
                throw IllegalStateException(resp.message ?: "Couldn't load your history.")
            }
            resp.data.orEmpty().map {
                AttemptRecord(
                    levelId = it.level_id,
                    levelName = it.level_name,
                    guess = it.guess_word,
                    wasCorrect = it.is_correct == 1,
                    timeTakenMs = it.time_taken_ms,
                    attemptedAt = it.attempted_at
                )
            }
        }.mapNetworkError()
}
