package com.reanime.core.network

import com.reanime.core.model.result.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class KtorNetworkClientTest {
    @Test
    fun `shared configuration sends HTML requests without credentials`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/home", request.url.toString())
            assertEquals("text/html", request.headers[HttpHeaders.Accept])
            assertEquals("ReAnime-test", request.headers[HttpHeaders.UserAgent])
            assertEquals(null, request.headers[HttpHeaders.Cookie])
            assertEquals(null, request.headers[HttpHeaders.Authorization])
            respond(
                "<main>Home</main>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
            )
        }
        createHttpClient("ReAnime-test", engine).use { client ->
            assertEquals(Result.Success("<main>Home</main>"), KtorNetworkClient(client).getHtml(URL))
        }
    }

    @Test
    fun `HTTP status is preserved without automatic retries`() = runTest {
        listOf(
            HttpStatusCode.Forbidden,
            HttpStatusCode.TooManyRequests,
            HttpStatusCode.ServiceUnavailable
        ).forEach { status ->
            var requests = 0
            createHttpClient(
                "ReAnime-test",
                MockEngine {
                    requests++
                    respond("", status)
                }
            ).use { client ->
                assertEquals(Result.Failure(NetworkError.Http(status.value)), KtorNetworkClient(client).getHtml(URL))
                assertEquals(1, requests)
            }
        }
    }

    @Test
    fun `redirect is returned without requesting another host`() = runTest {
        var requests = 0
        createHttpClient(
            "ReAnime-test",
            MockEngine {
                requests++
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://elsewhere.example/home"))
            }
        ).use { client ->
            assertEquals(
                Result.Failure(NetworkError.Http(HttpStatusCode.Found.value)),
                KtorNetworkClient(client).getHtml(URL)
            )
            assertEquals(1, requests)
        }
    }

    @Test
    fun `non-HTML response is rejected`() = runTest {
        createHttpClient(
            "ReAnime-test",
            MockEngine {
                respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ).use { client ->
            assertEquals(Result.Failure(NetworkError.UnexpectedResponse), KtorNetworkClient(client).getHtml(URL))
        }
    }

    @Test
    fun `body size is bounded`() = runTest {
        createHttpClient(
            "ReAnime-test",
            MockEngine {
                respond(
                    "x".repeat(2 * 1024 * 1024 + 1),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "text/html")
                )
            }
        ).use { client ->
            assertEquals(Result.Failure(NetworkError.ResponseTooLarge), KtorNetworkClient(client).getHtml(URL))
        }
    }

    @Test
    fun `invalid and credential-bearing URLs fail before network access`() = runTest {
        var requests = 0
        createHttpClient(
            "ReAnime-test",
            MockEngine {
                requests++
                respond("unexpected")
            }
        ).use { client ->
            listOf(
                "http://example.com/home",
                "https://user@example.com/home",
                "https://example.com:444/home",
                "bad url"
            )
                .forEach { url ->
                    assertEquals(Result.Failure(NetworkError.InvalidRequest), KtorNetworkClient(client).getHtml(url))
                }
            assertEquals(0, requests)
        }
    }

    @Test
    fun `transport exceptions become safe typed failures`() = runTest {
        createHttpClient("ReAnime-test", MockEngine { throw IOException("sensitive details") }).use { client ->
            assertEquals(Result.Failure(NetworkError.Connection), KtorNetworkClient(client).getHtml(URL))
        }
    }

    @Test
    fun `timeouts are distinguished from connection errors`() = runTest {
        createHttpClient("ReAnime-test", MockEngine { throw HttpRequestTimeoutException(URL, 1L) }).use { client ->
            assertEquals(Result.Failure(NetworkError.Timeout), KtorNetworkClient(client).getHtml(URL))
        }
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates`() = runTest {
        createHttpClient("ReAnime-test", MockEngine { throw CancellationException("cancelled") }).use { client ->
            KtorNetworkClient(client).getHtml(URL)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `programming defects are not silently converted to connection failures`() = runTest {
        createHttpClient("ReAnime-test", MockEngine { error("programming defect") }).use { client ->
            KtorNetworkClient(client).getHtml(URL)
        }
    }

    private companion object {
        const val URL = "https://example.com/home"
    }
}

private fun createHttpClient(userAgent: String, engine: MockEngine): HttpClient = createHttpClient(
    userAgent,
    object : HttpClientEngineFactory<MockEngineConfig> {
        override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine = engine
    }
)
