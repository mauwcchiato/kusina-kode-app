package com.example.kusinakode.data.repository

import android.content.Context
import android.util.Log
import com.example.kusinakode.Session
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.ChainQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Wins the server has not heard about yet, kept on the phone until it has.
 *
 * A winning attempt is the server's proof that a dish was solved: the dish's
 * KK and its palayok both wait on it. [RemoteAttemptRepository] retries a win a
 * few times, but a phone offline for longer than that, or closed mid-send,
 * used to lose the win for good. A win that still fails is parked here and
 * sent again the next time anything is recorded or Home opens, until the
 * server answers. Once it is on record, the server's pantry catch-up pays its
 * palayok on the next pantry load.
 *
 * Only wins are kept; a wrong guess is not worth holding on to.
 */
class AttemptOutbox(context: Context) {

    @Serializable
    data class PendingWin(
        val userId: Int,
        val levelId: Int,
        val guess: String,
        val timeTakenMs: Long
    )

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun add(win: PendingWin) = synchronized(lock) {
        val all = read()
        // One entry per player and dish is enough: the server only needs the
        // first win to know it was solved.
        if (all.none { it.userId == win.userId && it.levelId == win.levelId }) {
            write(all + win)
        }
    }

    fun pending(): List<PendingWin> = synchronized(lock) { read() }

    /** The server has answered for [win]; it no longer needs keeping. */
    fun settled(win: PendingWin) = synchronized(lock) {
        write(read().filterNot { it.userId == win.userId && it.levelId == win.levelId })
    }

    /**
     * Sends this player's parked wins. Anything the server answers, success or
     * a definite no, leaves the outbox; a network failure leaves it for next
     * time. Safe to call often: an empty outbox costs nothing, and only one
     * flush runs at a time.
     */
    suspend fun flush() {
        val userId = Session.userId ?: return
        flushing.withLock {
            for (win in pending().filter { it.userId == userId }) {
                try {
                    val resp = KusinaApi.postAttempt(
                        userId = win.userId,
                        levelId = win.levelId,
                        guess = win.guess,
                        correct = true,
                        timeTakenMs = win.timeTakenMs
                    )
                    if (resp.status == "success") {
                        resp.tx_ref?.let { ref ->
                            ChainQueue.serialized { KusinaApi.settleReward(ref) }
                            KkBalance.refresh()
                        }
                    } else {
                        Log.w(TAG, "parked win (level=${win.levelId}) rejected: ${resp.message}")
                    }
                    settled(win)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Still offline: stop here and keep the rest for next time.
                    Log.w(TAG, "parked win (level=${win.levelId}) still not sent: ${e.localizedMessage}")
                    return
                }
            }
        }
    }

    private fun read(): List<PendingWin> =
        prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString<List<PendingWin>>(it) }.getOrNull() }
            ?: emptyList()

    private fun write(list: List<PendingWin>) {
        prefs.edit().putString(KEY, json.encodeToString(list)).apply()
    }

    private companion object {
        const val TAG = "AttemptOutbox"
        const val PREFS = "kusinakode_attempt_outbox"
        const val KEY = "pending_wins"
        val json = Json { ignoreUnknownKeys = true }
        val lock = Any()
        val flushing = Mutex()
    }
}
