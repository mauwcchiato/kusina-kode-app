package com.example.kusinakode.domain.repository

interface UnlockRepository {
    /**
     * Fetches server unlocks, merges with local progress (uploading local
     * levels the server is missing), and returns the merged highest unlocked.
     */
    suspend fun syncFromServer(userId: Int): Result<Int>

    /** Local highest unlocked level; works offline and for guests. */
    fun localHighestUnlocked(userId: Int?): Int
}
