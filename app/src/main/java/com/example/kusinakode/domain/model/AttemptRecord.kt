package com.example.kusinakode.domain.model

/** One past attempt, for the progress view's history list. */
data class AttemptRecord(
    val levelId: Int,
    val levelName: String,
    val guess: String,
    val wasCorrect: Boolean,
    val timeTakenMs: Long?,
    val attemptedAt: Long?
)
