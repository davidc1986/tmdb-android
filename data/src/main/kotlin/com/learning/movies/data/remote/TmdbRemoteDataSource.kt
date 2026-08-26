package com.learning.movies.data.remote

import android.util.Log
import com.learning.movies.domain.Movie
import com.learning.movies.domain.MovieDetail
import com.learning.movies.domain.Outcome
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

private const val TAG = "TmdbRemoteDataSource"

class TmdbRemoteDataSource(
    private val api: MovieApi,
) : MovieRemoteDataSource {
    override suspend fun getPopularMovies(page: Int): Outcome<List<Movie>> =
        safeCall { api.getPopularMovies(page).results.map { it.toMovie() } }

    override suspend fun getMovieDetail(movieId: Int): Outcome<MovieDetail> =
        safeCall { api.getMovieDetail(movieId).toMovieDetail() }

    private suspend fun <T> safeCall(request: suspend () -> T): Outcome<T> =
        try {
            Outcome.Success(request())
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Outcome.Error.NetworkUnavailable
        } catch (e: HttpException) {
            Log.w(TAG, "TMDB request failed with HTTP ${e.code()}", e)
            Outcome.Error.RemoteFailure
        } catch (e: Exception) {
            Outcome.Error.Unknown(e)
        }
}
