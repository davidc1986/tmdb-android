package com.learning.movies.data.local

import kotlinx.coroutines.flow.Flow

interface MovieLocalDataSource {
    fun observeFavoriteIds(): Flow<List<Int>>
    suspend fun addFavorite(movieId: Int)
    suspend fun removeFavorite(movieId: Int)
}
