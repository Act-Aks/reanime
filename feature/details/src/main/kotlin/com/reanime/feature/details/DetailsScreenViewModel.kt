package com.reanime.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reanime.core.model.anime.AnimeDetailsRepository
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.EpisodeRepository
import com.reanime.core.model.result.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val detailsModule = module {
    viewModel { (request: AnimeRequest) -> DetailsScreenViewModel(request, get(), get(), get()) }
}

class DetailsScreenViewModel(
    val request: AnimeRequest,
    private val savedStateHandle: SavedStateHandle,
    private val detailsRepository: AnimeDetailsRepository,
    private val episodeRepository: EpisodeRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        DetailsScreenState(
            filter = EpisodeFilter.entries.firstOrNull { it.name == savedStateHandle.get<String>(FILTER_KEY) }
                ?: EpisodeFilter.ALL,
            selectedEpisode = savedStateHandle[EPISODE_KEY],
            synopsisExpanded = savedStateHandle[SYNOPSIS_KEY] ?: false
        )
    )
    val state = mutableState.asStateFlow()
    private var detailsJob: Job? = null
    private var episodesJob: Job? = null
    private var detailsRequestId = 0L
    private var episodeRequestId = 0L

    init {
        loadDetails()
        loadEpisodes()
    }

    fun onAction(action: DetailsScreenAction) {
        when (action) {
            DetailsScreenAction.Refresh -> {
                loadDetails()
                loadEpisodes()
            }

            DetailsScreenAction.RetryDetails -> loadDetails()

            DetailsScreenAction.RetryEpisodes -> loadEpisodes()

            DetailsScreenAction.ToggleSynopsis -> change(DetailsChange.ToggleSynopsis)

            is DetailsScreenAction.FilterEpisodes -> change(DetailsChange.FilterEpisodes(action.filter))

            is DetailsScreenAction.SelectEpisode -> change(DetailsChange.SelectEpisode(action.number))
        }
    }

    private fun loadDetails() {
        detailsJob?.cancel()
        val current = ++detailsRequestId
        change(DetailsChange.LoadingDetails)
        detailsJob = viewModelScope.launch {
            val result = detailsRepository.details(request)
            if (current != detailsRequestId) return@launch
            when (result) {
                is Result.Success -> change(DetailsChange.LoadedDetails(result.data))
                is Result.Failure -> change(DetailsChange.FailedDetails(result.error))
            }
        }
    }

    private fun loadEpisodes() {
        episodesJob?.cancel()
        val current = ++episodeRequestId
        change(DetailsChange.LoadingEpisodes)
        episodesJob = viewModelScope.launch {
            val result = episodeRepository.episodes(request)
            if (current != episodeRequestId) return@launch
            when (result) {
                is Result.Success -> change(DetailsChange.LoadedEpisodes(result.data))
                is Result.Failure -> change(DetailsChange.FailedEpisodes(result.error))
            }
        }
    }

    private fun change(change: DetailsChange) {
        val next = reduce(mutableState.value, change)
        mutableState.value = next
        savedStateHandle[FILTER_KEY] = next.filter.name
        savedStateHandle[EPISODE_KEY] = next.selectedEpisode
        savedStateHandle[SYNOPSIS_KEY] = next.synopsisExpanded
    }

    private companion object {
        const val FILTER_KEY = "episodeFilter"
        const val EPISODE_KEY = "selectedEpisode"
        const val SYNOPSIS_KEY = "synopsisExpanded"
    }
}
