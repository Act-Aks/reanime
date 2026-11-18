package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReAnimeCatalogueSourceTest {
    @Test
    fun `requests only the selected mirror home without credentials`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://reanime.bz/home", request.url.toString())
            assertEquals(null, request.headers[HttpHeaders.Cookie])
            assertEquals(null, request.headers[HttpHeaders.Authorization])
            respond(homeFixture(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))
        }
        HttpClient(engine).use { client ->
            assertTrue(ReAnimeCatalogueSource(client).home(CatalogueMirror.BZ) is CatalogueResult.Success)
        }
    }

    @Test
    fun `HTTP failures map to safe domain errors without retrying`() = runTest {
        val cases = mapOf(
            HttpStatusCode.Forbidden to CatalogueError.ACCESS_BLOCKED,
            HttpStatusCode.TooManyRequests to CatalogueError.RATE_LIMITED,
            HttpStatusCode.ServiceUnavailable to CatalogueError.SOURCE_UNAVAILABLE,
            HttpStatusCode.NotFound to CatalogueError.UNEXPECTED_RESPONSE
        )
        cases.forEach { (status, error) ->
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond("", status)
                }
            ).use { client ->
                assertEquals(CatalogueResult.Failure(error), ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ))
                assertEquals(1, requests)
            }
        }
    }

    @Test
    fun `unexpected content type is rejected`() = runTest {
        HttpClient(
            MockEngine {
                respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ).use { client ->
            assertEquals(
                CatalogueResult.Failure(CatalogueError.UNEXPECTED_RESPONSE),
                ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ)
            )
        }
    }

    @Test
    fun `redirect does not contact an untrusted destination`() = runTest {
        var requests = 0
        HttpClient(
            MockEngine {
                requests++
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://example.com/home"))
            }
        ) { followRedirects = false }.use { client ->
            assertEquals(
                CatalogueResult.Failure(CatalogueError.UNEXPECTED_RESPONSE),
                ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ)
            )
            assertEquals(1, requests)
        }
    }

    @Test
    fun `oversized body is rejected before parsing`() = runTest {
        HttpClient(
            MockEngine {
                respond(
                    "x".repeat(2 * 1024 * 1024 + 1),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "text/html")
                )
            }
        ).use { client ->
            assertEquals(
                CatalogueResult.Failure(CatalogueError.RESPONSE_TOO_LARGE),
                ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ)
            )
        }
    }

    @Test
    fun `transport failure does not expose exception details`() = runTest {
        HttpClient(MockEngine { throw IOException("sensitive transport details") }).use { client ->
            assertEquals(
                CatalogueResult.Failure(CatalogueError.NETWORK),
                ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ)
            )
        }
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates instead of becoming a UI error`() = runTest {
        HttpClient(MockEngine { throw CancellationException("cancelled") }).use { client ->
            ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ)
        }
    }
}
