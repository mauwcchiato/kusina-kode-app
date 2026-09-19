package com.example.kusinakode.data.repository

import android.content.Context
import com.example.kusinakode.CompletedManager
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.Session
import com.example.kusinakode.UnlockManager
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.repository.UnlockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Merges server unlock rows with local [UnlockManager]/[CompletedManager],
 * uploading local progress when this device is ahead of the server.
 * (Replaces the old ProgressSync object.)
 */
class DefaultUnlockRepository(context: Context) : UnlockRepository {

    private val appContext = context.applicationContext

    override fun localHighestUnlocked(userId: Int?): Int =
        UnlockManager.getUnlockedLevel(appContext, userId)

    override suspend fun syncFromServer(userId: Int): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val resp = KusinaApi.getUnlocks(userId)
            merge(userId, resp.data ?: emptyList())
        }
    }.mapNetworkError()

    private suspend fun merge(userId: Int, serverLevelIds: List<Int>): Int {
        val maxLevel = LevelProvider.levelCount
        val serverMax = serverLevelIds.maxOrNull() ?: 1
        val localMax = UnlockManager.getUnlockedLevel(appContext, userId)
        val merged = maxOf(serverMax, localMax).coerceIn(1, maxLevel)

        UnlockManager.setUnlockedLevelFromServerMerge(appContext, userId, merged)

        val inferredCompleted = (merged - 1).coerceAtLeast(0)
        val localCompleted = CompletedManager.getHighestCompleted(appContext, userId)
        val mergedCompleted = maxOf(localCompleted, inferredCompleted).coerceIn(0, maxLevel)
        CompletedManager.setHighestCompletedFromServerMerge(appContext, userId, mergedCompleted)

        // Device ahead of server: upload the missing unlock rows.
        if (localMax > serverMax) {
            for (level in (serverMax + 1)..localMax) {
                try {
                    KusinaApi.postUnlock(userId, level)
                } catch (_: Exception) {
                }
            }
        }
        return merged
    }
}
