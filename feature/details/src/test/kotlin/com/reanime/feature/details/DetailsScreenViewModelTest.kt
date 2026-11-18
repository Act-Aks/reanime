package com.reanime.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.AnimeDetailsRepository
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.anime.EpisodeListing
import com.reanime.core.model.anime.EpisodeRepository
import com.reanime.core.model.result.Result
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
class DetailsScreenViewModelTest {
    private val store = ViewModelStore()
    private var nextId = 0

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
    fun `episodes can load independently while metadata is pending`() = runTest {
        val response = CompletableDeferred<Result<AnimeDetails, DetailsError>>()
        val vm = create(detailsLoad = { response.await() })
        runCurrent()
        assertTrue(vm.state.value.details is DetailsContentState.Loading)
        assertTrue(vm.state.value.episodes is EpisodesContentState.Content)
        response.complete(Result.Success(DetailsFixtures.details()))
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `retrying one request does not reissue the other`() = runTest {
        var metadataCalls = 0
        var episodeCalls = 0
        val vm = create(
            detailsLoad = {
                metadataCalls++
                Result.Success(DetailsFixtures.details(it))
            },
            episodeLoad = {
                episodeCalls++
                Result.Failure(EpisodeError.NOT_FOUND)
            }
        )
        advanceUntilIdle()
        vm.onAction(DetailsScreenAction.RetryDetails)
        advanceUntilIdle()
        assertEquals(2, metadataCalls)
        assertEquals(1, episodeCalls)
        vm.onAction(DetailsScreenAction.RetryEpisodes)
        advanceUntilIdle()
        assertEquals(2, episodeCalls)
    }

    @Test
    fun `selection filter and synopsis restore from small saved inputs`() = runTest {
        val handle = SavedStateHandle()
        val vm = create(handle = handle)
        advanceUntilIdle()
        vm.onAction(DetailsScreenAction.FilterEpisodes(EpisodeFilter.SUB))
        vm.onAction(DetailsScreenAction.SelectEpisode("1"))
        vm.onAction(DetailsScreenAction.ToggleSynopsis)
        val restored = create(handle = handle)
        advanceUntilIdle()
        assertEquals(EpisodeFilter.SUB, restored.state.value.filter)
        assertEquals("1", restored.state.value.selectedEpisode)
        assertTrue(restored.state.value.synopsisExpanded)
    }

    @Test
    fun `stale metadata response cannot overwrite a retry result`() = runTest {
        val old = CompletableDeferred<Result<AnimeDetails, DetailsError>>()
        var calls = 0
        val vm = create(detailsLoad = {
            if (++calls == 1) {
                withContext(NonCancellable) { old.await() }
            } else {
                Result.Failure(DetailsError.RATE_LIMITED)
            }
        })
        runCurrent()
        vm.onAction(DetailsScreenAction.RetryDetails)
        runCurrent()
        old.complete(Result.Success(DetailsFixtures.details()))
        advanceUntilIdle()
        assertEquals(DetailsContentState.Error(DetailsError.RATE_LIMITED), vm.state.value.details)
    }

    @Test
    fun `stale episode response cannot overwrite a retry result`() = runTest {
        val old = CompletableDeferred<Result<EpisodeListing, EpisodeError>>()
        var calls = 0
        val vm = create(episodeLoad = {
            if (++calls == 1) {
                withContext(NonCancellable) { old.await() }
            } else {
                Result.Failure(EpisodeError.ACCESS_BLOCKED)
            }
        })
        runCurrent()
        vm.onAction(DetailsScreenAction.RetryEpisodes)
        runCurrent()
        old.complete(Result.Success(DetailsFixtures.episodes()))
        advanceUntilIdle()
        assertEquals(EpisodesContentState.Error(EpisodeError.ACCESS_BLOCKED), vm.state.value.episodes)
    }

    @Test
    fun `different title requests have separate state and use their own identifiers`() = runTest {
        val first = create()
        val secondRequest = DetailsFixtures.request.copy(animeId = "another-show-123")
        val second = create(request = secondRequest)
        advanceUntilIdle()
        first.onAction(DetailsScreenAction.ToggleSynopsis)
        assertFalse(second.state.value.synopsisExpanded)
        assertEquals(secondRequest.animeId, (second.state.value.details as DetailsContentState.Content).data.anime.id)
    }

    private fun create(
        request: AnimeRequest = DetailsFixtures.request,
        handle: SavedStateHandle = SavedStateHandle(),
        detailsLoad: suspend (
            AnimeRequest
        ) -> Result<AnimeDetails, DetailsError> = { Result.Success(DetailsFixtures.details(it)) },
        episodeLoad: suspend (
            AnimeRequest
        ) -> Result<EpisodeListing, EpisodeError> = { Result.Success(DetailsFixtures.episodes(it)) }
    ): DetailsScreenViewModel {
        val detailsRepo = object : AnimeDetailsRepository {
            override suspend fun details(request: AnimeRequest): Result<AnimeDetails, DetailsError> =
                detailsLoad(request)
        }
        val episodeRepo = object : EpisodeRepository {
            override suspend fun episodes(request: AnimeRequest): Result<EpisodeListing, EpisodeError> =
                episodeLoad(request)
        }
        return DetailsScreenViewModel(request, handle, detailsRepo, episodeRepo).also {
            store.put("vm-${nextId++}", it)
        }
    }
}
