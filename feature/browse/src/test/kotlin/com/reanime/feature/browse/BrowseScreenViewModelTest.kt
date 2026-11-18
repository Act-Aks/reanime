package com.reanime.feature.browse

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseScreenViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `action publishes the reduced state`() {
        val viewModel = BrowseScreenViewModel(SavedStateHandle())

        viewModel.onAction(BrowseScreenAction.ToggleTechnicalDetails)

        assertTrue(viewModel.state.value.showTechnicalDetails)
    }

    @Test
    fun `state restores when ViewModel is recreated from saved input`() {
        val handle = SavedStateHandle()
        BrowseScreenViewModel(handle).onAction(BrowseScreenAction.ToggleTechnicalDetails)

        val restored = BrowseScreenViewModel(handle)

        assertTrue(restored.state.value.showTechnicalDetails)
        restored.onAction(BrowseScreenAction.ToggleTechnicalDetails)
        assertFalse(restored.state.value.showTechnicalDetails)
    }
}
