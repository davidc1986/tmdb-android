package com.learning.movies

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.learning.movies.presentation.moviedetail.MovieDetailScreen
import com.learning.movies.presentation.movielist.MovieListScreen

private const val MOVIE_LIST_ROUTE = "movieList"
private const val MOVIE_DETAIL_ROUTE = "movieDetail/{movieId}"
private const val MOVIE_ID_ARG = "movieId"

@Composable
fun MoviesNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MOVIE_LIST_ROUTE) {
        composable(MOVIE_LIST_ROUTE) {
            MovieListScreen(
                onMovieClick = { movieId -> navController.navigate("movieDetail/$movieId") },
            )
        }

        composable(
            route = MOVIE_DETAIL_ROUTE,
            arguments = listOf(navArgument(MOVIE_ID_ARG) { type = NavType.IntType }),
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt(MOVIE_ID_ARG) ?: return@composable
            MovieDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
