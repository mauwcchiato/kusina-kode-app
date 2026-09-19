package com.example.kusinakode.domain.model

data class LeaderboardRow(
    val name: String,
    val correctCount: Int,
    /** Accumulated gamification points — the primary ranking measure. */
    val points: Int = 0,
    val frameId: String? = null,
    val characterId: String? = null
)
