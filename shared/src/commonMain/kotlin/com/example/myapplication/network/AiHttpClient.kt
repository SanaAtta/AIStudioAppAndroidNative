package com.example.myapplication.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

object AiHttpClient {
    val http: HttpClient by lazy {
        createPlatformHttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 60_000
                requestTimeoutMillis = 300_000
                socketTimeoutMillis = 300_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer ${AiConfig.API_KEY}")
            }
        }
    }
}
