package com.learning.movies.data.remote

import com.learning.movies.domain.Outcome
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class TmdbRemoteDataSourceTest {

    private val api = mockk<MovieApi>()
    private val dataSource = TmdbRemoteDataSource(api)

    @Test
    fun `getPopularMovies maps dto results to domain movies on success`() = runTest {
        coEvery { api.getPopularMovies(1) } returns MoviePageDto(
            page = 1,
            results = listOf(
                MovieDto(id = 1, title = "A Real Movie", overview = "An overview", voteAverage = 8.1),
            ),
            totalPages = 1,
            totalResults = 1,
        )

        val result = dataSource.getPopularMovies(1)

        assertEquals(Outcome.Success(listOf(MovieDto(id = 1, title = "A Real Movie", overview = "An overview", voteAverage = 8.1).toMovie())), result)
    }

    @Test
    fun `getPopularMovies returns NetworkUnavailable on IOException`() = runTest {
        coEvery { api.getPopularMovies(any()) } throws IOException()

        val result = dataSource.getPopularMovies()

        assertEquals(Outcome.Error.NetworkUnavailable, result)
    }

    @Test
    fun `getPopularMovies returns RemoteFailure on HttpException`() = runTest {
        coEvery { api.getPopularMovies(any()) } throws httpException(500)

        val result = dataSource.getPopularMovies()

        assertEquals(Outcome.Error.RemoteFailure, result)
    }

    @Test
    fun `getPopularMovies returns Unknown on unexpected exception`() = runTest {
        val cause = RuntimeException("boom")
        coEvery { api.getPopularMovies(any()) } throws cause

        val result = dataSource.getPopularMovies()

        assertTrue(result is Outcome.Error.Unknown)
        assertEquals(cause, (result as Outcome.Error.Unknown).cause)
    }

    @Test
    fun `getMovieDetail maps dto to domain on success`() = runTest {
        val dto = MovieDetailDto(id = 1, title = "A Real Movie", overview = "An overview", runtime = 118)
        coEvery { api.getMovieDetail(1) } returns dto

        val result = dataSource.getMovieDetail(1)

        assertEquals(Outcome.Success(dto.toMovieDetail()), result)
    }

    @Test
    fun `getMovieDetail returns NetworkUnavailable on IOException`() = runTest {
        coEvery { api.getMovieDetail(any()) } throws IOException()

        val result = dataSource.getMovieDetail(1)

        assertEquals(Outcome.Error.NetworkUnavailable, result)
    }

    @Test
    fun `getMovieDetail returns RemoteFailure on HttpException`() = runTest {
        coEvery { api.getMovieDetail(any()) } throws httpException(404)

        val result = dataSource.getMovieDetail(1)

        assertEquals(Outcome.Error.RemoteFailure, result)
    }

    private fun httpException(code: Int): HttpException {
        val body = "".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }
}
