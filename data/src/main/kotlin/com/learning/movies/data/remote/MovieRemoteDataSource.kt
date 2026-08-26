package com.learning.movies.data.remote

import com.learning.movies.domain.Movie
import com.learning.movies.domain.MovieDetail
import com.learning.movies.domain.Outcome

interface MovieRemoteDataSource {
    suspend fun getPopularMovies(page: Int = 1): Outcome<List<Movie>>
    suspend fun getMovieDetail(movieId: Int): Outcome<MovieDetail>
}
