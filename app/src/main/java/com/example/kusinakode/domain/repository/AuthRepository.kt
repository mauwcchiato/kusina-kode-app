package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.ResetCodeResult
import com.example.kusinakode.domain.model.UserSession

interface AuthRepository {
    suspend fun login(identifier: String, password: String): Result<UserSession>

    /** Registers and immediately logs the new account in. */
    suspend fun register(
        name: String,
        nickname: String?,
        email: String,
        password: String
    ): Result<UserSession>

    suspend fun requestPasswordReset(email: String): Result<ResetCodeResult>

    /** Returns the confirmation message on success. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<String>
}
