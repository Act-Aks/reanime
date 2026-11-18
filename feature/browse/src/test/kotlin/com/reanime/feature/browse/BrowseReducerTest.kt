package com.reanime.feature.browse

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseReducerTest {
    @Test
    fun `technical details start hidden`() {
        assertFalse(BrowseScreenState().showTechnicalDetails)
    }

    @Test
    fun `toggle creates new state without changing the old state`() {
        val original = BrowseScreenState()
        val next = reduce(original, BrowseChange.ToggleTechnicalDetails)

        assertFalse(original.showTechnicalDetails)
        assertTrue(next.showTechnicalDetails)
    }

    @Test
    fun `two toggles restore the initial state`() {
        val original = BrowseScreenState()
        val next = reduce(
            reduce(original, BrowseChange.ToggleTechnicalDetails),
            BrowseChange.ToggleTechnicalDetails
        )

        assertEquals(original, next)
    }

    @Test
    fun `refresh retains visible data and failure ends the progress indicator`() {
        val content = BrowseCatalogueState.Content(CatalogueHome(persistentListOf()))
        val original = BrowseScreenState(catalogue = content)
        val loading = reduce(original, BrowseChange.Loading(CatalogueMirror.CZ))
        assertTrue((loading.catalogue as BrowseCatalogueState.Content).refreshing)

        val failed = reduce(loading, BrowseChange.Failed(CatalogueMirror.CZ, CatalogueError.NETWORK))
        val retained = failed.catalogue as BrowseCatalogueState.Content
        assertEquals(content.home, retained.home)
        assertFalse(retained.refreshing)
        assertEquals(CatalogueError.NETWORK, retained.refreshError)
    }

    @Test
    fun `switching mirror clears content and ignores results from the previous mirror`() {
        val content = CatalogueHome(persistentListOf())
        val initial = BrowseScreenState(catalogue = BrowseCatalogueState.Content(content))
        val switched = reduce(initial, BrowseChange.Loading(CatalogueMirror.BZ))

        assertEquals(BrowseCatalogueState.Loading, switched.catalogue)
        assertEquals(switched, reduce(switched, BrowseChange.Loaded(CatalogueMirror.CZ, content)))
    }
}
