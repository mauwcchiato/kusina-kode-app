package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.WalletStatus

/**
 * Read-only custodial wallet access. Per the integration contract the mobile
 * app NEVER touches keys — it only reads balances/status via the backend.
 * Live impl: [com.example.kusinakode.data.repository.RemoteWalletRepository]
 * calling GET /api/wallet/balance.php with the bearer token.
 */
interface WalletRepository {
    suspend fun walletStatus(userId: Int?): Result<WalletStatus>
}
