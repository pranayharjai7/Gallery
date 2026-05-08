package com.pranayharjai7.gallery.data.room

import com.pranayharjai7.gallery.data.room.dao.FavoritesDao
import com.pranayharjai7.gallery.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomFavoritesRepository @Inject constructor(
    private val favoritesDao: FavoritesDao
) : FavoritesRepository {

    override fun observeIds(): Flow<Set<Long>> =
        favoritesDao.observeIds().map { it.toSet() }

    override suspend fun toggle(mediaId: Long) {
        favoritesDao.toggle(mediaId)
    }

    override suspend fun contains(mediaId: Long): Boolean =
        favoritesDao.contains(mediaId)
}
