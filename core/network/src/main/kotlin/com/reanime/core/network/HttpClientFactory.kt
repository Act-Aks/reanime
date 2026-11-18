package com.reanime.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent

/** Every call creates an independent, credential-free client. No automatic retries or redirects. */
fun createHttpClient(userAgent: String): HttpClient = createHttpClient(userAgent, OkHttp) {
    config { retryOnConnectionFailure(false) }
}

// Use the factory overload: Ktor then owns and closes the engine with its client.
internal fun <T : HttpClientEngineConfig> createHttpClient(
    userAgent: String,
    factory: HttpClientEngineFactory<T>,
    configureEngine: T.() -> Unit = {}
): HttpClient = HttpClient(factory) {
    engine(configureEngine)
    expectSuccess = false
    followRedirects = false
    install(UserAgent) { agent = userAgent }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}

private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val REQUEST_TIMEOUT_MILLIS = 20_000L
