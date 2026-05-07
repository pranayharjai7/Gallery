package com.gallery.data.room

import com.gallery.data.room.dao.HiddenDao
import com.gallery.data.room.entity.HiddenEntity
import com.gallery.domain.repository.HiddenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomHiddenRepository @Inject constructor(
    private val hiddenDao: HiddenDao
) : HiddenRepository {

    override fun observeIds(): Flow<Set<Long>> =
        hiddenDao.observeIds().map { it.toSet() }

    override suspend fun add(mediaId: Long) {
        hiddenDao.insert(HiddenEntity(mediaId = mediaId))
    }

    override suspend fun remove(mediaId: Long) {
        hiddenDao.deleteById(mediaId)
    }
}
