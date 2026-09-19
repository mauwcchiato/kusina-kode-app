package com.example.kusinakode.data.repository

import android.util.Log
import com.example.kusinakode.Session
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.repository.AttemptRepository

class RemoteAttemptRepository : AttemptRepository {

    override suspend fun recordAttempt(
        levelId: Int,
        guess: String,
        isCorrect: Boolean,
        timeTakenMs: Long
    ) {
        // Guests have no server-side record; gameplay continues regardless.
        val userId = Session.userId ?: return
        try {
            val resp = KusinaApi.postAttempt(
                userId = userId,
                levelId = levelId,
                guess = guess,
                correct = isCorrect,
                timeTakenMs = timeTakenMs
            )
            if (resp.status != "success") {
                Log.w(TAG, "postAttempt(level=$levelId) rejected: ${resp.message}")
            } else {
                // Serialised: a winning attempt and the badges it unlocks
                // otherwise append to the chain at the same instant (D-31).
                resp.tx_ref?.let { ref -> ChainQueue.serialized { KusinaApi.settleReward(ref) } }
            }
        } catch (e: Exception) {
            Log.e(TAG, "postAttempt(level=$levelId) failed: ${e.localizedMessage}", e)
        }
    }

    private companion object {
        const val TAG = "RemoteAttemptRepo"
    }
}
