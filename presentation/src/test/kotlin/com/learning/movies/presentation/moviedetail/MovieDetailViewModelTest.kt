package com.learning.movies.presentation.moviedetail

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
class MovieDetailViewModelTest {

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

    private fun createViewModel(movieId: Int = 1): MovieDetailViewModel {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .addInterceptor(TmdbAuthInterceptor("fake-token"))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MoviesDatabase::class.java).build()

        val remoteDataSource = TmdbRemoteDataSource(retrofit.create(MovieApi::class.java))
        val localDataSource = RoomMovieLocalDataSource(database.favoriteDao())
        val repository = MovieRepositoryImpl(remoteDataSource, localDataSource)

        return MovieDetailViewModel(repository, movieId)
    }

    private fun enqueueMovieDetail() {
        server.enqueue(
            MockResponse(
                body = """
                {
                  "id": 1,
                  "title": "A Real Movie",
                  "overview": "An overview",
                  "poster_path": "/poster.jpg",
                  "release_date": "2026-01-01",
                  "vote_average": 8.1,
                  "runtime": 118,
                  "tagline": "A real tagline",
                  "genres": [{"id": 18, "name": "Drama"}]
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `successful load renders detail from the real network stack`() = runTest {
        enqueueMovieDetail()

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieDetailState.Content>().test {
            val content = awaitItem()
            assertEquals("A Real Movie", content.detail.title)
            assertEquals("Drama", content.detail.genresText)
            assertEquals("2026 · 118m", content.detail.subtitle)
            assertFalse(content.detail.isFavorite)
        }
    }

    @Test
    fun `toggling favorite reactively updates the detail with no new network call`() = runTest {
        enqueueMovieDetail()

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieDetailState.Content>().test {
            val beforeToggle = awaitItem()
            assertFalse(beforeToggle.detail.isFavorite)

            viewModel.onIntent(MovieDetailIntent.ToggleFavorite)

            val afterToggle = awaitItem()
            assertTrue(afterToggle.detail.isFavorite)
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `server error with no prior content surfaces as a full error state`() = runTest {
        server.enqueue(MockResponse(code = 500))

        val viewModel = createViewModel()

        viewModel.state.filterIsInstance<MovieDetailState.Error>().test {
            awaitItem()
        }
    }
}
