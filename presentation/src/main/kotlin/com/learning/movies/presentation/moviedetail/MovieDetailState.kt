package com.learning.movies.presentation.moviedetail

sealed interface MovieDetailState {
    data object Loading : MovieDetailState

    data class Content(val detail: MovieDetailUiModel) : MovieDetailState

    data class Error(val message: String) : MovieDetailState
}
