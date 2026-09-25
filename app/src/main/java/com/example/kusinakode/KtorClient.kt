package com.kusinakode

import com.example.kusinakode.Session
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.json.Json

object KtorClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        // Fail a stalled call in a bounded time instead of leaving the UI to
        // wait on CIO's defaults. ErrorMapping already turns these three
        // exceptions into a friendly "slow/again" message.
        //   request: whole call, DNS to last byte - the ceiling a spinner waits
        //   connect: TCP/TLS handshake - a dead host should not eat the budget
        //   socket : gap between bytes once connected - catches a stalled stream
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis  = 30_000
        }
        // Every account-scoped endpoint authenticates by bearer token, so it
        // goes on here once rather than at each of the ~15 call sites.
        // Read per request, not captured: the token changes at login.
        defaultRequest {
            Session.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
