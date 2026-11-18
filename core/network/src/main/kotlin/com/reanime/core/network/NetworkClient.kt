package com.reanime.core.network

import com.reanime.core.model.result.Error
import com.reanime.core.model.result.Result

sealed interface NetworkError : Error {
    data object Connection : NetworkError
    data object Timeout : NetworkError
    data object InvalidRequest : NetworkError
    data object UnexpectedResponse : NetworkError
    data object ResponseTooLarge : NetworkError
    data class Http(val statusCode: Int) : NetworkError
}

/** Transport contract. Website endpoints, parsing, and domain errors belong to sources. */
fun interface NetworkClient {
    suspend fun getHtml(url: String): Result<String, NetworkError>
}
