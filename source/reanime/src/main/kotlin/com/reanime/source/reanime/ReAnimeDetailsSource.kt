package com.reanime.source.reanime

import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.anime.EpisodeListing
import com.reanime.core.model.result.Result
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.NetworkError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface AnimeDetailsPageSource {
    suspend fun details(request: AnimeRequest): Result<AnimeDetails, DetailsError>
}

fun interface EpisodePageSource {
    suspend fun episodes(request: AnimeRequest): Result<EpisodeListing, EpisodeError>
}

class ReAnimeDetailsSource(
    private val network: NetworkClient,
    private val parser: ReAnimeDetailsParser = ReAnimeDetailsParser(),
    private val parsingDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AnimeDetailsPageSource {
    override suspend fun details(request: AnimeRequest): Result<AnimeDetails, DetailsError> {
        if (!ReAnimeLinks.isAnimeId(request.animeId)) return Result.Failure(DetailsError.INVALID_ID)
        return when (val response = network.getHtml("https://${request.mirror.host}/anime/${request.animeId}")) {
            is Result.Success -> withContext(parsingDispatcher) { parser.parse(response.data, request) }
            is Result.Failure -> Result.Failure(response.error.toDetailsError())
        }
    }
}

class ReAnimeEpisodeSource(
    private val network: NetworkClient,
    private val parser: ReAnimeEpisodeParser = ReAnimeEpisodeParser(),
    private val parsingDispatcher: CoroutineDispatcher = Dispatchers.Default
) : EpisodePageSource {
    override suspend fun episodes(request: AnimeRequest): Result<EpisodeListing, EpisodeError> {
        if (!ReAnimeLinks.isAnimeId(request.animeId)) return Result.Failure(EpisodeError.INVALID_ID)
        return when (val response = network.getHtml("https://${request.mirror.host}/watch/${request.animeId}?ep=1")) {
            is Result.Success -> withContext(parsingDispatcher) { parser.parse(response.data, request) }
            is Result.Failure -> Result.Failure(response.error.toEpisodeError())
        }
    }
}

private fun NetworkError.toDetailsError(): DetailsError = when (this) {
    NetworkError.Connection -> DetailsError.NETWORK

    NetworkError.Timeout -> DetailsError.TIMEOUT

    NetworkError.InvalidRequest, NetworkError.UnexpectedResponse -> DetailsError.UNEXPECTED_RESPONSE

    NetworkError.ResponseTooLarge -> DetailsError.RESPONSE_TOO_LARGE

    is NetworkError.Http -> when (statusCode) {
        in ACCESS_DENIED -> DetailsError.ACCESS_BLOCKED
        NOT_FOUND -> DetailsError.NOT_FOUND
        RATE_LIMITED -> DetailsError.RATE_LIMITED
        in SERVER_ERRORS -> DetailsError.SOURCE_UNAVAILABLE
        else -> DetailsError.UNEXPECTED_RESPONSE
    }
}

private fun NetworkError.toEpisodeError(): EpisodeError = when (this) {
    NetworkError.Connection -> EpisodeError.NETWORK

    NetworkError.Timeout -> EpisodeError.TIMEOUT

    NetworkError.InvalidRequest, NetworkError.UnexpectedResponse -> EpisodeError.UNEXPECTED_RESPONSE

    NetworkError.ResponseTooLarge -> EpisodeError.RESPONSE_TOO_LARGE

    is NetworkError.Http -> when (statusCode) {
        in ACCESS_DENIED -> EpisodeError.ACCESS_BLOCKED
        NOT_FOUND -> EpisodeError.NOT_FOUND
        RATE_LIMITED -> EpisodeError.RATE_LIMITED
        in SERVER_ERRORS -> EpisodeError.SOURCE_UNAVAILABLE
        else -> EpisodeError.UNEXPECTED_RESPONSE
    }
}

private val ACCESS_DENIED = setOf(401, 403)
private const val NOT_FOUND = 404
private const val RATE_LIMITED = 429
private val SERVER_ERRORS = 500..599
