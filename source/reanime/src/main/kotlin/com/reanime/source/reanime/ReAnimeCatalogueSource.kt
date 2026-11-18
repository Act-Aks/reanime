package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.NetworkError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface CataloguePageSource {
    suspend fun home(mirror: CatalogueMirror): Result<CatalogueHome, CatalogueError>
}

class ReAnimeCatalogueSource(
    private val network: NetworkClient,
    private val parser: ReAnimeHomeParser = ReAnimeHomeParser(),
    private val parsingDispatcher: CoroutineDispatcher = Dispatchers.Default
) : CataloguePageSource {
    override suspend fun home(mirror: CatalogueMirror): Result<CatalogueHome, CatalogueError> =
        when (val response = network.getHtml("https://${mirror.host}/home")) {
            is Result.Success -> withContext(parsingDispatcher) { parser.parse(response.data, mirror) }
            is Result.Failure -> Result.Failure(response.error.toCatalogueError())
        }

    private fun NetworkError.toCatalogueError(): CatalogueError = when (this) {
        NetworkError.Connection -> CatalogueError.NETWORK

        NetworkError.Timeout -> CatalogueError.TIMEOUT

        NetworkError.InvalidRequest, NetworkError.UnexpectedResponse -> CatalogueError.UNEXPECTED_RESPONSE

        NetworkError.ResponseTooLarge -> CatalogueError.RESPONSE_TOO_LARGE

        is NetworkError.Http -> when (statusCode) {
            in ACCESS_DENIED_STATUSES -> CatalogueError.ACCESS_BLOCKED
            RATE_LIMITED_STATUS -> CatalogueError.RATE_LIMITED
            in SERVER_ERROR_STATUSES -> CatalogueError.SOURCE_UNAVAILABLE
            else -> CatalogueError.UNEXPECTED_RESPONSE
        }
    }

    private companion object {
        val ACCESS_DENIED_STATUSES = setOf(401, 403)
        const val RATE_LIMITED_STATUS = 429
        val SERVER_ERROR_STATUSES = 500..599
    }
}
