package com.learning.movies.data.repository

import app.cash.turbine.test
import com.learning.movies.data.local.MovieLocalDataSource
import com.learning.movies.data.remote.MovieRemoteDataSource
import com.learning.movies.domain.Movie
import com.learning.movies.domain.MovieDetail
import com.learning.movies.domain.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieRepositoryImplTest {

    private val remoteDataSource = mockk<MovieRemoteDataSource>()
    private val localDataSource = mockk<MovieLocalDataSource>()
    private val repository = MovieRepositoryImpl(remoteDataSource, localDataSource)

    private val movie = Movie(
        id = 1,
        title = "A Real Movie",
        overview = "An overview",
        posterPath = null,
        releaseDate = "2026-01-01",
        voteAverage = 8.1,
        isFavorite = false,
    )

    private val movieDetail = MovieDetail(
        id = 1,
        title = "A Real Movie",
        overview = "An overview",
        posterPath = null,
        releaseDate = "2026-01-01",
        voteAverage = 8.1,
        isFavorite = false,
        genres = emptyList(),
        runtimeMinutes = null,
        tagline = null,
    )

    @Test
    fun `observePopularMovies merges remote result with local favorite ids`() = runTest {
        coEvery { remoteDataSource.getPopularMovies() } returns Outcome.Success(listOf(movie))
        every { localDataSource.observeFavoriteIds() } returns flowOf(listOf(1))

        repository.observePopularMovies().test {
            val result = awaitItem()
            assertEquals(Outcome.Success(listOf(movie.copy(isFavorite = true))), result)
            awaitComplete()
        }
    }

    @Test
    fun `observePopularMovies propagates a remote error without touching local favorites`() = runTest {
        coEvery { remoteDataSource.getPopularMovies() } returns Outcome.Error.NetworkUnavailable

        repository.observePopularMovies().test {
            assertEquals(Outcome.Error.NetworkUnavailable, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 0) { localDataSource.observeFavoriteIds() }
    }

    @Test
    fun `observeMovieDetail merges remote result with local favorite ids`() = runTest {
        coEvery { remoteDataSource.getMovieDetail(1) } returns Outcome.Success(movieDetail)
        every { localDataSource.observeFavoriteIds() } returns flowOf(listOf(1))

        repository.observeMovieDetail(1).test {
            val result = awaitItem()
            assertEquals(Outcome.Success(movieDetail.copy(isFavorite = true)), result)
            awaitComplete()
        }
    }

    @Test
    fun `observeMovieDetail propagates a remote error without touching local favorites`() = runTest {
        coEvery { remoteDataSource.getMovieDetail(1) } returns Outcome.Error.RemoteFailure

        repository.observeMovieDetail(1).test {
            assertEquals(Outcome.Error.RemoteFailure, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 0) { localDataSource.observeFavoriteIds() }
    }

    @Test
    fun `toggleFavorite adds when not currently favorite`() = runTest {
        coEvery { localDataSource.addFavorite(any()) } returns Unit

        repository.toggleFavorite(1, isFavorite = false)

        coVerify(exactly = 1) { localDataSource.addFavorite(1) }
        coVerify(exactly = 0) { localDataSource.removeFavorite(any()) }
    }

    @Test
    fun `toggleFavorite removes when currently favorite`() = runTest {
        coEvery { localDataSource.removeFavorite(any()) } returns Unit

        repository.toggleFavorite(1, isFavorite = true)

        coVerify(exactly = 1) { localDataSource.removeFavorite(1) }
        coVerify(exactly = 0) { localDataSource.addFavorite(any()) }
    }
}
