package com.example.kusinakode.domain.repository

/**
 * Persists every guess (correct or not) per the shared Attempt contract:
 * Attempt { attempt_id, user_id, level_id, is_correct, time_taken_ms, timestamp }
 */
interface AttemptRepository {
    suspend fun recordAttempt(levelId: Int, guess: String, isCorrect: Boolean, timeTakenMs: Long)
}
