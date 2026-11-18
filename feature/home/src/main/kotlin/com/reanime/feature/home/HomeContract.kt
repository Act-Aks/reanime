package com.reanime.feature.home

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror

data class HomeScreenState(
    val mirror: CatalogueMirror = CatalogueMirror.CZ,
    val catalogue: HomeCatalogueState = HomeCatalogueState.Loading,
    val showTechnicalDetails: Boolean = false
)

sealed interface HomeCatalogueState {
    data object Loading : HomeCatalogueState
    data class Content(
        val home: CatalogueHome,
        val refreshing: Boolean = false,
        val refreshError: CatalogueError? = null
    ) : HomeCatalogueState
    data class Error(val reason: CatalogueError) : HomeCatalogueState
}

sealed interface HomeScreenAction {
    data object Refresh : HomeScreenAction
    data object Retry : HomeScreenAction
    data class SelectMirror(val mirror: CatalogueMirror) : HomeScreenAction
    data object ToggleTechnicalDetails : HomeScreenAction
}

internal sealed interface HomeChange {
    data object ToggleTechnicalDetails : HomeChange
    data class Loading(val mirror: CatalogueMirror) : HomeChange
    data class Loaded(val mirror: CatalogueMirror, val home: CatalogueHome) : HomeChange
    data class Failed(val mirror: CatalogueMirror, val error: CatalogueError) : HomeChange
}

internal fun reduce(state: HomeScreenState, change: HomeChange): HomeScreenState = when (change) {
    HomeChange.ToggleTechnicalDetails -> state.copy(showTechnicalDetails = !state.showTechnicalDetails)

    is HomeChange.Loading -> {
        val content = state.catalogue as? HomeCatalogueState.Content
        state.copy(
            mirror = change.mirror,
            catalogue = if (state.mirror == change.mirror && content != null) {
                content.copy(refreshing = true, refreshError = null)
            } else {
                HomeCatalogueState.Loading
            }
        )
    }

    is HomeChange.Loaded -> if (state.mirror == change.mirror) {
        state.copy(catalogue = HomeCatalogueState.Content(change.home))
    } else {
        state
    }

    is HomeChange.Failed -> if (state.mirror == change.mirror) {
        val content = state.catalogue as? HomeCatalogueState.Content
        state.copy(
            catalogue = content?.copy(refreshing = false, refreshError = change.error)
                ?: HomeCatalogueState.Error(change.error)
        )
    } else {
        state
    }
}
