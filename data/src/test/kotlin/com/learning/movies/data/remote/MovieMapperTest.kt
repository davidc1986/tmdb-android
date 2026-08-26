package com.learning.movies.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MovieMapperTest {

    @Test
    fun `maps every field from dto to domain`() {
        val dto = MovieDto(
            id = 1,
            title = "A Real Movie",
            overview = "An overview",
            posterPath = "/poster.jpg",
            releaseDate = "2026-01-01",
            voteAverage = 8.1,
        )

        val movie = dto.toMovie()

        assertEquals(1, movie.id)
        assertEquals("A Real Movie", movie.title)
        assertEquals("An overview", movie.overview)
        assertEquals("/poster.jpg", movie.posterPath)
        assertEquals("2026-01-01", movie.releaseDate)
        assertEquals(8.1, movie.voteAverage, 0.0)
    }

    @Test
    fun `remote data never knows about favorites`() {
        val dto = MovieDto(id = 1, title = "A Real Movie", overview = "An overview")

        val movie = dto.toMovie()

        assertFalse(movie.isFavorite)
    }
}
