package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readBuffer
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray

fun interface CataloguePageSource {
    suspend fun home(mirror: CatalogueMirror): CatalogueResult
}

fun createReAnimeHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    followRedirects = false
    engine { config { retryOnConnectionFailure(false) } }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}

class ReAnimeCatalogueSource(
    private val client: HttpClient,
    private val parser: ReAnimeHomeParser = ReAnimeHomeParser(),
    private val parsingDispatcher: CoroutineDispatcher = Dispatchers.Default
) : CataloguePageSource {
    override suspend fun home(mirror: CatalogueMirror): CatalogueResult = try {
        client.prepareGet("https://${mirror.host}/home") {
            headers.append(HttpHeaders.Accept, "text/html")
            headers.append(HttpHeaders.UserAgent, "ReAnime/0.1 (native Android catalogue client)")
        }.execute { response ->
            val error = responseError(response.status)
            when {
                error != null -> CatalogueResult.Failure(error)

                response.contentType()?.withoutParameters()?.toString() !in HTML_TYPES ->
                    CatalogueResult.Failure(CatalogueError.UNEXPECTED_RESPONSE)

                else -> {
                    val bytes = response.bodyAsChannel().readBuffer(MAX_HTML_BYTES + 1).readByteArray()
                    if (bytes.size > MAX_HTML_BYTES) {
                        CatalogueResult.Failure(CatalogueError.RESPONSE_TOO_LARGE)
                    } else {
                        withContext(parsingDispatcher) { parser.parse(bytes.decodeToString(), mirror) }
                    }
                }
            }
        }
    } catch (_: HttpRequestTimeoutException) {
        CatalogueResult.Failure(CatalogueError.TIMEOUT)
    } catch (_: ConnectTimeoutException) {
        CatalogueResult.Failure(CatalogueError.TIMEOUT)
    } catch (_: SocketTimeoutException) {
        CatalogueResult.Failure(CatalogueError.TIMEOUT)
    } catch (_: IOException) {
        CatalogueResult.Failure(CatalogueError.NETWORK)
    }

    private fun responseError(status: HttpStatusCode): CatalogueError? = when (status) {
        HttpStatusCode.OK -> null

        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> CatalogueError.ACCESS_BLOCKED

        HttpStatusCode.TooManyRequests -> CatalogueError.RATE_LIMITED

        else -> if (status.value in SERVER_ERROR_RANGE) {
            CatalogueError.SOURCE_UNAVAILABLE
        } else {
            CatalogueError.UNEXPECTED_RESPONSE
        }
    }

    private companion object {
        const val MAX_HTML_BYTES = 2L * 1024 * 1024
        val HTML_TYPES = setOf("text/html", "application/xhtml+xml")
        val SERVER_ERROR_RANGE = 500..599
    }
}

private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val REQUEST_TIMEOUT_MILLIS = 20_000L
