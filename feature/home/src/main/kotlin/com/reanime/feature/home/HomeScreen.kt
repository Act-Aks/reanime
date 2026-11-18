package com.reanime.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueSectionKind
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    onAbout: () -> Unit,
    onAnimeSelected: (String, CatalogueMirror) -> Unit,
    viewModel: HomeScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::onAction, onAbout, onAnimeSelected)
}

@Composable
internal fun HomeScreen(
    state: HomeScreenState,
    onAction: (HomeScreenAction) -> Unit,
    onAbout: () -> Unit,
    onAnimeSelected: (String, CatalogueMirror) -> Unit
) {
    Scaffold { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "header") {
                HomeHeader(state, onAction, onAbout)
            }
            when (val catalogue = state.catalogue) {
                HomeCatalogueState.Loading -> item(key = "loading") {
                    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading_catalogue))
                    }
                }

                is HomeCatalogueState.Error -> item(key = "error") {
                    CatalogueErrorCard(catalogue.reason, onRetry = { onAction(HomeScreenAction.Retry) })
                }

                is HomeCatalogueState.Content -> {
                    if (catalogue.refreshing) {
                        item(key = "refreshing") {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                        }
                    }
                    catalogue.refreshError?.let { error ->
                        item(key = "refresh-error") {
                            CatalogueErrorCard(error, onRetry = { onAction(HomeScreenAction.Retry) })
                        }
                    }
                    catalogueSections(catalogue.home) { onAnimeSelected(it, state.mirror) }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(state: HomeScreenState, onAction: (HomeScreenAction) -> Unit, onAbout: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reanime_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.catalogue_subtitle), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.catalogue_proof_notice), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.source_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogueMirror.entries.forEach { mirror ->
                FilterChip(
                    selected = state.mirror == mirror,
                    onClick = { onAction(HomeScreenAction.SelectMirror(mirror)) },
                    label = { Text(".${mirror.host.substringAfter('.')}") }
                )
            }
        }
        Text(state.mirror.host, style = MaterialTheme.typography.bodySmall)
        val loading = state.catalogue is HomeCatalogueState.Loading ||
            (state.catalogue as? HomeCatalogueState.Content)?.refreshing == true
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAction(HomeScreenAction.Refresh) }, enabled = !loading) {
                Text(stringResource(R.string.refresh))
            }
            TextButton(onClick = onAbout) { Text(stringResource(R.string.about_build)) }
        }
        TextButton(onClick = { onAction(HomeScreenAction.ToggleTechnicalDetails) }) {
            Text(stringResource(if (state.showTechnicalDetails) R.string.hide_details else R.string.show_details))
        }
        if (state.showTechnicalDetails) Text(stringResource(R.string.technical_details))
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.catalogueSections(
    home: CatalogueHome,
    onAnimeSelected: (String) -> Unit
) {
    if (home.sections.isEmpty()) {
        item(key = "empty") {
            Text(stringResource(R.string.empty_catalogue), modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
    items(home.sections, key = { it.kind.name }) { section ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(section.kind.titleResource()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(section.anime, key = { it.id }) { anime -> AnimePoster(anime) { onAnimeSelected(anime.id) } }
            }
        }
    }
}

@Composable
private fun AnimePoster(anime: AnimeSummary, onClick: () -> Unit) {
    val label = stringResource(R.string.open_details, anime.title)
    Column(Modifier.width(136.dp).clickable(onClickLabel = label, role = Role.Button, onClick = onClick)) {
        Card {
            Box(Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT_RATIO)) {
                Text(
                    text = anime.title.take(1),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Center).clearAndSetSemantics { }
                )
                AsyncImage(
                    model = anime.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(anime.title, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

private const val POSTER_ASPECT_RATIO = 0.75f

private fun CatalogueSectionKind.titleResource(): Int = when (this) {
    CatalogueSectionKind.LATEST_EPISODES -> R.string.latest_episodes
    CatalogueSectionKind.NEW_ON_SITE -> R.string.new_on_site
    CatalogueSectionKind.UPCOMING -> R.string.upcoming
}
