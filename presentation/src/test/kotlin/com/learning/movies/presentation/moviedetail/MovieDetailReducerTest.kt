package com.learning.movies.presentation.moviedetail

import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailReducerTest {

    private val detail = MovieDetailUiModel(
        id = 1,
        title = "Movie One",
        tagline = "A tagline",
        subtitle = "2026 · 120m",
        genresText = "Drama",
        overview = "An overview",
        isFavorite = false,
    )

    @Test
    fun `load succeeds`() {
        val newState = reduceMovieDetailState(MovieDetailState.Loading, MovieDetailResult.Loaded(detail))

        assertEquals(MovieDetailState.Content(detail), newState)
    }

    @Test
    fun `load fails`() {
        val newState = reduceMovieDetailState(MovieDetailState.Loading, MovieDetailResult.Failed("network error"))

        assertEquals(MovieDetailState.Error("network error"), newState)
    }

    @Test
    fun `retry after an error goes back to loading`() {
        val current = MovieDetailState.Error("network error")

        val newState = reduceMovieDetailState(current, MovieDetailResult.Loading)

        assertEquals(MovieDetailState.Loading, newState)
    }

    @Test
    fun `retry from stale content replaces it with loading`() {
        val current = MovieDetailState.Content(detail)

        val newState = reduceMovieDetailState(current, MovieDetailResult.Loading)

        assertEquals(MovieDetailState.Loading, newState)
    }
}
