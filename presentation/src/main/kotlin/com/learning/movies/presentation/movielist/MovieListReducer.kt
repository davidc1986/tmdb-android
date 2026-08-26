package com.learning.movies.presentation.movielist

fun reduceMovieListState(
    current: MovieListState,
    result: MovieListResult,
): MovieListState = when (result) {
    is MovieListResult.Loading -> when (current) {
        is MovieListState.Content -> current.copy(isRefreshing = true)
        else -> MovieListState.Loading
    }

    is MovieListResult.Loaded -> MovieListState.Content(movies = result.movies)

    is MovieListResult.Failed -> when (current) {
        is MovieListState.Content -> current.copy(isRefreshing = false)
        else -> MovieListState.Error(result.message)
    }
}
