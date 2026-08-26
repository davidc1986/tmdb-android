package com.learning.movies.data.local

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMovieLocalDataSourceTest {

    private val favoriteDao = mockk<FavoriteDao>()
    private val localDataSource = RoomMovieLocalDataSource(favoriteDao)

    @Test
    fun `observeFavoriteIds delegates to the dao`() = runTest {
        every { favoriteDao.observeFavoriteIds() } returns flowOf(listOf(1, 2, 3))

        localDataSource.observeFavoriteIds().test {
            assertEquals(listOf(1, 2, 3), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `addFavorite inserts an entity with the given id`() = runTest {
        coEvery { favoriteDao.add(any()) } returns Unit

        localDataSource.addFavorite(5)

        coVerify(exactly = 1) { favoriteDao.add(FavoriteMovieEntity(movieId = 5)) }
    }

    @Test
    fun `removeFavorite deletes an entity with the given id`() = runTest {
        coEvery { favoriteDao.remove(any()) } returns Unit

        localDataSource.removeFavorite(5)

        coVerify(exactly = 1) { favoriteDao.remove(FavoriteMovieEntity(movieId = 5)) }
    }
}
