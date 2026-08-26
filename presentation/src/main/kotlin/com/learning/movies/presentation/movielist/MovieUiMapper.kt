package com.learning.movies.presentation.movielist

import com.learning.movies.domain.Movie

fun Movie.toMovieListItemUiModel(): MovieListItemUiModel = MovieListItemUiModel(
    id = id,
    title = title,
    displayYear = releaseDate?.take(4) ?: "—",
    isFavorite = isFavorite,
)
