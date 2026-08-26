package com.learning.movies.presentation.moviedetail

import app.cash.turbine.test
import com.learning.movies.domain.MovieDetail
import com.learning.movies.domain.Outcome
import com.learning.movies.domain.repository.MovieRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelMockTest {

    private val repository = mockk<MovieRepository>()

    private val movieDetail = MovieDetail(
        id = 1,
        title = "A Real Movie",
        overview = "An overview",
        posterPath = null,
        releaseDate = "2026-01-01",
        voteAverage = 8.1,
        isFavorite = false,
        genres = listOf("Drama"),
        runtimeMinutes = 118,
        tagline = "A real tagline",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads detail and emits Content on success`() = runTest {
        every { repository.observeMovieDetail(1) } returns flowOf(Outcome.Success(movieDetail))

        val viewModel = MovieDetailViewModel(repository, movieId = 1)

        viewModel.state.test {
            assertEquals(MovieDetailState.Loading, awaitItem())
            val content = awaitItem() as MovieDetailState.Content
            assertEquals(movieDetail.toMovieDetailUiModel(), content.detail)
        }
    }

    @Test
    fun `load failure emits Error state`() = runTest {
        every { repository.observeMovieDetail(1) } returns flowOf(Outcome.Error.NetworkUnavailable)

        val viewModel = MovieDetailViewModel(repository, movieId = 1)

        viewModel.state.test {
            assertEquals(MovieDetailState.Loading, awaitItem())
            assertEquals(MovieDetailState.Error("No internet connection"), awaitItem())
        }
    }

    @Test
    fun `ToggleFavorite calls repository with the current detail's id and favorite flag`() = runTest {
        every { repository.observeMovieDetail(1) } returns flowOf(Outcome.Success(movieDetail))
        coEvery { repository.toggleFavorite(any(), any()) } returns Unit

        val viewModel = MovieDetailViewModel(repository, movieId = 1)
        viewModel.state.test {
            awaitItem()
            awaitItem()
        }

        viewModel.onIntent(MovieDetailIntent.ToggleFavorite)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.toggleFavorite(movieDetail.id, movieDetail.isFavorite) }
    }

    @Test
    fun `ToggleFavorite before content has loaded does nothing`() = runTest {
        every { repository.observeMovieDetail(1) } returns flowOf(Outcome.Error.NetworkUnavailable)

        val viewModel = MovieDetailViewModel(repository, movieId = 1)
        viewModel.state.test {
            awaitItem()
            awaitItem()
        }

        viewModel.onIntent(MovieDetailIntent.ToggleFavorite)

        coVerify(exactly = 0) { repository.toggleFavorite(any(), any()) }
    }

    @Test
    fun `Retry re-invokes the repository`() = runTest {
        every { repository.observeMovieDetail(1) } returns flowOf(Outcome.Error.RemoteFailure)

        val viewModel = MovieDetailViewModel(repository, movieId = 1)
        advanceUntilIdle()

        viewModel.onIntent(MovieDetailIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.observeMovieDetail(1) }
    }
}
