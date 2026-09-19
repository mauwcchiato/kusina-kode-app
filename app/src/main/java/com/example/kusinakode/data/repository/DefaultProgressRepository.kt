package com.example.kusinakode.data.repository

import android.content.Context
import android.util.Log
import com.example.kusinakode.CompletedManager
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.Session
import com.example.kusinakode.UnlockManager
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.repository.ProgressRepository

class DefaultProgressRepository(context: Context) : ProgressRepository {

    private val appContext = context.applicationContext

    override suspend fun onLevelCompleted(levelId: Int) {
        val userId = Session.userId
        CompletedManager.markCompleted(appContext, userId, levelId)
        UnlockManager.unlockNextLevel(appContext, userId, levelId)

        if (userId != null && levelId < LevelProvider.levelCount) {
            try {
                KusinaApi.postUnlock(userId, levelId + 1)
            } catch (e: Exception) {
                // Local unlock already succeeded; ProgressSync reconciles later.
                Log.e(TAG, "postUnlock(level=${levelId + 1}) failed: ${e.localizedMessage}", e)
            }
        }
    }

    private companion object {
        const val TAG = "DefaultProgressRepo"
    }
}
