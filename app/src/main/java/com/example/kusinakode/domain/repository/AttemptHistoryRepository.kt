package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.AttemptRecord

interface AttemptHistoryRepository {
    suspend fun recentAttempts(userId: Int, limit: Int = 30): Result<List<AttemptRecord>>
}
