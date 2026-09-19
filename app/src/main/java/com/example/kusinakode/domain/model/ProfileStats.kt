package com.example.kusinakode.domain.model

data class ProfileStats(
    val currentRank: Int?,
    val highestLevel: Int,
    val bestTime: String?,
    val name: String,
    val nickname: String?,
    val email: String
)
