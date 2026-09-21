package com.example.kusinakode.data.repository

import com.example.kusinakode.data.net.NetworkStatus
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
 * The distinction that matters most is offline vs unreachable: "no internet"
 * and "the server didn't answer" look identical to a player but need opposite
 * responses - check your Wi-Fi, versus wait and try again.
 *
 * That used to be decided by exception type alone, and it was wrong. The rule
 * was "UnknownHostException means offline", which only holds while the API is
 * addressed by name. Ours is a bare IP, and connecting to a literal address
 * does no DNS lookup, so that exception never arrived. Every offline player
 * fell through to the unreachable branch and was told the server had moved -
 * advice that made sense when the backend was a laptop on the team's Wi-Fi
 * and is simply false now that it is a fixed address in Azure.
 *
 * So the device is asked directly, and the exception only decides what kind
 * of failure it was once we know the connection itself is fine.
 */
internal fun Throwable.toAppError(): AppError {
    val technical = "${this::class.simpleName}: ${localizedMessage ?: message ?: "no detail"}"

    // Checked before anything else: with no connection, every one of the
    // branches below would be describing a symptom rather than the cause.
    val offline = AppError(
        kind = AppError.Kind.OFFLINE,
        headline = "No internet connection",
        guidance = "Turn on Wi-Fi or mobile data, then try again.",
        technical = technical
    )

    return when {
        // Thrown by our own repositories for a refusal the server explained.
        // Checked first: a wrong password is a real answer and must not be
        // relabelled as a network problem just because the radio is flaky.
        this is IllegalStateException -> AppError(
            kind = AppError.Kind.REJECTED,
            headline = message ?: "That didn't work",
            guidance = "Check the details above and try again.",
            technical = null // it's already a human sentence
        )

        this is UnknownHostException -> offline

        !NetworkStatus.isOnline() -> offline

        this is ConnectTimeoutException ||
            this is SocketTimeoutException ||
            this is HttpRequestTimeoutException ||
            this is ConnectException -> AppError(
            kind = AppError.Kind.UNREACHABLE,
            headline = "Can't reach Kusina Kode",
            guidance = "Your connection is working, but our server isn't " +
                "answering right now. Please try again in a moment.",
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
