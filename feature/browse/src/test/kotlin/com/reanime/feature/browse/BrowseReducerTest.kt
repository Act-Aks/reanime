package com.reanime.feature.browse

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
        val next = reduce(original, BrowseScreenAction.ToggleTechnicalDetails)

        assertFalse(original.showTechnicalDetails)
        assertTrue(next.showTechnicalDetails)
    }

    @Test
    fun `two toggles restore the initial state`() {
        val original = BrowseScreenState()
        val next = reduce(
            reduce(original, BrowseScreenAction.ToggleTechnicalDetails),
            BrowseScreenAction.ToggleTechnicalDetails
        )

        assertEquals(original, next)
    }
}
