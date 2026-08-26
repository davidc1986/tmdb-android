package com.learning.movies.presentation.moviedetail

import com.learning.movies.domain.MovieDetail

fun MovieDetail.toMovieDetailUiModel(): MovieDetailUiModel {
    val year = releaseDate?.take(4) ?: "—"
    val runtime = runtimeMinutes?.let { "${it}m" }
    return MovieDetailUiModel(
        id = id,
        title = title,
        tagline = tagline,
        subtitle = listOfNotNull(year, runtime).joinToString(" · "),
        genresText = genres.joinToString(),
        overview = overview,
        isFavorite = isFavorite,
    )
}
