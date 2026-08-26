package com.learning.movies.presentation.movielist

import com.learning.movies.domain.Movie
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieUiMapperTest {

    @Test
    fun `maps domain movie to ui model`() {
        val movie = Movie(
            id = 1,
            title = "A Real Movie",
            overview = "An overview",
            posterPath = null,
            releaseDate = "2026-03-15",
            voteAverage = 8.1,
            isFavorite = true,
        )

        val uiModel = movie.toMovieListItemUiModel()

        assertEquals(1, uiModel.id)
        assertEquals("A Real Movie", uiModel.title)
        assertEquals("2026", uiModel.displayYear)
        assertEquals(true, uiModel.isFavorite)
    }

    @Test
    fun `missing release date falls back to an em dash`() {
        val movie = Movie(
            id = 1,
            title = "A Real Movie",
            overview = "An overview",
            posterPath = null,
            releaseDate = null,
            voteAverage = 8.1,
            isFavorite = false,
        )

        val uiModel = movie.toMovieListItemUiModel()

        assertEquals("—", uiModel.displayYear)
    }
}
