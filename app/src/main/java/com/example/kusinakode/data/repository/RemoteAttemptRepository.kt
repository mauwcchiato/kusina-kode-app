package com.example.kusinakode.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import android.util.Log
import com.example.kusinakode.Session
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.repository.AttemptRepository

/**
 * @param outbox where a win waits until the server has it. Null (as in tests)
 *   means a win that fails every retry is only logged, as before.
 */
class RemoteAttemptRepository(
    private val outbox: AttemptOutbox? = null
) : AttemptRepository {

    override suspend fun recordAttempt(
        levelId: Int,
        guess: String,
        isCorrect: Boolean,
        timeTakenMs: Long
    ) {
        // Guests have no server-side record; gameplay continues regardless.
        val userId = Session.userId ?: return
        // Anything left from an earlier offline win goes first.
        outbox?.flush()

        // A winning attempt is what the server reads as proof the dish was
        // solved: the palayok grant and the dish's KK both wait on it. It is
        // parked in the outbox before it is sent, so even the app closing
        // mid-send cannot lose it, and it leaves only once the server answers.
        // It is also retried a few times now, rather than waiting for the next
        // flush. A miss is sent once and not kept.
        val parked = if (isCorrect) {
            AttemptOutbox.PendingWin(userId, levelId, guess, timeTakenMs).also { outbox?.add(it) }
        } else null
        val tries = if (isCorrect) WIN_TRIES else 1
        for (attempt in 1..tries) {
            try {
                val resp = KusinaApi.postAttempt(
                    userId = userId,
                    levelId = levelId,
                    guess = guess,
                    correct = isCorrect,
                    timeTakenMs = timeTakenMs
                )
                if (resp.status != "success") {
                    // The server answered and said no; asking again won't change it.
                    Log.w(TAG, "postAttempt(level=$levelId) rejected: ${resp.message}")
                } else {
                    // Serialised: a winning attempt and the badges it unlocks
                    // otherwise append to the chain at the same instant (D-31).
                    resp.tx_ref?.let { ref ->
                        ChainQueue.serialized { KusinaApi.settleReward(ref) }
                        // The win's KK has landed: tell every screen showing
                        // the balance, so the next level opens with it.
                        KkBalance.refresh()
                    }
                }
                parked?.let { outbox?.settled(it) }
                return
            } catch (e: CancellationException) {
                // Leaving the screen: the win stays parked for the next flush.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "postAttempt(level=$levelId) try $attempt/$tries failed: ${e.localizedMessage}", e)
                if (attempt < tries) delay(RETRY_BACKOFF_MS * attempt)
            }
        }
        // Every try failed: the win stays parked and goes with the next flush.
    }

    private companion object {
        const val TAG = "RemoteAttemptRepo"
        const val WIN_TRIES = 3
        const val RETRY_BACKOFF_MS = 1_000L
    }
}
