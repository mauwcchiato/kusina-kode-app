package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.GoogleOutcome
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

    /**
     * Exchanges a verified Google ID token for a session, creating the
     * account on first use. One method for both routes because the server
     * makes no distinction either - sign-up and sign-in are the same call.
     */
    suspend fun signInWithGoogle(idToken: String, create: Boolean = false): Result<GoogleOutcome>

    suspend fun requestPasswordReset(email: String): Result<ResetCodeResult>

    /** Returns the confirmation message on success. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<String>
}
