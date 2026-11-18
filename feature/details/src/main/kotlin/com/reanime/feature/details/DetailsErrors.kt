package com.reanime.feature.details

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.EpisodeError

@Composable
internal fun DetailsFailure(message: Int, onRetry: () -> Unit) {
    Column {
        Text(stringResource(message))
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

internal fun DetailsError.messageResource(): Int = when (this) {
    DetailsError.INVALID_ID -> R.string.invalid_id
    DetailsError.NETWORK -> R.string.network_error
    DetailsError.TIMEOUT -> R.string.timeout_error
    DetailsError.ACCESS_BLOCKED -> R.string.blocked_error
    DetailsError.RATE_LIMITED -> R.string.rate_error
    DetailsError.NOT_FOUND -> R.string.details_missing
    DetailsError.SOURCE_UNAVAILABLE -> R.string.server_error
    DetailsError.UNEXPECTED_RESPONSE -> R.string.response_error
    DetailsError.RESPONSE_TOO_LARGE -> R.string.size_error
    DetailsError.LAYOUT_CHANGED -> R.string.layout_error
    DetailsError.IDENTITY_MISMATCH -> R.string.identity_error
}

internal fun EpisodeError.messageResource(): Int = when (this) {
    EpisodeError.INVALID_ID -> R.string.invalid_id
    EpisodeError.NETWORK -> R.string.network_error
    EpisodeError.TIMEOUT -> R.string.timeout_error
    EpisodeError.ACCESS_BLOCKED -> R.string.blocked_error
    EpisodeError.RATE_LIMITED -> R.string.rate_error
    EpisodeError.NOT_FOUND -> R.string.episodes_missing
    EpisodeError.SOURCE_UNAVAILABLE -> R.string.server_error
    EpisodeError.UNEXPECTED_RESPONSE -> R.string.response_error
    EpisodeError.RESPONSE_TOO_LARGE -> R.string.size_error
    EpisodeError.LAYOUT_CHANGED -> R.string.layout_error
    EpisodeError.IDENTITY_MISMATCH -> R.string.identity_error
}
