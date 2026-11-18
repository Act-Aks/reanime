package com.reanime.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueRepository
import com.reanime.core.model.result.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeScreenViewModel)
}

class HomeScreenViewModel(private val savedStateHandle: SavedStateHandle, private val repository: CatalogueRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(
        HomeScreenState(
            mirror = CatalogueMirror.entries.firstOrNull { it.name == savedStateHandle.get<String>(MIRROR_KEY) }
                ?: CatalogueMirror.CZ,
            showTechnicalDetails = savedStateHandle[DETAILS_KEY] ?: false
        )
    )
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var requestId = 0L

    init {
        load(mutableState.value.mirror, refresh = false)
    }

    // Actions and state transitions are confined to the main thread.
    fun onAction(action: HomeScreenAction) {
        when (action) {
            HomeScreenAction.Refresh, HomeScreenAction.Retry -> load(state.value.mirror, refresh = true)

            is HomeScreenAction.SelectMirror -> if (action.mirror != state.value.mirror) {
                savedStateHandle[MIRROR_KEY] = action.mirror.name
                load(action.mirror, refresh = false)
            }

            HomeScreenAction.ToggleTechnicalDetails -> {
                change(HomeChange.ToggleTechnicalDetails)
                savedStateHandle[DETAILS_KEY] = state.value.showTechnicalDetails
            }
        }
    }

    private fun load(mirror: CatalogueMirror, refresh: Boolean) {
        request?.cancel()
        val currentRequest = ++requestId
        change(HomeChange.Loading(mirror))
        request = viewModelScope.launch {
            val result = repository.home(mirror, refresh)
            // Generation guard also protects against a misbehaving, cancellation-ignoring source.
            if (currentRequest != requestId) return@launch
            when (result) {
                is Result.Success -> change(HomeChange.Loaded(mirror, result.data))
                is Result.Failure -> change(HomeChange.Failed(mirror, result.error))
            }
        }
    }

    private fun change(change: HomeChange) {
        mutableState.value = reduce(mutableState.value, change)
    }

    private companion object {
        const val DETAILS_KEY = "showTechnicalDetails"
        const val MIRROR_KEY = "catalogueMirror"
    }
}
