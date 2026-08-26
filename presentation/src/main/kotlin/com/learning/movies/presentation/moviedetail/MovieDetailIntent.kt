package com.learning.movies.presentation.moviedetail

sealed interface MovieDetailIntent {
    data object Retry : MovieDetailIntent
    data object ToggleFavorite : MovieDetailIntent
}
