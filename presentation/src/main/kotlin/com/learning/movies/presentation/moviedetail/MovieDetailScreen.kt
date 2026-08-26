package com.learning.movies.presentation.moviedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Int,
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = koinViewModel(parameters = { parametersOf(movieId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movie details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            is MovieDetailState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            is MovieDetailState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Couldn't load movie: ${current.message}")
            }

            is MovieDetailState.Content -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.detail.title, style = MaterialTheme.typography.headlineSmall)
                    current.detail.tagline?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(current.detail.subtitle, style = MaterialTheme.typography.bodySmall)
                    if (current.detail.genresText.isNotEmpty()) {
                        Text(current.detail.genresText, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(current.detail.overview, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { viewModel.onIntent(MovieDetailIntent.ToggleFavorite) }) {
                    Icon(
                        imageVector = if (current.detail.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (current.detail.isFavorite) "Remove from favorites" else "Add to favorites",
                    )
                }
            }
        }
    }
}
