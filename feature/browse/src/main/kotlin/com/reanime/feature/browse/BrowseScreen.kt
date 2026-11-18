package com.reanime.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun BrowseRoute(onAbout: () -> Unit, viewModel: BrowseScreenViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BrowseScreen(state = state, onAction = viewModel::onAction, onAbout = onAbout)
}

@Composable
private fun BrowseScreen(state: BrowseScreenState, onAction: (BrowseScreenAction) -> Unit, onAbout: () -> Unit) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.reanime_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.foundation_status), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.source_not_connected))
            Button(onClick = { onAction(BrowseScreenAction.ToggleTechnicalDetails) }) {
                Text(stringResource(if (state.showTechnicalDetails) R.string.hide_details else R.string.show_details))
            }
            if (state.showTechnicalDetails) {
                Text(stringResource(R.string.technical_details))
            }
            TextButton(onClick = onAbout) { Text(stringResource(R.string.about_build)) }
        }
    }
}
