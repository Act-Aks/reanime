package com.reanime.core.model.catalogue

import com.reanime.core.model.result.Error
import com.reanime.core.model.result.Result
import kotlinx.collections.immutable.PersistentList

enum class CatalogueMirror(val host: String) {
    CZ("reanime.cz"),
    BZ("reanime.bz"),
    WTF("reanime.wtf")
}

data class AnimeSummary(val id: String, val title: String, val posterUrl: String?)

enum class CatalogueSectionKind {
    LATEST_EPISODES,
    NEW_ON_SITE,
    UPCOMING
}

data class CatalogueSection(val kind: CatalogueSectionKind, val anime: PersistentList<AnimeSummary>)

data class CatalogueHome(val sections: PersistentList<CatalogueSection>)

enum class CatalogueError : Error {
    NETWORK,
    TIMEOUT,
    ACCESS_BLOCKED,
    RATE_LIMITED,
    SOURCE_UNAVAILABLE,
    UNEXPECTED_RESPONSE,
    RESPONSE_TOO_LARGE,
    LAYOUT_CHANGED
}

interface CatalogueRepository {
    suspend fun home(mirror: CatalogueMirror, refresh: Boolean = false): Result<CatalogueHome, CatalogueError>
}
