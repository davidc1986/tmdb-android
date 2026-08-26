package com.learning.movies.presentation.movielist

sealed interface MovieListResult {
    data object Loading : MovieListResult
    data class Loaded(val movies: List<MovieListItemUiModel>) : MovieListResult
    data class Failed(val message: String) : MovieListResult
}
