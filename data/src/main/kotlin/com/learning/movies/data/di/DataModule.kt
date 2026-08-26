package com.learning.movies.data.di

import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.learning.movies.data.BuildConfig
import com.learning.movies.data.local.MovieLocalDataSource
import com.learning.movies.data.local.MoviesDatabase
import com.learning.movies.data.local.RoomMovieLocalDataSource
import com.learning.movies.data.remote.MovieApi
import com.learning.movies.data.remote.MovieRemoteDataSource
import com.learning.movies.data.remote.TmdbAuthInterceptor
import com.learning.movies.data.remote.TmdbRemoteDataSource
import com.learning.movies.data.repository.MovieRepositoryImpl
import com.learning.movies.domain.repository.MovieRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

val dataModule = module {
    single { Json { ignoreUnknownKeys = true } }

    single {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(TmdbAuthInterceptor(BuildConfig.TMDB_API_READ_TOKEN))
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single { get<Retrofit>().create(MovieApi::class.java) }

    single<MovieRemoteDataSource> { TmdbRemoteDataSource(get()) }

    single {
        Room.databaseBuilder(androidContext(), MoviesDatabase::class.java, "movies.db").build()
    }
    single { get<MoviesDatabase>().favoriteDao() }
    single<MovieLocalDataSource> { RoomMovieLocalDataSource(get()) }

    single<MovieRepository> { MovieRepositoryImpl(get(), get()) }
}
