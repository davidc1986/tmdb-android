package com.learning.movies.domain

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>

    sealed interface Error : Outcome<Nothing> {
        data object NetworkUnavailable : Error
        data object RemoteFailure : Error
        data class Unknown(val cause: Throwable) : Error
    }
}
