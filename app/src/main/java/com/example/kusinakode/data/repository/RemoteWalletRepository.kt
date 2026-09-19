package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.model.WalletStatus
import com.example.kusinakode.domain.repository.WalletRepository

/**
 * Read-only wallet. Keys stay on the server; this only calls
 * GET wallet/balance.php with the bearer token.
 */
class RemoteWalletRepository : WalletRepository {
    override suspend fun walletStatus(userId: Int?): Result<WalletStatus> = runCatching {
        val resp = KusinaApi.getWalletBalance()
        val data = resp.data ?: error(resp.message ?: "No wallet data")
        WalletStatus(
            balanceKk = data.balance_kk,
            isLive = data.is_live
        )
    }
}
