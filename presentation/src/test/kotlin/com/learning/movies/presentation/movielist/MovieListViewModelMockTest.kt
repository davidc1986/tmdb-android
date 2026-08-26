package com.learning.movies.presentation.movielist

import app.cash.turbine.test
import com.learning.movies.domain.Movie
import com.learning.movies.domain.Outcome
import com.learning.movies.domain.repository.MovieRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelMockTest {

    private val repository = mockk<MovieRepository>()

    private val movie = Movie(
        id = 1,
        title = "A Real Movie",
        overview = "An overview",
        posterPath = null,
        releaseDate = "2026-01-01",
        voteAverage = 8.1,
        isFavorite = false,
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
    fun `init loads movies and emits Content on success`() = runTest {
        every { repository.observePopularMovies() } returns flowOf(Outcome.Success(listOf(movie)))

        val viewModel = MovieListViewModel(repository)

        viewModel.state.test {
            assertEquals(MovieListState.Loading, awaitItem())
            val content = awaitItem() as MovieListState.Content
            assertEquals(listOf(movie.toMovieListItemUiModel()), content.movies)
        }
    }

    @Test
    fun `load failure with no prior content emits Error state`() = runTest {
        every { repository.observePopularMovies() } returns flowOf(Outcome.Error.NetworkUnavailable)

        val viewModel = MovieListViewModel(repository)

        viewModel.state.test {
            assertEquals(MovieListState.Loading, awaitItem())
            assertEquals(MovieListState.Error("No internet connection"), awaitItem())
        }
    }

    @Test
    fun `refresh failure with existing content surfaces a snackbar effect and keeps stale content`() = runTest {
        every { repository.observePopularMovies() } returnsMany listOf(
            flowOf(Outcome.Success(listOf(movie))),
            flowOf(Outcome.Error.RemoteFailure),
        )

        val viewModel = MovieListViewModel(repository)

        viewModel.state.filterIsInstance<MovieListState.Content>().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.effects.test {
            viewModel.onIntent(MovieListIntent.Refresh)

            val effect = awaitItem()
            assertTrue(effect is MovieListEffect.ShowError)
        }

        val finalState = viewModel.state.value as MovieListState.Content
        assertFalse(finalState.isRefreshing)
        assertEquals(listOf(movie.toMovieListItemUiModel()), finalState.movies)
    }

    @Test
    fun `ToggleFavorite calls repository with the movie's id and current favorite flag`() = runTest {
        every { repository.observePopularMovies() } returns flowOf(Outcome.Success(listOf(movie)))
        coEvery { repository.toggleFavorite(any(), any()) } returns Unit

        val viewModel = MovieListViewModel(repository)
        val uiModel = movie.toMovieListItemUiModel()

        viewModel.onIntent(MovieListIntent.ToggleFavorite(uiModel))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.toggleFavorite(uiModel.id, uiModel.isFavorite) }
    }

    @Test
    fun `SelectMovie emits a NavigateToDetail effect with the movie's id`() = runTest {
        every { repository.observePopularMovies() } returns flowOf(Outcome.Success(listOf(movie)))

        val viewModel = MovieListViewModel(repository)
        val uiModel = movie.toMovieListItemUiModel()

        viewModel.effects.test {
            viewModel.onIntent(MovieListIntent.SelectMovie(uiModel))

            val effect = awaitItem()
            assertTrue(effect is MovieListEffect.NavigateToDetail)
            assertEquals(uiModel.id, (effect as MovieListEffect.NavigateToDetail).movieId)
        }
    }

    @Test
    fun `Retry re-invokes the repository`() = runTest {
        every { repository.observePopularMovies() } returns flowOf(Outcome.Error.RemoteFailure)

        val viewModel = MovieListViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(MovieListIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.observePopularMovies() }
    }
}
