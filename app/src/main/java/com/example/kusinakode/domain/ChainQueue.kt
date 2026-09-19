package com.example.kusinakode.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialises everything this device sends into the reward pipeline.
 *
 * The backend builds a block's index and prev_hash in Phase 2 but appends it
 * in Phase 3 — a separate request. Two rewards in flight at once therefore
 * both claim the same index, and the Phase 3 integrity check rejects the
 * chain with "Chain failed to sync after append" (defect D-31).
 *
 * Winning a dish can easily fire three rewards at the same instant: the round
 * itself plus any badges it unlocks. Queueing them keeps this client to one
 * chain write at a time.
 *
 * This is a client-side mitigation, NOT the fix. It does nothing about two
 * phones playing at once, which is exactly what the stress-test pass (G-07)
 * will do. The append has to become atomic server-side.
 */
object ChainQueue {
    private val mutex = Mutex()

    suspend fun <T> serialized(block: suspend () -> T): T = mutex.withLock { block() }
}
