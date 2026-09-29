package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The player's KK balance, one copy for the whole app.
 *
 * Home, the round, the wallet, the shop and the pantry each used to fetch
 * their own copy once, at different moments, and never heard about the
 * others: a win's KK settled after the next level had already read the old
 * figure, and the round's copy started at 0 and stayed there whenever its
 * single fetch failed. Now every screen watches this one value, and anything
 * that learns a newer figure (a balance fetch, a pantry reply, a purchase)
 * publishes it here.
 *
 * A failed fetch never writes: the last known balance stays on screen rather
 * than turning into 0. Null means no figure yet for this account.
 */
object KkBalance {

    private val _kk = MutableStateFlow<Long?>(null)
    val kk: StateFlow<Long?> = _kk.asStateFlow()

    fun publish(balanceKk: Long) {
        _kk.value = balanceKk
    }

    /** Asks the server; returns the fresh balance, or null if it could not. */
    suspend fun refresh(): Long? {
        val fresh = runCatching { KusinaApi.getWalletBalance().data?.balance_kk }.getOrNull()
        if (fresh != null) publish(fresh)
        return fresh
    }

    /** Signing out must not leave the next account reading this number. */
    fun clear() {
        _kk.value = null
    }
}
