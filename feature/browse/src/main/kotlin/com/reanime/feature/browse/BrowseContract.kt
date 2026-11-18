package com.reanime.feature.browse

data class BrowseScreenState(val showTechnicalDetails: Boolean = false)

sealed interface BrowseScreenAction {
    data object ToggleTechnicalDetails : BrowseScreenAction
}

internal fun reduce(state: BrowseScreenState, action: BrowseScreenAction): BrowseScreenState = when (action) {
    BrowseScreenAction.ToggleTechnicalDetails -> state.copy(showTechnicalDetails = !state.showTechnicalDetails)
}
