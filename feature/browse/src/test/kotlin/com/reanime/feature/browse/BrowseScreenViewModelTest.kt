package com.reanime.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueRepository
import com.reanime.core.model.catalogue.CatalogueResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseScreenViewModelTest {
    private val store = ViewModelStore()
    private var nextId = 0
    private val home = CatalogueHome(persistentListOf())

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load publishes repository content`() = runTest {
        val viewModel = create()

        advanceUntilIdle()

        assertEquals(BrowseCatalogueState.Content(home), viewModel.state.value.catalogue)
    }

    @Test
    fun `technical details and selected mirror restore from saved inputs`() = runTest {
        val handle = SavedStateHandle()
        val first = create(handle)
        first.onAction(BrowseScreenAction.ToggleTechnicalDetails)
        first.onAction(BrowseScreenAction.SelectMirror(CatalogueMirror.BZ))

        val restored = create(handle)
        advanceUntilIdle()

        assertEquals(CatalogueMirror.BZ, restored.state.value.mirror)
        assertTrue(restored.state.value.showTechnicalDetails)
        restored.onAction(BrowseScreenAction.ToggleTechnicalDetails)
        assertFalse(restored.state.value.showTechnicalDetails)
    }

    @Test
    fun `source failures become actionable state`() = runTest {
        val viewModel = create(
            repository = repository { _, _ ->
                CatalogueResult.Failure(CatalogueError.ACCESS_BLOCKED)
            }
        )

        advanceUntilIdle()

        assertEquals(BrowseCatalogueState.Error(CatalogueError.ACCESS_BLOCKED), viewModel.state.value.catalogue)
    }

    @Test
    fun `refresh bypasses repository cache`() = runTest {
        val refreshes = mutableListOf<Boolean>()
        val viewModel = create(
            repository = repository { _, refresh ->
                refreshes += refresh
                CatalogueResult.Success(home)
            }
        )
        advanceUntilIdle()
        viewModel.onAction(BrowseScreenAction.Refresh)
        advanceUntilIdle()

        assertEquals(listOf(false, true), refreshes)
    }

    @Test
    fun `older same-mirror request cannot replace a newer result even if cancellation is ignored`() = runTest {
        val firstResponse = CompletableDeferred<CatalogueResult>()
        var requests = 0
        val viewModel = create(
            repository = repository { _, _ ->
                if (++requests == 1) {
                    withContext(NonCancellable) { firstResponse.await() }
                } else {
                    CatalogueResult.Failure(CatalogueError.RATE_LIMITED)
                }
            }
        )
        runCurrent()
        viewModel.onAction(BrowseScreenAction.Refresh)
        runCurrent()
        firstResponse.complete(CatalogueResult.Success(home))
        advanceUntilIdle()

        assertEquals(BrowseCatalogueState.Error(CatalogueError.RATE_LIMITED), viewModel.state.value.catalogue)
    }

    private fun create(
        handle: SavedStateHandle = SavedStateHandle(),
        repository: CatalogueRepository = repository { _, _ -> CatalogueResult.Success(home) }
    ): BrowseScreenViewModel = BrowseScreenViewModel(handle, repository).also { store.put("vm-${nextId++}", it) }

    private fun repository(load: suspend (CatalogueMirror, Boolean) -> CatalogueResult): CatalogueRepository =
        object : CatalogueRepository {
            override suspend fun home(mirror: CatalogueMirror, refresh: Boolean): CatalogueResult =
                load(mirror, refresh)
        }
}
