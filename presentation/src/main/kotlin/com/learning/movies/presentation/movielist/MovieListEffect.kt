package com.learning.movies.presentation.movielist

sealed interface MovieListEffect {
    data class ShowError(val message: String) : MovieListEffect
    data class NavigateToDetail(val movieId: Int) : MovieListEffect
}
