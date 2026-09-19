package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.repository.PointsWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Custodial KKCoin for in-round help. XP is never debited here.
 */
class RemoteKkWallet : PointsWallet {

    private val _balance = MutableStateFlow(0)

    override fun balance(): Flow<Int> = _balance.asStateFlow()

    override suspend fun refresh() {
        runCatching {
            val data = KusinaApi.getWalletBalance().data
            _balance.value = (data?.balance_kk ?: 0L).toInt()
        }
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
