package com.reanime.core.model.anime

import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Error
import com.reanime.core.model.result.Result
import kotlinx.collections.immutable.PersistentList

data class AnimeRequest(val animeId: String, val mirror: CatalogueMirror)

data class AnimeFact(val label: String, val value: String)

data class AnimeDetails(
    val anime: AnimeSummary,
    val alternativeTitle: String?,
    val synopsis: String?,
    val facts: PersistentList<AnimeFact>,
    val genres: PersistentList<String>
)

enum class DetailsError : Error {
    INVALID_ID,
    NETWORK,
    TIMEOUT,
    ACCESS_BLOCKED,
    RATE_LIMITED,
    NOT_FOUND,
    SOURCE_UNAVAILABLE,
    UNEXPECTED_RESPONSE,
    RESPONSE_TOO_LARGE,
    LAYOUT_CHANGED,
    IDENTITY_MISMATCH
}

interface AnimeDetailsRepository {
    suspend fun details(request: AnimeRequest): Result<AnimeDetails, DetailsError>
}
