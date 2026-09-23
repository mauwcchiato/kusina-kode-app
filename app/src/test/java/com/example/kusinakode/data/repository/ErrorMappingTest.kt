package com.example.kusinakode.data.repository

import com.example.kusinakode.domain.model.AppError
import io.ktor.client.network.sockets.ConnectTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

class ErrorMappingTest {

    @Test
    fun `a timeout reads as the server not answering, not as being offline`() {
        // The distinction the player needs: their connection is fine, ours isn't.
        val error = ConnectTimeoutException("http://192.168.100.21/…").toAppError()

        assertEquals(AppError.Kind.UNREACHABLE, error.kind)
        // Was "guidance should hint at the address changing". That advice made
        // sense when the backend was a laptop on the team's Wi-Fi; the API now
        // has a fixed address in Azure, so telling a player it had moved was
        // simply false. What the guidance has to do is separate "your
        // connection is fine" from "we are not answering" - which is the
        // distinction this test is named for.
        assertTrue(
            "guidance should say the connection is fine and the server is not",
            error.guidance.contains("connection", ignoreCase = true) &&
                error.guidance.contains("server", ignoreCase = true)
        )
    }

    @Test
    fun `an unresolvable host reads as offline`() {
        val error = UnknownHostException("api.example.com").toAppError()
        assertEquals(AppError.Kind.OFFLINE, error.kind)
    }

    @Test
    fun `a refused connection is unreachable, not unknown`() {
        assertEquals(AppError.Kind.UNREACHABLE, ConnectException("refused").toAppError().kind)
    }

    @Test
    fun `a dropped socket is unreachable`() {
        assertEquals(AppError.Kind.UNREACHABLE, IOException("reset by peer").toAppError().kind)
    }

    @Test
    fun `a server refusal keeps its own wording and hides technical noise`() {
        // Repositories throw IllegalStateException with the server's message.
        val error = IllegalStateException("Invalid credentials").toAppError()

        assertEquals(AppError.Kind.REJECTED, error.kind)
        assertEquals("Invalid credentials", error.headline)
        assertNull("a human sentence needs no Details section", error.technical)
        assertFalse("retrying the same password won't help", error.isRetryable)
    }

    @Test
    fun `no message ever leaks a raw url into the headline`() {
        val raw = "Connect timeout has expired [url=http://192.168.100.21/kusinakode/rest/login.php]"
        val error = ConnectTimeoutException(raw).toAppError()

        assertFalse("headline must stay readable", error.headline.contains("http"))
        assertFalse(error.headline.contains("["))
        assertNotNull("but the detail is still available for debugging", error.technical)
    }

    @Test
    fun `an unrecognised failure still produces something calm and retryable`() {
        val error = RuntimeException("kaboom").toAppError()

        assertEquals(AppError.Kind.UNKNOWN, error.kind)
        assertTrue(error.headline.isNotBlank())
        assertTrue(error.guidance.isNotBlank())
        assertTrue(error.isRetryable)
    }

    @Test
    fun `mapNetworkError wraps a failure once, not repeatedly`() {
        val once = Result.failure<String>(UnknownHostException("x")).mapNetworkError()
        val twice = once.mapNetworkError()

        val first = once.exceptionOrNull() as AppException
        val second = twice.exceptionOrNull() as AppException
        assertEquals(first.error, second.error)
    }

    @Test
    fun `a successful result passes through untouched`() {
        assertEquals("ok", Result.success("ok").mapNetworkError().getOrNull())
    }
}
