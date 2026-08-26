package com.learning.movies.presentation.moviedetail

data class MovieDetailUiModel(
    val id: Int,
    val title: String,
    val tagline: String?,
    val subtitle: String,
    val genresText: String,
    val overview: String,
    val isFavorite: Boolean,
)
