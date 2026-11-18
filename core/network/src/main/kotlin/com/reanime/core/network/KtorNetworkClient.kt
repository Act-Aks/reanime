package com.reanime.core.network

import com.reanime.core.model.result.Result
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readBuffer
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import kotlinx.io.readByteArray

class KtorNetworkClient(private val client: HttpClient) : NetworkClient {
    override suspend fun getHtml(url: String): Result<String, NetworkError> {
        if (!isSafeHttpsUrl(url)) return Result.Failure(NetworkError.InvalidRequest)
        return requestHtml(url)
    }

    private suspend fun requestHtml(url: String): Result<String, NetworkError> = try {
        client.prepareGet(url) { headers.append(HttpHeaders.Accept, "text/html") }.execute { response ->
            when {
                response.status != HttpStatusCode.OK -> Result.Failure(NetworkError.Http(response.status.value))

                response.contentType()?.withoutParameters()?.toString() !in HTML_TYPES ->
                    Result.Failure(NetworkError.UnexpectedResponse)

                else -> {
                    val bytes = response.bodyAsChannel().readBuffer(MAX_HTML_BYTES + 1).readByteArray()
                    if (bytes.size > MAX_HTML_BYTES) {
                        Result.Failure(NetworkError.ResponseTooLarge)
                    } else {
                        Result.Success(bytes.decodeToString())
                    }
                }
            }
        }
    } catch (_: HttpRequestTimeoutException) {
        Result.Failure(NetworkError.Timeout)
    } catch (_: ConnectTimeoutException) {
        Result.Failure(NetworkError.Timeout)
    } catch (_: SocketTimeoutException) {
        Result.Failure(NetworkError.Timeout)
    } catch (_: IOException) {
        Result.Failure(NetworkError.Connection)
    }

    private fun isSafeHttpsUrl(url: String): Boolean = try {
        val uri = URI(url)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            (uri.port == -1 || uri.port == HTTPS_PORT)
    } catch (_: URISyntaxException) {
        false
    }

    private companion object {
        const val MAX_HTML_BYTES = 2L * 1024 * 1024
        const val HTTPS_PORT = 443
        val HTML_TYPES = setOf("text/html", "application/xhtml+xml")
    }
}
