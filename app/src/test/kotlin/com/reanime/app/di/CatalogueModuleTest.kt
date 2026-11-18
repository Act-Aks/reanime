package com.reanime.app.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueResult
import com.reanime.feature.browse.BrowseCatalogueState
import com.reanime.feature.browse.BrowseScreenViewModel
import com.reanime.feature.browse.browseModule
import com.reanime.source.reanime.CataloguePageSource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
        val home = CatalogueHome(persistentListOf())
        val application = koinApplication {
            allowOverride(true)
            modules(
                catalogueModule,
                browseModule,
                module {
                    single { SavedStateHandle() }
                    single<CataloguePageSource> { CataloguePageSource { CatalogueResult.Success(home) } }
                }
            )
        }
        val store = ViewModelStore()
        try {
            val viewModel = application.koin.get<BrowseScreenViewModel>()
            store.put("browse", viewModel)
            advanceUntilIdle()

            assertEquals(BrowseCatalogueState.Content(home), viewModel.state.value.catalogue)
        } finally {
            store.clear()
            application.close()
        }
    }
}
