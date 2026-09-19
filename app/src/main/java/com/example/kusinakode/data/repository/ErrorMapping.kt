package com.example.kusinakode.data.repository

import com.example.kusinakode.domain.model.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Carries a classified [AppError] so screens don't re-parse exception text. */
class AppException(val error: AppError) : Exception(error.headline)

/**
 * Turns whatever the network layer threw into something worth showing.
 *
 * The distinction that matters most here is offline vs unreachable. "No
 * internet" and "the server didn't answer" look identical to a player but
 * need opposite responses, and while the API lives on a LAN address the
 * second one happens every time the team moves to a different network.
 */
internal fun Throwable.toAppError(): AppError {
    val technical = "${this::class.simpleName}: ${localizedMessage ?: message ?: "no detail"}"

    return when {
        // Thrown by our own repositories for a refusal the server explained.
        this is IllegalStateException -> AppError(
            kind = AppError.Kind.REJECTED,
            headline = message ?: "That didn't work",
            guidance = "Check the details above and try again.",
            technical = null // it's already a human sentence
        )

        this is UnknownHostException -> AppError(
            kind = AppError.Kind.OFFLINE,
            headline = "You're offline",
            guidance = "Turn on Wi-Fi or mobile data, then try again.",
            technical = technical
        )

        this is ConnectTimeoutException ||
            this is SocketTimeoutException ||
            this is HttpRequestTimeoutException ||
            this is ConnectException -> AppError(
            kind = AppError.Kind.UNREACHABLE,
            headline = "The kitchen isn't answering",
            guidance = "We reached the network but not the server. If you've " +
                "switched Wi-Fi, its address has probably changed.",
            technical = technical
        )

        this is ResponseException -> AppError(
            kind = AppError.Kind.SERVER,
            headline = "Something burned on our side",
            guidance = "The server had a problem with that request. Give it a moment.",
            technical = "${technical} (HTTP ${response.status.value})"
        )

        this is IOException -> AppError(
            kind = AppError.Kind.UNREACHABLE,
            headline = "Lost the connection",
            guidance = "The connection dropped mid-request. Try again.",
            technical = technical
        )

        else -> AppError(
            kind = AppError.Kind.UNKNOWN,
            headline = "That didn't go through",
            guidance = "Something unexpected happened. Try again.",
            technical = technical
        )
    }
}

/**
 * Wraps a failed [Result] so its exception carries a classified error.
 *
 * Screens that only read `e.message` still improve — they get the headline
 * instead of a stack-trace fragment — while screens that want the full
 * treatment can check for [AppException].
 */
internal fun <T> Result<T>.mapNetworkError(): Result<T> = recoverCatching { e ->
    if (e is AppException) throw e
    throw AppException(e.toAppError())
}
