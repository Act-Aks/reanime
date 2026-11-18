package com.reanime.feature.details

import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.Episode
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.anime.EpisodeLanguage
import com.reanime.core.model.anime.EpisodeListing
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

enum class EpisodeFilter { ALL, SUB, DUB }

data class DetailsScreenState(
    val details: DetailsContentState = DetailsContentState.Loading,
    val episodes: EpisodesContentState = EpisodesContentState.Loading,
    val filter: EpisodeFilter = EpisodeFilter.ALL,
    val selectedEpisode: String? = null,
    val synopsisExpanded: Boolean = false
) {
    val isLoading: Boolean get() = details is DetailsContentState.Loading || episodes is EpisodesContentState.Loading ||
        (details as? DetailsContentState.Content)?.refreshing == true ||
        (episodes as? EpisodesContentState.Content)?.refreshing == true

    val visibleEpisodes: PersistentList<Episode>
        get() {
            val listing = (episodes as? EpisodesContentState.Content)?.data ?: return persistentListOf()
            return when (filter) {
                EpisodeFilter.ALL -> listing.episodes
                EpisodeFilter.SUB -> listing.episodes.filter { EpisodeLanguage.SUB in it.languages }.toPersistentList()
                EpisodeFilter.DUB -> listing.episodes.filter { EpisodeLanguage.DUB in it.languages }.toPersistentList()
            }
        }
}

sealed interface DetailsContentState {
    data object Loading : DetailsContentState
    data class Content(
        val data: AnimeDetails,
        val refreshing: Boolean = false,
        val refreshError: DetailsError? = null
    ) : DetailsContentState
    data class Error(val reason: DetailsError) : DetailsContentState
}

sealed interface EpisodesContentState {
    data object Loading : EpisodesContentState
    data class Content(
        val data: EpisodeListing,
        val refreshing: Boolean = false,
        val refreshError: EpisodeError? = null
    ) : EpisodesContentState
    data class Error(val reason: EpisodeError) : EpisodesContentState
}

sealed interface DetailsScreenAction {
    data object Refresh : DetailsScreenAction
    data object RetryDetails : DetailsScreenAction
    data object RetryEpisodes : DetailsScreenAction
    data object ToggleSynopsis : DetailsScreenAction
    data class FilterEpisodes(val filter: EpisodeFilter) : DetailsScreenAction
    data class SelectEpisode(val number: String) : DetailsScreenAction
}

internal sealed interface DetailsChange {
    data object LoadingDetails : DetailsChange
    data class LoadedDetails(val data: AnimeDetails) : DetailsChange
    data class FailedDetails(val error: DetailsError) : DetailsChange
    data object LoadingEpisodes : DetailsChange
    data class LoadedEpisodes(val data: EpisodeListing) : DetailsChange
    data class FailedEpisodes(val error: EpisodeError) : DetailsChange
    data object ToggleSynopsis : DetailsChange
    data class FilterEpisodes(val filter: EpisodeFilter) : DetailsChange
    data class SelectEpisode(val number: String) : DetailsChange
}

internal fun reduce(state: DetailsScreenState, change: DetailsChange): DetailsScreenState = when (change) {
    DetailsChange.LoadingDetails -> state.copy(
        details = (state.details as? DetailsContentState.Content)
            ?.copy(refreshing = true, refreshError = null) ?: DetailsContentState.Loading
    )

    is DetailsChange.LoadedDetails -> state.copy(details = DetailsContentState.Content(change.data))

    is DetailsChange.FailedDetails -> state.copy(
        details = (state.details as? DetailsContentState.Content)
            ?.copy(refreshing = false, refreshError = change.error) ?: DetailsContentState.Error(change.error)
    )

    DetailsChange.LoadingEpisodes -> state.copy(
        episodes = (state.episodes as? EpisodesContentState.Content)
            ?.copy(refreshing = true, refreshError = null) ?: EpisodesContentState.Loading
    )

    is DetailsChange.LoadedEpisodes -> state.copy(
        episodes = EpisodesContentState.Content(change.data),
        selectedEpisode = state.selectedEpisode?.takeIf { selected ->
            change.data.episodes.any { it.number == selected }
        }
    )

    is DetailsChange.FailedEpisodes -> state.copy(
        episodes = (state.episodes as? EpisodesContentState.Content)
            ?.copy(refreshing = false, refreshError = change.error) ?: EpisodesContentState.Error(change.error)
    )

    DetailsChange.ToggleSynopsis -> state.copy(synopsisExpanded = !state.synopsisExpanded)

    is DetailsChange.FilterEpisodes -> state.copy(filter = change.filter, selectedEpisode = null)

    is DetailsChange.SelectEpisode -> selectEpisode(state, change.number)
}

private fun selectEpisode(state: DetailsScreenState, number: String): DetailsScreenState =
    if (state.visibleEpisodes.any { it.number == number }) {
        state.copy(selectedEpisode = number.takeUnless { it == state.selectedEpisode })
    } else {
        state
    }
