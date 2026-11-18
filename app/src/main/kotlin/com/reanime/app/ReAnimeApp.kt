package com.reanime.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.reanime.feature.details.DetailsRoute
import com.reanime.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
internal data object Home : NavKey

@Serializable
internal data object About : NavKey

@Composable
fun ReAnimeApp() {
    val backStack = rememberNavBackStack(Home)
    val onBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeRoute(
                    onAbout = { if (backStack.lastOrNull() == Home) backStack.add(About) },
                    onAnimeSelected = { id, mirror ->
                        val destination = AnimeDetailsKey(id, mirror.name)
                        if (backStack.lastOrNull() == Home) backStack.add(destination)
                    }
                )
            }
            entry<About> { InfoScreen(R.string.about_title, R.string.about_description, onBack) }
            entry<AnimeDetailsKey> { key ->
                val request = key.requestOrNull()
                if (request == null) {
                    InfoScreen(R.string.invalid_destination, R.string.invalid_destination_message, onBack)
                } else {
                    DetailsRoute(request, onBack)
                }
            }
        }
    )
}

@Composable
private fun InfoScreen(title: Int, message: Int, onBack: () -> Unit) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(title))
            Text(stringResource(message))
            Button(onClick = onBack) { Text(stringResource(R.string.back)) }
        }
    }
}
