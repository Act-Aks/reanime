package com.reanime.feature.details

import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.Episode
import com.reanime.core.model.anime.EpisodeLanguage
import com.reanime.core.model.anime.EpisodeListing
import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.catalogue.CatalogueMirror
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object DetailsFixtures {
    val request = AnimeRequest("moon-garden-abc123", CatalogueMirror.CZ)
    fun details(request: AnimeRequest = this.request): AnimeDetails = AnimeDetails(
        AnimeSummary(request.animeId, request.animeId, null),
        null,
        "Synopsis",
        persistentListOf(),
        persistentListOf()
    )
    fun episodes(request: AnimeRequest = this.request): EpisodeListing = EpisodeListing(
        request.animeId,
        persistentListOf(
            Episode("1", "First", "First synopsis", persistentSetOf(EpisodeLanguage.SUB), null, null),
            Episode("2", "Second", null, persistentSetOf(EpisodeLanguage.DUB), null, null)
        ),
        2
    )
}
