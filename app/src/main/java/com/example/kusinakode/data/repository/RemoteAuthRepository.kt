package com.example.kusinakode.data.repository

import com.example.kusinakode.Session
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.model.AppError
import com.example.kusinakode.domain.model.GoogleOutcome
import com.example.kusinakode.domain.model.ResetCodeResult
import com.example.kusinakode.domain.model.UserSession
import com.example.kusinakode.domain.repository.AuthRepository

class RemoteAuthRepository : AuthRepository {

    override suspend fun login(identifier: String, password: String): Result<UserSession> = runCatching {
        val resp = KusinaApi.login(identifier.trim(), password)
        if (resp.status == "success" && resp.user_id != null && resp.display_name != null) {
            val session = UserSession(
                userId = resp.user_id,
                displayName = resp.display_name,
                nickname = resp.nickname ?: resp.display_name,
                email = resp.email ?: identifier.trim()
            )
            // Set before anything else runs: every account-scoped request
            // after this point is rejected without it.
            Session.token = resp.token
            Session.nickname = session.nickname
            Session.email = resp.email
            session
        } else if (resp.code == "account_locked") {
            // Not a rejection the player can fix by typing more carefully,
            // so it carries its own kind and the screen offers recovery
            // instead of a retry button.
            throw AppException(
                AppError(
                    kind = AppError.Kind.LOCKED,
                    headline = "Account locked",
                    guidance = resp.message
                        ?: "Too many sign-in attempts. Reset your password to get back in."
                )
            )
        } else {
            throw IllegalStateException(resp.message ?: "Login failed")
        }
    }.mapNetworkError()

    override suspend fun register(
        name: String,
        nickname: String?,
        email: String,
        password: String
    ): Result<UserSession> = runCatching {
        val resp = KusinaApi.register(name.trim(), nickname, email.trim(), password)
        if (resp.status != "success" || resp.user_id == null) {
            throw IllegalStateException(resp.message ?: "Sign-up failed")
        }
    }.mapNetworkError().fold(
        onSuccess = {
            login(email, password).recoverCatching {
                throw IllegalStateException("Sign-up succeeded but login failed")
            }
        },
        onFailure = { Result.failure(it) }
    )

    override suspend fun signInWithGoogle(idToken: String, create: Boolean): Result<GoogleOutcome> = runCatching {
        val resp = KusinaApi.googleSignIn(idToken, create)

        // Not an error: the server checked, found nobody, and wrote nothing.
        // It hands back the email so the app can name the account it is
        // offering to create.
        if (resp.status == "no_account") {
            return@runCatching GoogleOutcome.NeedsSignUp(
                email = resp.email ?: "",
                name = resp.name ?: resp.display_name ?: ""
            )
        }

        if (resp.status == "success" && resp.user_id != null && resp.display_name != null) {
            val session = UserSession(
                userId = resp.user_id,
                displayName = resp.display_name,
                nickname = resp.nickname ?: resp.display_name,
                email = resp.email ?: ""
            )
            // Same ordering as login(): the token has to be in place before
            // anything account-scoped runs, or the next call is rejected.
            Session.token = resp.token
            Session.nickname = session.nickname
            Session.email = resp.email
            GoogleOutcome.SignedIn(session)
        } else {
            throw IllegalStateException(resp.message ?: "Google sign-in failed")
        }
    }.mapNetworkError()

    override suspend fun requestPasswordReset(email: String): Result<ResetCodeResult> = runCatching {
        val resp = KusinaApi.requestPasswordReset(email.trim())
        if (resp.status == "success") {
            ResetCodeResult(
                message = resp.message ?: "If this email exists, a reset code was sent.",
                devCode = resp.debug_code
            )
        } else {
            throw IllegalStateException(resp.message ?: "Request failed")
        }
    }.mapNetworkError()

    override suspend fun verifyResetCode(email: String, code: String): Result<Unit> =
        runCatching {
            val resp = KusinaApi.verifyResetCode(email.trim(), code.trim())
            if (resp.status != "success") {
                throw IllegalStateException(resp.message ?: "Invalid code or expired")
            }
        }.mapNetworkError()

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<String> =
        runCatching {
            val resp = KusinaApi.resetPassword(email.trim(), code.trim(), newPassword)
            if (resp.status == "success") {
                resp.message ?: "Password updated. You can log in now."
            } else {
                throw IllegalStateException(resp.message ?: "Reset failed")
            }
        }.mapNetworkError()
}

/** Wraps transport-level failures in a user-presentable message. */
// mapNetworkError now lives in ErrorMapping.kt, where it classifies the
// failure instead of prefixing the raw exception text with "Network error:".
