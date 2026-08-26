package com.learning.movies.domain

data class MovieDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val isFavorite: Boolean,
    val genres: List<String>,
    val runtimeMinutes: Int?,
    val tagline: String?,
)
