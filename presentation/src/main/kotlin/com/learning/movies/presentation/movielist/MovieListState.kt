package com.learning.movies.presentation.movielist

sealed interface MovieListState {
    data object Loading : MovieListState

    data class Content(
        val movies: List<MovieListItemUiModel>,
        val isRefreshing: Boolean = false,
    ) : MovieListState

    data class Error(val message: String) : MovieListState
}
