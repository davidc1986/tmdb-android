package com.learning.movies.data.remote

import com.learning.movies.domain.Movie

fun MovieDto.toMovie(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    isFavorite = false,
)
