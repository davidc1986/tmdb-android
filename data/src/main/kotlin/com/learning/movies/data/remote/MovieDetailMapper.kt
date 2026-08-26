package com.learning.movies.data.remote

import com.learning.movies.domain.MovieDetail

fun MovieDetailDto.toMovieDetail(): MovieDetail = MovieDetail(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    isFavorite = false,
    genres = genres.map { it.name },
    runtimeMinutes = runtime,
    tagline = tagline?.takeIf { it.isNotBlank() },
)
