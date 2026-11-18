package com.reanime.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reanime.core.model.catalogue.CatalogueError

@Composable
internal fun CatalogueErrorCard(error: CatalogueError, onRetry: () -> Unit) {
    Card(Modifier.padding(horizontal = 24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(error.messageResource()))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

private fun CatalogueError.messageResource(): Int = when (this) {
    CatalogueError.NETWORK -> R.string.error_network
    CatalogueError.TIMEOUT -> R.string.error_timeout
    CatalogueError.ACCESS_BLOCKED -> R.string.error_blocked
    CatalogueError.RATE_LIMITED -> R.string.error_rate_limited
    CatalogueError.SOURCE_UNAVAILABLE -> R.string.error_unavailable
    CatalogueError.UNEXPECTED_RESPONSE -> R.string.error_response
    CatalogueError.RESPONSE_TOO_LARGE -> R.string.error_size
    CatalogueError.LAYOUT_CHANGED -> R.string.error_layout
}
