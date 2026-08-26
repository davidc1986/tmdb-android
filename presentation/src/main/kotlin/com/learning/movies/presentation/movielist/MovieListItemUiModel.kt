package com.learning.movies.presentation.movielist

data class MovieListItemUiModel(
    val id: Int,
    val title: String,
    val displayYear: String,
    val isFavorite: Boolean,
)
