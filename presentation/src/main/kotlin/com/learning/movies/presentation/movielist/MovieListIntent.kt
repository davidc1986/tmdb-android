package com.learning.movies.presentation.movielist

sealed interface MovieListIntent {
    data object LoadMovies : MovieListIntent
    data object Refresh : MovieListIntent
    data object Retry : MovieListIntent
    data class ToggleFavorite(val movie: MovieListItemUiModel) : MovieListIntent
    data class SelectMovie(val movie: MovieListItemUiModel) : MovieListIntent
}
