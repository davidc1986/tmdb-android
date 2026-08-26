package com.learning.movies.presentation.moviedetail

import com.learning.movies.domain.MovieDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailUiMapperTest {

    @Test
    fun `maps domain movie detail to ui model`() {
        val detail = MovieDetail(
            id = 1,
            title = "A Real Movie",
            overview = "An overview",
            posterPath = null,
            releaseDate = "2026-03-15",
            voteAverage = 8.1,
            isFavorite = true,
            genres = listOf("Drama", "Comedy"),
            runtimeMinutes = 118,
            tagline = "A real tagline",
        )

        val uiModel = detail.toMovieDetailUiModel()

        assertEquals(1, uiModel.id)
        assertEquals("A Real Movie", uiModel.title)
        assertEquals("A real tagline", uiModel.tagline)
        assertEquals("2026 · 118m", uiModel.subtitle)
        assertEquals("Drama, Comedy", uiModel.genresText)
        assertEquals(true, uiModel.isFavorite)
    }

    @Test
    fun `missing runtime and release date omit those parts of the subtitle`() {
        val detail = MovieDetail(
            id = 1,
            title = "A Real Movie",
            overview = "An overview",
            posterPath = null,
            releaseDate = null,
            voteAverage = 8.1,
            isFavorite = false,
            genres = emptyList(),
            runtimeMinutes = null,
            tagline = null,
        )

        val uiModel = detail.toMovieDetailUiModel()

        assertEquals("—", uiModel.subtitle)
        assertEquals("", uiModel.genresText)
    }
}
