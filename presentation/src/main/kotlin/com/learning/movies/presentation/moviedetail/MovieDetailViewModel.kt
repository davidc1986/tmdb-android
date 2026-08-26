package com.learning.movies.presentation.moviedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learning.movies.domain.Outcome
import com.learning.movies.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieDetailViewModel(
    private val repository: MovieRepository,
    private val movieId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<MovieDetailState>(MovieDetailState.Loading)
    val state: StateFlow<MovieDetailState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onIntent(intent: MovieDetailIntent) {
        when (intent) {
            MovieDetailIntent.Retry -> load()
            MovieDetailIntent.ToggleFavorite -> toggleFavorite()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { reduceMovieDetailState(it, MovieDetailResult.Loading) }

            repository.observeMovieDetail(movieId).collect { result ->
                when (result) {
                    is Outcome.Success -> {
                        val uiModel = result.value.toMovieDetailUiModel()
                        _state.update { reduceMovieDetailState(it, MovieDetailResult.Loaded(uiModel)) }
                    }

                    is Outcome.Error.NetworkUnavailable -> {
                        fail("No internet connection")
                    }

                    is Outcome.Error.RemoteFailure -> {
                        fail("Something went wrong. Please try again.")
                    }

                    is Outcome.Error.Unknown -> {
                        fail(result.cause.message ?: "Something went wrong")
                    }
                }
            }
        }
    }

    private fun toggleFavorite() {
        val current = _state.value
        if (current !is MovieDetailState.Content) return
        viewModelScope.launch {
            repository.toggleFavorite(current.detail.id, current.detail.isFavorite)
        }
    }

    private suspend fun fail(message: String) {
        _state.update { reduceMovieDetailState(it, MovieDetailResult.Failed(message)) }
    }
}
