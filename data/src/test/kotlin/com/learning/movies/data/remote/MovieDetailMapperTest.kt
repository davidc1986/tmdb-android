package com.learning.movies.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDetailMapperTest {

    @Test
    fun `maps every field from dto to domain`() {
        val dto = MovieDetailDto(
            id = 1,
            title = "A Real Movie",
            overview = "An overview",
            posterPath = "/poster.jpg",
            releaseDate = "2026-01-01",
            voteAverage = 8.1,
            genres = listOf(GenreDto(id = 18, name = "Drama"), GenreDto(id = 35, name = "Comedy")),
            runtime = 118,
            tagline = "A real tagline",
        )

        val detail = dto.toMovieDetail()

        assertEquals(1, detail.id)
        assertEquals("A Real Movie", detail.title)
        assertEquals(listOf("Drama", "Comedy"), detail.genres)
        assertEquals(118, detail.runtimeMinutes)
        assertEquals("A real tagline", detail.tagline)
        assertFalse(detail.isFavorite)
    }

    @Test
    fun `blank tagline is treated as absent`() {
        val dto = MovieDetailDto(id = 1, title = "A Real Movie", overview = "An overview", tagline = "   ")

        val detail = dto.toMovieDetail()

        assertNull(detail.tagline)
    }
}
