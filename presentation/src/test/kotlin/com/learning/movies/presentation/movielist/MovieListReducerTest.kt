package com.learning.movies.presentation.movielist

import org.junit.Assert.assertEquals
import org.junit.Test

class MovieListReducerTest {

    private val movies = listOf(
        MovieListItemUiModel(
            id = 1,
            title = "Movie One",
            displayYear = "2026",
            isFavorite = false,
        ),
    )

    @Test
    fun `first load succeeds`() {
        val newState = reduceMovieListState(MovieListState.Loading, MovieListResult.Loaded(movies))

        assertEquals(MovieListState.Content(movies), newState)
    }

    @Test
    fun `first load fails`() {
        val newState = reduceMovieListState(MovieListState.Loading, MovieListResult.Failed("network error"))

        assertEquals(MovieListState.Error("network error"), newState)
    }

    @Test
    fun `pull-to-refresh starts spinner without losing existing content`() {
        val current = MovieListState.Content(movies, isRefreshing = false)

        val newState = reduceMovieListState(current, MovieListResult.Loading)

        assertEquals(MovieListState.Content(movies, isRefreshing = true), newState)
    }

    @Test
    fun `refresh failure keeps stale content and stops the spinner`() {
        val current = MovieListState.Content(movies, isRefreshing = true)

        val newState = reduceMovieListState(current, MovieListResult.Failed("network error"))

        assertEquals(MovieListState.Content(movies, isRefreshing = false), newState)
    }
}
