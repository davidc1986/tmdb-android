package com.learning.movies.domain.repository

import com.learning.movies.domain.Movie
import com.learning.movies.domain.MovieDetail
import com.learning.movies.domain.Outcome
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun observePopularMovies(): Flow<Outcome<List<Movie>>>
    fun observeMovieDetail(movieId: Int): Flow<Outcome<MovieDetail>>
    suspend fun toggleFavorite(movieId: Int, isFavorite: Boolean)
}
