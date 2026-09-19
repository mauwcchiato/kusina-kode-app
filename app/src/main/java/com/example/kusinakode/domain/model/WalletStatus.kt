package com.example.kusinakode.domain.model

data class WalletStatus(
    val balanceKk: Long,
    /** False until the blockchain backend (Module 3) is live. */
    val isLive: Boolean
)
