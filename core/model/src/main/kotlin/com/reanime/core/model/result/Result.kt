package com.reanime.core.model.result

/** Marker contract for typed errors. Implement it in the module that owns the failure. */
interface Error

/** Explicit success/failure; cancellation and unexpected programming defects are not values. */
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>
}

inline fun <D, E : Error, R> Result<D, E>.map(transform: (D) -> R): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
}

inline fun <D, E : Error, F : Error> Result<D, E>.mapError(transform: (E) -> F): Result<D, F> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Failure(transform(error))
}
