package com.reanime.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val browseModule = module {
    viewModelOf(::BrowseScreenViewModel)
}

class BrowseScreenViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(
        BrowseScreenState(showTechnicalDetails = savedStateHandle[DETAILS_KEY] ?: false)
    )
    val state = mutableState.asStateFlow()

    // Called on the main thread by the UI. No asynchronous operations exist yet.
    fun onAction(action: BrowseScreenAction) {
        val next = reduce(mutableState.value, action)
        mutableState.value = next
        savedStateHandle[DETAILS_KEY] = next.showTechnicalDetails
    }

    private companion object {
        const val DETAILS_KEY = "showTechnicalDetails"
    }
}
