package com.reanime.feature.browse

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror

data class BrowseScreenState(
    val mirror: CatalogueMirror = CatalogueMirror.CZ,
    val catalogue: BrowseCatalogueState = BrowseCatalogueState.Loading,
    val showTechnicalDetails: Boolean = false
)

sealed interface BrowseCatalogueState {
    data object Loading : BrowseCatalogueState
    data class Content(
        val home: CatalogueHome,
        val refreshing: Boolean = false,
        val refreshError: CatalogueError? = null
    ) : BrowseCatalogueState
    data class Error(val reason: CatalogueError) : BrowseCatalogueState
}

sealed interface BrowseScreenAction {
    data object Refresh : BrowseScreenAction
    data object Retry : BrowseScreenAction
    data class SelectMirror(val mirror: CatalogueMirror) : BrowseScreenAction
    data object ToggleTechnicalDetails : BrowseScreenAction
}

internal sealed interface BrowseChange {
    data object ToggleTechnicalDetails : BrowseChange
    data class Loading(val mirror: CatalogueMirror) : BrowseChange
    data class Loaded(val mirror: CatalogueMirror, val home: CatalogueHome) : BrowseChange
    data class Failed(val mirror: CatalogueMirror, val error: CatalogueError) : BrowseChange
}

internal fun reduce(state: BrowseScreenState, change: BrowseChange): BrowseScreenState = when (change) {
    BrowseChange.ToggleTechnicalDetails -> state.copy(showTechnicalDetails = !state.showTechnicalDetails)

    is BrowseChange.Loading -> {
        val content = state.catalogue as? BrowseCatalogueState.Content
        state.copy(
            mirror = change.mirror,
            catalogue = if (state.mirror == change.mirror && content != null) {
                content.copy(refreshing = true, refreshError = null)
            } else {
                BrowseCatalogueState.Loading
            }
        )
    }

    is BrowseChange.Loaded -> if (state.mirror == change.mirror) {
        state.copy(catalogue = BrowseCatalogueState.Content(change.home))
    } else {
        state
    }

    is BrowseChange.Failed -> if (state.mirror == change.mirror) {
        val content = state.catalogue as? BrowseCatalogueState.Content
        state.copy(
            catalogue = content?.copy(refreshing = false, refreshError = change.error)
                ?: BrowseCatalogueState.Error(change.error)
        )
    } else {
        state
    }
}
