package com.reanime.app.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueSection
import com.reanime.core.model.catalogue.CatalogueSectionKind
import com.reanime.core.model.result.Result
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.di.networkModule
import com.reanime.feature.details.DetailsContentState
import com.reanime.feature.details.DetailsScreenViewModel
import com.reanime.feature.details.EpisodesContentState
import com.reanime.feature.details.detailsModule
import com.reanime.feature.home.HomeCatalogueState
import com.reanime.feature.home.HomeScreenViewModel
import com.reanime.feature.home.homeModule
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogueModuleTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `production repository and ViewModel bindings resolve without live network access`() = runTest {
        val home = CatalogueHome(
            persistentListOf(
                CatalogueSection(
                    CatalogueSectionKind.LATEST_EPISODES,
                    persistentListOf(AnimeSummary("test-123", "Test", null))
                )
            )
        )
        val application = koinApplication {
            allowOverride(true)
            modules(
                networkModule("ReAnime-test"),
                catalogueModule,
                homeModule,
                module {
                    single { SavedStateHandle() }
                    single<NetworkClient> { NetworkClient { Result.Success(HOME_HTML) } }
                }
            )
        }
        val store = ViewModelStore()
        try {
            val viewModel = application.koin.get<HomeScreenViewModel>()
            store.put("home", viewModel)
            val loaded = viewModel.state.first { it.catalogue !is HomeCatalogueState.Loading }

            assertEquals(HomeCatalogueState.Content(home), loaded.catalogue)
        } finally {
            store.clear()
            application.close()
        }
    }

    private companion object {
        const val HOME_HTML = "<main><section><h2>Latest Episodes</h2>" +
            "<a href='/anime/test-123'><h3>Test</h3></a></section></main>"
    }

    @Test
    fun `details DI forwards independent route parameters through repositories to fake transport`() = runTest {
        val requests = mutableListOf<String>()
        val application = koinApplication {
            allowOverride(true)
            modules(
                networkModule("ReAnime-test"),
                catalogueModule,
                detailsModule,
                module {
                    factory { SavedStateHandle() }
                    single<NetworkClient> {
                        NetworkClient { url ->
                            requests += url
                            Result.Success(detailsHtml(url))
                        }
                    }
                }
            )
        }
        val store = ViewModelStore()
        try {
            val firstRequest = AnimeRequest("moon-garden-abc123", CatalogueMirror.CZ)
            val secondRequest = AnimeRequest("another-show-123", CatalogueMirror.BZ)
            val first = application.koin.get<DetailsScreenViewModel> { parametersOf(firstRequest) }
            val second = application.koin.get<DetailsScreenViewModel> { parametersOf(secondRequest) }
            store.put("first", first)
            store.put("second", second)
            val loadedFirst = first.state.first { !it.isLoading }
            val loadedSecond = second.state.first { !it.isLoading }
            assertEquals(firstRequest.animeId, (loadedFirst.details as DetailsContentState.Content).data.anime.id)
            assertEquals(secondRequest.animeId, (loadedSecond.details as DetailsContentState.Content).data.anime.id)
            assertEquals(0, (loadedFirst.episodes as EpisodesContentState.Content).data.episodes.size)
            assertEquals(
                setOf(
                    "https://reanime.cz/anime/moon-garden-abc123",
                    "https://reanime.cz/watch/moon-garden-abc123?ep=1",
                    "https://reanime.bz/anime/another-show-123",
                    "https://reanime.bz/watch/another-show-123?ep=1"
                ),
                requests.toSet()
            )
        } finally {
            store.clear()
            application.close()
        }
    }

    private fun detailsHtml(url: String): String {
        val id = url.substringAfterLast('/').substringBefore('?')
        val canonical = "<link rel='canonical' href='$url'>"
        return if (url.contains("/anime/")) {
            "$canonical<main><div><div><h1 data-anime-title>$id</h1></div><p>Synopsis</p></div></main>"
        } else {
            "$canonical<main><div><h2>Episodes</h2><span>0</span></div></main>"
        }
    }
}
