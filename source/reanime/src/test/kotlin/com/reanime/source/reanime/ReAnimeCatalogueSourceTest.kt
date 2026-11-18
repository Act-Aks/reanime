package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.NetworkError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReAnimeCatalogueSourceTest {
    @Test
    fun `selected mirror endpoint is passed to the injected transport`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val network = NetworkClient { url ->
            requestedUrls += url
            Result.Success(homeFixture())
        }

        val result = ReAnimeCatalogueSource(network).home(CatalogueMirror.BZ)

        assertTrue(result is Result.Success)
        assertEquals(listOf("https://reanime.bz/home"), requestedUrls)
    }

    @Test
    fun `transport failures map to feature-owned errors without retrying`() = runTest {
        val cases = mapOf(
            NetworkError.Connection to CatalogueError.NETWORK,
            NetworkError.Timeout to CatalogueError.TIMEOUT,
            NetworkError.InvalidRequest to CatalogueError.UNEXPECTED_RESPONSE,
            NetworkError.UnexpectedResponse to CatalogueError.UNEXPECTED_RESPONSE,
            NetworkError.ResponseTooLarge to CatalogueError.RESPONSE_TOO_LARGE,
            NetworkError.Http(401) to CatalogueError.ACCESS_BLOCKED,
            NetworkError.Http(403) to CatalogueError.ACCESS_BLOCKED,
            NetworkError.Http(429) to CatalogueError.RATE_LIMITED,
            NetworkError.Http(503) to CatalogueError.SOURCE_UNAVAILABLE,
            NetworkError.Http(404) to CatalogueError.UNEXPECTED_RESPONSE,
            NetworkError.Http(302) to CatalogueError.UNEXPECTED_RESPONSE
        )
        cases.forEach { (error, expected) ->
            var requests = 0
            val source = ReAnimeCatalogueSource(
                NetworkClient {
                    requests++
                    Result.Failure(error)
                }
            )

            assertEquals(Result.Failure(expected), source.home(CatalogueMirror.CZ))
            assertEquals(1, requests)
        }
    }

    @Test(expected = CancellationException::class)
    fun `source does not convert cancellation into a result`() = runTest {
        ReAnimeCatalogueSource(NetworkClient { throw CancellationException("cancelled") })
            .home(CatalogueMirror.CZ)
    }
}
