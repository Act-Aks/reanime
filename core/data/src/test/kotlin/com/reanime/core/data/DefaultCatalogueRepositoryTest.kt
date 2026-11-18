package com.reanime.core.data

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueResult
import com.reanime.source.reanime.CataloguePageSource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCatalogueRepositoryTest {
    private val success = CatalogueResult.Success(CatalogueHome(persistentListOf()))

    @Test
    fun `fresh success is cached while explicit refresh bypasses cache`() = runTest {
        var requests = 0
        val repository = DefaultCatalogueRepository(
            CataloguePageSource {
                requests++
                success
            }
        )

        repository.home(CatalogueMirror.CZ)
        repository.home(CatalogueMirror.CZ)
        assertEquals(1, requests)
        repository.home(CatalogueMirror.CZ, refresh = true)
        assertEquals(2, requests)
    }

    @Test
    fun `cache expires and mirrors have independent entries`() = runTest {
        var requests = 0
        var now = 0L
        val repository = DefaultCatalogueRepository(
            CataloguePageSource {
                requests++
                success
            },
            nowMillis = { now }
        )

        repository.home(CatalogueMirror.CZ)
        repository.home(CatalogueMirror.BZ)
        assertEquals(2, requests)
        now = 300_000
        repository.home(CatalogueMirror.CZ)
        assertEquals(3, requests)
    }

    @Test
    fun `failures are not cached`() = runTest {
        var requests = 0
        val repository = DefaultCatalogueRepository(
            CataloguePageSource {
                requests++
                CatalogueResult.Failure(CatalogueError.ACCESS_BLOCKED)
            }
        )

        repository.home(CatalogueMirror.CZ)
        repository.home(CatalogueMirror.CZ)
        assertEquals(2, requests)
    }

    @Test
    fun `concurrent loads share a successful cache fill`() = runTest {
        var requests = 0
        val response = CompletableDeferred<CatalogueResult>()
        val repository = DefaultCatalogueRepository(
            CataloguePageSource {
                requests++
                response.await()
            }
        )

        val first = async { repository.home(CatalogueMirror.CZ) }
        val second = async { repository.home(CatalogueMirror.CZ) }
        runCurrent()
        assertEquals(1, requests)
        response.complete(success)
        assertEquals(success, first.await())
        assertEquals(success, second.await())
        assertEquals(1, requests)
    }
}
