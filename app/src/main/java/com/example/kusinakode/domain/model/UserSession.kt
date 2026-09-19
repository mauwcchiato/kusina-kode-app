package com.example.kusinakode.domain.model

data class UserSession(
    val userId: Int,
    val displayName: String,
    val nickname: String?,
    val email: String
)
