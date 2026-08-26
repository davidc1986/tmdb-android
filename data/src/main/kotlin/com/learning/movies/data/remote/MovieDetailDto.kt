package com.learning.movies.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailDto(
    val id: Int,
    val title: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<GenreDto> = emptyList(),
    val runtime: Int? = null,
    val tagline: String? = null,
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String,
)
