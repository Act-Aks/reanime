package com.reanime.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.Episode
import com.reanime.core.model.anime.EpisodeLanguage
import com.reanime.core.model.anime.EpisodeListing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailsRoute(
    request: AnimeRequest,
    onBack: () -> Unit,
    viewModel: DetailsScreenViewModel = koinViewModel(
        key = "${request.mirror.name}:${request.animeId}"
    ) { parametersOf(request) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailsScreen(state, request.mirror.host, viewModel::onAction, onBack)
}

@Composable
internal fun DetailsScreen(
    state: DetailsScreenState,
    sourceHost: String,
    onAction: (DetailsScreenAction) -> Unit,
    onBack: () -> Unit
) {
    Scaffold { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "navigation") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                    TextButton(onClick = { onAction(DetailsScreenAction.Refresh) }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.refresh))
                    }
                }
            }
            item(key = "source") { Text(sourceHost, style = MaterialTheme.typography.labelMedium) }
            item(key = "metadata-notice") { Text(stringResource(R.string.metadata_notice)) }
            detailsContent(state, onAction)
            episodeContent(state, onAction)
        }
    }
}

private fun LazyListScope.detailsContent(state: DetailsScreenState, onAction: (DetailsScreenAction) -> Unit) {
    when (val details = state.details) {
        DetailsContentState.Loading -> item(key = "details-loading") {
            CircularProgressIndicator()
            Text(stringResource(R.string.loading_details))
        }

        is DetailsContentState.Error -> item(key = "details-error") {
            DetailsFailure(details.reason.messageResource()) { onAction(DetailsScreenAction.RetryDetails) }
        }

        is DetailsContentState.Content -> {
            if (details.refreshing) {
                item(key = "details-refreshing") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            details.refreshError?.let { error ->
                item(key = "details-refresh-error") {
                    DetailsFailure(error.messageResource()) { onAction(DetailsScreenAction.RetryDetails) }
                }
            }
            item(key = "details-metadata") { AnimeMetadata(details.data, state.synopsisExpanded, onAction) }
        }
    }
}

@Composable
private fun AnimeMetadata(details: AnimeDetails, expanded: Boolean, onAction: (DetailsScreenAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        details.anime.posterUrl?.let { poster ->
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(144.dp).aspectRatio(POSTER_RATIO)
            )
        }
        Text(
            details.anime.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics {
                heading()
            }
        )
        details.alternativeTitle?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
        details.synopsis?.let { synopsis ->
            Text(synopsis, maxLines = if (expanded) Int.MAX_VALUE else 6, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { onAction(DetailsScreenAction.ToggleSynopsis) }) {
                Text(stringResource(if (expanded) R.string.show_less else R.string.show_more))
            }
        }
        if (details.genres.isNotEmpty()) {
            Text(
                details.genres.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        details.facts.forEach { fact ->
            Column {
                Text(fact.label, style = MaterialTheme.typography.labelMedium)
                Text(fact.value)
            }
        }
    }
}

private fun LazyListScope.episodeContent(state: DetailsScreenState, onAction: (DetailsScreenAction) -> Unit) {
    item(key = "episode-heading") {
        Text(
            stringResource(R.string.episodes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics {
                heading()
            }
        )
    }
    when (val episodes = state.episodes) {
        EpisodesContentState.Loading -> item(key = "episodes-loading") {
            CircularProgressIndicator()
            Text(stringResource(R.string.loading_episodes))
        }

        is EpisodesContentState.Error -> item(key = "episodes-error") {
            DetailsFailure(episodes.reason.messageResource()) { onAction(DetailsScreenAction.RetryEpisodes) }
        }

        is EpisodesContentState.Content -> {
            if (episodes.refreshing) {
                item(key = "episodes-refreshing") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            episodes.refreshError?.let { error ->
                item(key = "episodes-refresh-error") {
                    DetailsFailure(error.messageResource()) { onAction(DetailsScreenAction.RetryEpisodes) }
                }
            }
            if (episodes.data.isPartial) {
                item(key = "partial-listing") {
                    PartialListingNotice(episodes.data)
                }
            }
            item(key = "episode-filters") { EpisodeFilters(state.filter, onAction) }
            val visible = state.visibleEpisodes
            if (visible.isEmpty()) {
                item(key = "no-episodes") {
                    Text(
                        stringResource(
                            if (episodes.data.episodes.isEmpty()) R.string.no_episodes else R.string.no_filter_matches
                        )
                    )
                }
            }
            items(visible, key = { "episode-${it.number}" }) { episode ->
                EpisodeCard(episode, state.selectedEpisode == episode.number) {
                    onAction(DetailsScreenAction.SelectEpisode(episode.number))
                }
            }
        }
    }
}

@Composable
private fun PartialListingNotice(listing: EpisodeListing) {
    val total = listing.reportedCount ?: return
    Text(pluralStringResource(R.plurals.partial_listing, total, listing.episodes.size, total))
}

@Composable
private fun EpisodeFilters(selected: EpisodeFilter, onAction: (DetailsScreenAction) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(EpisodeFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onAction(DetailsScreenAction.FilterEpisodes(filter)) },
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                EpisodeFilter.ALL -> R.string.all
                                EpisodeFilter.SUB -> R.string.sub
                                EpisodeFilter.DUB -> R.string.dub
                            }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun EpisodeCard(episode: Episode, expanded: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { stateDescription = description }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.episode_title, episode.number, episode.title),
                style = MaterialTheme.typography.titleSmall
            )
            val labels = episode.languages.map { language ->
                stringResource(
                    if (language == EpisodeLanguage.SUB) R.string.sub else R.string.dub
                )
            }
            if (labels.isNotEmpty()) Text(stringResource(R.string.site_tags, labels.joinToString(" · ")))
            if (expanded) {
                Text(episode.synopsis ?: stringResource(R.string.no_synopsis))
                episode.aired?.let { Text(stringResource(R.string.aired, it)) }
            } else {
                Text(stringResource(R.string.episode_info), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private const val POSTER_RATIO = 0.75f
