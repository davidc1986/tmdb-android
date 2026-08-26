package com.learning.movies.presentation.di

import com.learning.movies.presentation.moviedetail.MovieDetailViewModel
import com.learning.movies.presentation.movielist.MovieListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::MovieListViewModel)
    viewModel { (movieId: Int) -> MovieDetailViewModel(get(), movieId) }
}
