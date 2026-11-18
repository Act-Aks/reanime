package com.reanime.feature.home

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {
    @Test
    fun `technical details start hidden`() {
        assertFalse(HomeScreenState().showTechnicalDetails)
    }

    @Test
    fun `toggle creates new state without changing the old state`() {
        val original = HomeScreenState()
        val next = reduce(original, HomeChange.ToggleTechnicalDetails)
        assertFalse(original.showTechnicalDetails)
        assertTrue(next.showTechnicalDetails)
    }

    @Test
    fun `two toggles restore the initial state`() {
        val original = HomeScreenState()
        assertEquals(
            original,
            reduce(reduce(original, HomeChange.ToggleTechnicalDetails), HomeChange.ToggleTechnicalDetails)
        )
    }

    @Test
    fun `refresh retains visible data and failure ends the progress indicator`() {
        val content = HomeCatalogueState.Content(CatalogueHome(persistentListOf()))
        val original = HomeScreenState(catalogue = content)
        val loading = reduce(original, HomeChange.Loading(CatalogueMirror.CZ))
        assertTrue((loading.catalogue as HomeCatalogueState.Content).refreshing)
        val failed = reduce(loading, HomeChange.Failed(CatalogueMirror.CZ, CatalogueError.NETWORK))
        val retained = failed.catalogue as HomeCatalogueState.Content
        assertEquals(content.home, retained.home)
        assertFalse(retained.refreshing)
        assertEquals(CatalogueError.NETWORK, retained.refreshError)
    }

    @Test
    fun `switching mirror clears content and ignores results from the previous mirror`() {
        val content = CatalogueHome(persistentListOf())
        val initial = HomeScreenState(catalogue = HomeCatalogueState.Content(content))
        val switched = reduce(initial, HomeChange.Loading(CatalogueMirror.BZ))
        assertEquals(HomeCatalogueState.Loading, switched.catalogue)
        assertEquals(switched, reduce(switched, HomeChange.Loaded(CatalogueMirror.CZ, content)))
    }
}
