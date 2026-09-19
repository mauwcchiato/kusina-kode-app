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
        // Every account-scoped endpoint authenticates by bearer token, so it
        // goes on here once rather than at each of the ~15 call sites.
        // Read per request, not captured: the token changes at login.
        defaultRequest {
            Session.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
