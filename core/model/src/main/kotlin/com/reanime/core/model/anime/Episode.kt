package com.reanime.core.model.anime

import com.reanime.core.model.result.Error
import com.reanime.core.model.result.Result
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet

enum class EpisodeLanguage { SUB, DUB }

/** Number is the source's validated episode identity, including supported decimal specials. */
data class Episode(
    val number: String,
    val title: String,
    val synopsis: String?,
    val languages: PersistentSet<EpisodeLanguage>,
    val aired: String?,
    val thumbnailUrl: String?
)

data class EpisodeListing(val animeId: String, val episodes: PersistentList<Episode>, val reportedCount: Int?) {
    val isPartial: Boolean get() = reportedCount != null && episodes.size < reportedCount
}

enum class EpisodeError : Error {
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

interface EpisodeRepository {
    suspend fun episodes(request: AnimeRequest): Result<EpisodeListing, EpisodeError>
}
