package com.reanime.app.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueSection
import com.reanime.core.model.catalogue.CatalogueSectionKind
import com.reanime.core.model.result.Result
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.di.networkModule
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
}
