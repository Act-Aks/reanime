package com.reanime.core.data

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueRepository
import com.reanime.core.model.result.Result
import com.reanime.source.reanime.CataloguePageSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultCatalogueRepository(
    private val source: CataloguePageSource,
    private val nowMillis: () -> Long = { System.nanoTime() / NANOS_PER_MILLISECOND }
) : CatalogueRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<CatalogueMirror, CachedHome>()

    override suspend fun home(mirror: CatalogueMirror, refresh: Boolean): Result<CatalogueHome, CatalogueError> =
        mutex.withLock {
            val cached = cache[mirror]
            if (!refresh && cached != null && nowMillis() - cached.savedAt < CACHE_LIFETIME_MILLIS) {
                return@withLock cached.result
            }
            source.home(mirror).also { result ->
                if (result is Result.Success) cache[mirror] = CachedHome(result, nowMillis())
            }
        }

    private data class CachedHome(val result: Result.Success<CatalogueHome>, val savedAt: Long)

    private companion object {
        const val CACHE_LIFETIME_MILLIS = 5 * 60 * 1000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
