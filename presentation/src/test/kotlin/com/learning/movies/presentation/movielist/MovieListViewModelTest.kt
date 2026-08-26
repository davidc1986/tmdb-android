package com.learning.movies.presentation.movielist

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.learning.movies.data.local.MoviesDatabase
import com.learning.movies.data.local.RoomMovieLocalDataSource
import com.learning.movies.data.remote.MovieApi
import com.learning.movies.data.remote.TmdbAuthInterceptor
import com.learning.movies.data.remote.TmdbRemoteDataSource
import com.learning.movies.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        server.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(baseUrl: okhttp3.HttpUrl = server.url("/")): MovieListViewModel {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .addInterceptor(TmdbAuthInterceptor("fake-token"))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MoviesDatabase::class.java).build()

        val remoteDataSource = TmdbRemoteDataSource(retrofit.create(MovieApi::class.java))
        val localDataSource = RoomMovieLocalDataSource(database.favoriteDao())
        val repository = MovieRepositoryImpl(remoteDataSource, localDataSource)

        return MovieListViewModel(repository)
    }

    private fun enqueuePopularMovies() {
        server.enqueue(
            MockResponse(
                body = """
                {
                  "page": 1,
                  "results": [
                    {
                      "id": 1,
                      "title": "A Real Movie",
                      "overview": "An overview",
                      "poster_path": "/poster.jpg",
                      "release_date": "2026-01-01",
                      "vote_average": 8.1
                    }
                  ],
                  "total_pages": 1,
                  "total_results": 1
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `successful load renders movies from the real network stack`() = runTest {
        enqueuePopularMovies()

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieListState.Content>().test {
            val content = awaitItem()
            assertEquals(1, content.movies.size)
            assertEquals("A Real Movie", content.movies.first().title)
            assertFalse(content.movies.first().isFavorite)
        }
    }

    @Test
    fun `toggling favorite reactively updates the list with no new network call`() = runTest {
        enqueuePopularMovies()

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieListState.Content>().test {
            val beforeToggle = awaitItem()
            assertFalse(beforeToggle.movies.first().isFavorite)

            viewModel.onIntent(MovieListIntent.ToggleFavorite(beforeToggle.movies.first()))

            val afterToggle = awaitItem()
            assertTrue(afterToggle.movies.first().isFavorite)
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `server error with no prior content surfaces as a full error state`() = runTest {
        server.enqueue(MockResponse(code = 500))

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieListState.Error>().test {
            awaitItem()
        }
    }

    @Test
    fun `the auth interceptor attaches the bearer token to every request`() = runTest {
        server.enqueue(MockResponse(code = 500))

        val viewModel = createViewModel()
        viewModel.state.filterIsInstance<MovieListState.Error>().test { awaitItem() }

        val recorded = server.takeRequest()
        assertEquals("Bearer fake-token", recorded.headers["Authorization"])
    }

    @Test
    fun `refresh failure with stale content surfaces a snackbar effect`() = runTest {
        enqueuePopularMovies()
        server.enqueue(MockResponse(code = 500))

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieListState.Content>().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.effects.test {
            viewModel.onIntent(MovieListIntent.Refresh)

            val effect = awaitItem()
            assertTrue(effect is MovieListEffect.ShowError)
        }
    }

    @Test
    fun `connection failure with no prior content surfaces as network unavailable`() = runTest {
        val baseUrl = server.url("/")
        server.close()

        val viewModel = createViewModel(baseUrl)

        viewModel.state.filterIsInstance<MovieListState.Error>().test {
            val error = awaitItem()
            assertEquals("No internet connection", error.message)
        }
    }
}
