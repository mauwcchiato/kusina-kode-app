package com.example.kusinakode.domain.model

data class ResetCodeResult(
    val message: String,
    /** Only present on debug backends that echo the code instead of emailing it. */
    val devCode: String?
)
