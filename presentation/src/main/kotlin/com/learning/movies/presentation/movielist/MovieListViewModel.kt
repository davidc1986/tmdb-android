package com.learning.movies.presentation.movielist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learning.movies.domain.Outcome
import com.learning.movies.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieListViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MovieListState>(MovieListState.Loading)
    val state: StateFlow<MovieListState> = _state.asStateFlow()

    private val _effects = Channel<MovieListEffect>()
    val effects = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        onIntent(MovieListIntent.LoadMovies)
    }

    fun onIntent(intent: MovieListIntent) {
        when (intent) {
            MovieListIntent.LoadMovies,
            MovieListIntent.Refresh,
            MovieListIntent.Retry,
            -> load()

            is MovieListIntent.ToggleFavorite -> toggleFavorite(intent.movie)
            is MovieListIntent.SelectMovie -> selectMovie(intent.movie)
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hadContent = _state.value is MovieListState.Content
            _state.update { reduceMovieListState(it, MovieListResult.Loading) }

            repository.observePopularMovies().collect { result ->
                when (result) {
                    is Outcome.Success -> {
                        val uiModels = result.value.map { it.toMovieListItemUiModel() }
                        _state.update { reduceMovieListState(it, MovieListResult.Loaded(uiModels)) }
                    }

                    is Outcome.Error.NetworkUnavailable -> {
                        fail("No internet connection", hadContent)
                    }

                    is Outcome.Error.RemoteFailure -> {
                        fail("Something went wrong. Please try again.", hadContent)
                    }

                    is Outcome.Error.Unknown -> {
                        fail(result.cause.message ?: "Something went wrong", hadContent)
                    }
                }
            }
        }
    }

    private fun toggleFavorite(movie: MovieListItemUiModel) {
        viewModelScope.launch {
            repository.toggleFavorite(movie.id, movie.isFavorite)
        }
    }

    private fun selectMovie(movie: MovieListItemUiModel) {
        viewModelScope.launch {
            _effects.send(MovieListEffect.NavigateToDetail(movie.id))
        }
    }

    private suspend fun fail(message: String, hadContent: Boolean) {
        _state.update { reduceMovieListState(it, MovieListResult.Failed(message)) }
        if (hadContent) {
            _effects.send(MovieListEffect.ShowError(message))
        }
    }
}
