package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.repository.PointsWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Custodial KKCoin for in-round help. XP is never debited here.
 *
 * Reads and writes the app-wide [KkBalance] rather than keeping a copy of its
 * own. Each round used to start its own copy at 0, so the dock showed 0 KK
 * until a fetch landed - and kept showing it whenever that one fetch failed.
 */
class RemoteKkWallet : PointsWallet {

    override fun balance(): Flow<Int> = KkBalance.kk.filterNotNull().map { it.toInt() }

    override suspend fun refresh() {
        KkBalance.refresh()
    }

    override suspend fun spend(powerUp: PowerUp): Boolean {
        if (powerUp.spendKey.isEmpty() || powerUp.coinCost <= 0) return false
        val ok = runCatching {
            ChainQueue.serialized {
                val resp = KusinaApi.spendKk(powerUp.spendKey)
                val txRef = resp.tx_ref ?: return@serialized false
                val settled = KusinaApi.settleReward(txRef)
                settled?.status == "confirmed" || settled?.phase == 4
            }
        }.getOrDefault(false)
        refresh()
        return ok
    }
}
