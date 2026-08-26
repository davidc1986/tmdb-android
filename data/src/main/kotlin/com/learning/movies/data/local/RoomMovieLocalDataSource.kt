package com.learning.movies.data.local

import kotlinx.coroutines.flow.Flow

class RoomMovieLocalDataSource(
    private val favoriteDao: FavoriteDao,
) : MovieLocalDataSource {
    override fun observeFavoriteIds(): Flow<List<Int>> = favoriteDao.observeFavoriteIds()

    override suspend fun addFavorite(movieId: Int) {
        favoriteDao.add(FavoriteMovieEntity(movieId = movieId))
    }

    override suspend fun removeFavorite(movieId: Int) {
        favoriteDao.remove(FavoriteMovieEntity(movieId = movieId))
    }
}
