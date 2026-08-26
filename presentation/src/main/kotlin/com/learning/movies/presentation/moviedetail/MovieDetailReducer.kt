package com.learning.movies.presentation.moviedetail

fun reduceMovieDetailState(
    current: MovieDetailState,
    result: MovieDetailResult,
): MovieDetailState = when (result) {
    is MovieDetailResult.Loading -> MovieDetailState.Loading
    is MovieDetailResult.Loaded -> MovieDetailState.Content(detail = result.detail)
    is MovieDetailResult.Failed -> MovieDetailState.Error(result.message)
}
