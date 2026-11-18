package com.reanime.core.data

import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.AnimeDetailsRepository
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.anime.EpisodeListing
import com.reanime.core.model.anime.EpisodeRepository
import com.reanime.core.model.result.Result
import com.reanime.source.reanime.AnimeDetailsPageSource
import com.reanime.source.reanime.EpisodePageSource

/** Data-layer adapter for independent contracts; screen state retains results across rotation. */
class DefaultAnimeRepository(
    private val detailsSource: AnimeDetailsPageSource,
    private val episodeSource: EpisodePageSource
) : AnimeDetailsRepository,
    EpisodeRepository {
    override suspend fun details(request: AnimeRequest): Result<AnimeDetails, DetailsError> =
        detailsSource.details(request)

    override suspend fun episodes(request: AnimeRequest): Result<EpisodeListing, EpisodeError> =
        episodeSource.episodes(request)
}
