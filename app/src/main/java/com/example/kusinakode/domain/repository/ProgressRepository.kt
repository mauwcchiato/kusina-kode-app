package com.example.kusinakode.domain.repository

interface ProgressRepository {
    /** Marks [levelId] completed and unlocks the next level, locally and on the backend. */
    suspend fun onLevelCompleted(levelId: Int)
}
