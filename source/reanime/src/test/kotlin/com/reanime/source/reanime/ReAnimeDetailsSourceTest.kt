package com.reanime.source.reanime

import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.NetworkError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReAnimeDetailsSourceTest {
    private val request = AnimeRequest("moon-garden-abc123", CatalogueMirror.BZ)

    @Test
    fun `details and episodes request only their selected mirror HTML pages`() = runTest {
        val urls = mutableListOf<String>()
        val network = NetworkClient { url ->
            urls += url
            Result.Success(if (url.contains("/anime/")) detailsFixture() else episodeFixture())
        }
        assertTrue(ReAnimeDetailsSource(network).details(request) is Result.Success)
        assertTrue(ReAnimeEpisodeSource(network).episodes(request) is Result.Success)
        assertEquals(
            listOf("https://reanime.bz/anime/moon-garden-abc123", "https://reanime.bz/watch/moon-garden-abc123?ep=1"),
            urls
        )
    }

    @Test
    fun `malformed IDs never reach the transport`() = runTest {
        var requests = 0
        val network = NetworkClient {
            requests++
            Result.Success("unexpected")
        }
        listOf("../evil", "id?ep=2", "id/123", "", "x".repeat(201)).forEach { id ->
            val invalid = request.copy(animeId = id)
            assertEquals(Result.Failure(DetailsError.INVALID_ID), ReAnimeDetailsSource(network).details(invalid))
            assertEquals(Result.Failure(EpisodeError.INVALID_ID), ReAnimeEpisodeSource(network).episodes(invalid))
        }
        assertEquals(0, requests)
    }

    @Test
    fun `domain errors distinguish not found blocked and rate limited`() = runTest {
        val cases = listOf(
            Triple(NetworkError.Http(404), DetailsError.NOT_FOUND, EpisodeError.NOT_FOUND),
            Triple(NetworkError.Http(403), DetailsError.ACCESS_BLOCKED, EpisodeError.ACCESS_BLOCKED),
            Triple(NetworkError.Http(429), DetailsError.RATE_LIMITED, EpisodeError.RATE_LIMITED),
            Triple(NetworkError.Http(503), DetailsError.SOURCE_UNAVAILABLE, EpisodeError.SOURCE_UNAVAILABLE),
            Triple(NetworkError.Timeout, DetailsError.TIMEOUT, EpisodeError.TIMEOUT),
            Triple(NetworkError.Connection, DetailsError.NETWORK, EpisodeError.NETWORK),
            Triple(NetworkError.ResponseTooLarge, DetailsError.RESPONSE_TOO_LARGE, EpisodeError.RESPONSE_TOO_LARGE),
            Triple(NetworkError.InvalidRequest, DetailsError.UNEXPECTED_RESPONSE, EpisodeError.UNEXPECTED_RESPONSE),
            Triple(NetworkError.UnexpectedResponse, DetailsError.UNEXPECTED_RESPONSE, EpisodeError.UNEXPECTED_RESPONSE)
        )
        cases.forEach { (error, details, episodes) ->
            val network = NetworkClient { Result.Failure(error) }
            assertEquals(Result.Failure(details), ReAnimeDetailsSource(network).details(request))
            assertEquals(Result.Failure(episodes), ReAnimeEpisodeSource(network).episodes(request))
        }
    }

    @Test(expected = CancellationException::class)
    fun `episode cancellation propagates`() = runTest {
        ReAnimeEpisodeSource(NetworkClient { throw CancellationException("cancelled") }).episodes(request)
    }

    @Test(expected = CancellationException::class)
    fun `details cancellation propagates`() = runTest {
        ReAnimeDetailsSource(NetworkClient { throw CancellationException("cancelled") }).details(request)
    }
}
