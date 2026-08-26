package com.learning.movies.data.repository

import com.learning.movies.data.local.MovieLocalDataSource
import com.learning.movies.data.remote.MovieRemoteDataSource
import com.learning.movies.domain.Movie
import com.learning.movies.domain.MovieDetail
import com.learning.movies.domain.Outcome
import com.learning.movies.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class MovieRepositoryImpl(
    private val remoteDataSource: MovieRemoteDataSource,
    private val localDataSource: MovieLocalDataSource,
) : MovieRepository {

    override fun observePopularMovies(): Flow<Outcome<List<Movie>>> = flow {
        when (val result = remoteDataSource.getPopularMovies()) {
            is Outcome.Success -> {
                emitAll(
                    localDataSource.observeFavoriteIds().map { favoriteIds ->
                        Outcome.Success(
                            result.value.map { movie -> movie.copy(isFavorite = movie.id in favoriteIds) },
                        )
                    },
                )
            }

            is Outcome.Error -> emit(result)
        }
    }

    override fun observeMovieDetail(movieId: Int): Flow<Outcome<MovieDetail>> = flow {
        when (val result = remoteDataSource.getMovieDetail(movieId)) {
            is Outcome.Success -> {
                emitAll(
                    localDataSource.observeFavoriteIds().map { favoriteIds ->
                        Outcome.Success(result.value.copy(isFavorite = result.value.id in favoriteIds))
                    },
                )
            }

            is Outcome.Error -> emit(result)
        }
    }

    override suspend fun toggleFavorite(movieId: Int, isFavorite: Boolean) {
        if (isFavorite) {
            localDataSource.removeFavorite(movieId)
        } else {
            localDataSource.addFavorite(movieId)
        }
    }
}
