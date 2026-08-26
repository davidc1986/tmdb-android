package com.learning.movies.presentation.moviedetail

sealed interface MovieDetailResult {
    data object Loading : MovieDetailResult
    data class Loaded(val detail: MovieDetailUiModel) : MovieDetailResult
    data class Failed(val message: String) : MovieDetailResult
}
