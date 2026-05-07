package com.gallery.data.room

import com.gallery.data.room.dao.TrashDao
import com.gallery.data.room.entity.toEntity
import com.gallery.data.room.entity.toTrashItem
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTrashRepository @Inject constructor(
    private val trashDao: TrashDao
) : TrashRepository {

    override fun observeAll(): Flow<List<TrashItem>> =
        trashDao.observeAll().map { entities -> entities.map { it.toTrashItem() } }

    override suspend fun insert(item: TrashItem) {
        val path = item.originalUri.path.orEmpty()
        trashDao.insert(item.toEntity(path))
    }

    override suspend fun delete(id: Long) {
        trashDao.delete(id)
    }

    override suspend fun getExpired(before: Long): List<TrashItem> =
        trashDao.getExpired(before).map { it.toTrashItem() }

    override suspend fun deleteAll(ids: List<Long>) {
        trashDao.deleteAll(ids)
    }
}
